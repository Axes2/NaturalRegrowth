package net.axes.naturalregrowth.mixin;

import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.WeatherHandler;
import net.axes.naturalregrowth.ModBlocks;
import net.axes.naturalregrowth.block.HealingBlock; // <--- Import this
import net.axes.naturalregrowth.block.RegrowingStumpBlock; // <--- Import this
import net.axes.naturalregrowth.block.entity.HealingBlockEntity;
import net.axes.naturalregrowth.block.entity.RegrowingStumpBlockEntity;
import net.axes.naturalregrowth.util.TreeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameBusEvents.class)
public class AmbientDestructionMixin {

    @Redirect(method = "onLevelTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private static boolean onWindRemoveBlock(Level level, BlockPos pos, boolean isMoving) {
        BlockState oldState = level.getBlockState(pos);

        // --- 0. IMMUNITY (THE FIX) ---
        // If the block is already part the mod (Healing or Stump), the wind cannot hurt it further.
        // This prevents:
        // 1. Overwriting the "Original Block" memory with "Healing Log".
        // 2. Misidentifying "Healing Leaf" (which is tagged as a LOG) as a real log.
        if (oldState.getBlock() instanceof HealingBlock || oldState.getBlock() instanceof RegrowingStumpBlock) {
            return true; // "True" means handled removal, but since we didn't remove it, it stays
        }
        // ------------------------------------

        // --- 1. PROXIMITY CHECK: IS THIS A TORNADO? ---
        boolean isTornadoNearby = false;

        WeatherHandler handler = GameBusEvents.MANAGERS.get(level.dimension());
        if (handler != null) {
            for (Storm storm : handler.getStorms()) {
                if (storm.stormType == 0 && storm.stage >= 3 && !storm.dead) {
                    double distSq = storm.position.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
                    double dangerZone = storm.width + 40.0;
                    if (distSq < dangerZone * dangerZone) {
                        isTornadoNearby = true;
                        break;
                    }
                }
            }
        }

        // --- 2. FATAL LOGIC (TORNADO NEARBY) ---
        if (isTornadoNearby) {
            if (oldState.is(BlockTags.LOGS)) {
                if (level.getBlockState(pos.below()).is(BlockTags.DIRT)) {
                    level.setBlock(pos, ModBlocks.REGROWING_STUMP.get().defaultBlockState(), 3);
                    if (level.getBlockEntity(pos) instanceof RegrowingStumpBlockEntity stump) {
                        BlockState sapling = Blocks.OAK_SAPLING.defaultBlockState();
                        BlockState stripped = TreeUtils.getStrippedLog(level, pos, oldState);
                        stump.setMimic(stripped, sapling);
                        stump.setCreationTime(level.getGameTime());
                    }
                    return true;
                } else {
                    return level.removeBlock(pos, isMoving);
                }
            }
            if (oldState.is(BlockTags.LEAVES)) {
                return level.removeBlock(pos, isMoving);
            }
            return level.removeBlock(pos, isMoving);
        }


        // --- 3. HEALING LOGIC (WIND SCARS) ---

        // LEAVES
        if (oldState.is(BlockTags.LEAVES)) {
            level.setBlock(pos, ModBlocks.HEALING_LEAF.get().defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof HealingBlockEntity healer) {
                healer.setStates(oldState, Blocks.AIR.defaultBlockState());
            }
            return true;
        }

        // LOGS
        // Note: HEALING_LEAF is technically a "LOG" tag-wise, but the Immunity Clause at the top catches it first
        if (oldState.is(BlockTags.LOGS)) {
            if (level.getBlockState(pos.below()).is(BlockTags.DIRT)) {
                level.setBlock(pos, ModBlocks.REGROWING_STUMP.get().defaultBlockState(), 3);
                if (level.getBlockEntity(pos) instanceof RegrowingStumpBlockEntity stump) {
                    BlockState sapling = Blocks.OAK_SAPLING.defaultBlockState();
                    BlockState stripped = TreeUtils.getStrippedLog(level, pos, oldState);
                    stump.setMimic(stripped, sapling);
                    stump.setCreationTime(level.getGameTime());
                }
            } else {
                level.setBlock(pos, ModBlocks.HEALING_LOG.get().defaultBlockState(), 3);
                if (level.getBlockEntity(pos) instanceof HealingBlockEntity healer) {
                    BlockState stripped = TreeUtils.getStrippedLog(level, pos, oldState);
                    if (stripped.hasProperty(BlockStateProperties.AXIS) && oldState.hasProperty(BlockStateProperties.AXIS)) {
                        stripped = stripped.setValue(BlockStateProperties.AXIS, oldState.getValue(BlockStateProperties.AXIS));
                    }
                    healer.setStates(oldState, stripped);
                }
            }
            return true;
        }

        return level.removeBlock(pos, isMoving);
    }
}