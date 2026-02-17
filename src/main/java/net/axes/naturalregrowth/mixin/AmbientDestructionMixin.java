package net.axes.naturalregrowth.mixin;

import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.WeatherHandler;
import dev.protomanly.pmweather.weather.storms.StormTypes;
import net.axes.naturalregrowth.Config;
import net.axes.naturalregrowth.ModBlocks;
import net.axes.naturalregrowth.block.HealingBlock;
import net.axes.naturalregrowth.block.RegrowingStumpBlock;
import net.axes.naturalregrowth.block.entity.HealingBlockEntity;
import net.axes.naturalregrowth.block.entity.RegrowingStumpBlockEntity;
import net.axes.naturalregrowth.compat.dt.DTLoader;
import net.axes.naturalregrowth.compat.dt.DTIntegration;
import net.axes.naturalregrowth.util.TreeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.Random;

@Mixin(GameBusEvents.class)
public class AmbientDestructionMixin {

    // --- 1. SCALING DAMAGE DENSITY (FIXED) ---
    // UPDATED: Changed from 260 to 200 to match PMWeather 0.16.0
    @ModifyConstant(method = "onLevelTick", constant = @Constant(intValue = 200), remap = false)
    private static int modifyDamageIterations(int original) {
        int radius = Config.COMMON.windRadius.get();
        // 64 * 64 = 4096. If radius is default (64), this returns original (200).
        return (original * radius * radius) / 4096;
    }

    // --- 2. CONFIGURABLE RADIUS HOOK ---
    // This targets "PMWeather.RANDOM.nextInt(-64, 65)"
    @Redirect(
            method = "onLevelTick",
            at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(II)I"),
            remap = false
    )
    private static int onNextInt(Random instance, int origin, int bound) {
        // We catch the specific call where PMWeather calculates the offset
        if (origin == -64 && bound == 65) {
            int radius = Config.COMMON.windRadius.get();
            // Scale the random range to match our config
            return instance.nextInt(-radius, radius + 1);
        }
        return instance.nextInt(origin, bound);
    }

    // --- 3. DESTRUCTION LOGIC HOOK ---
    @Redirect(method = "onLevelTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private static boolean onWindRemoveBlock(Level level, BlockPos pos, boolean isMoving) {
        BlockState oldState = level.getBlockState(pos);

        // 0. IMMUNITY
        if (oldState.getBlock() instanceof HealingBlock || oldState.getBlock() instanceof RegrowingStumpBlock) {
            return true;
        }

        // 1. DYNAMIC TREES BYPASS
        if (DTLoader.isLoaded()) {
            if (DTIntegration.isDTBranch(oldState)) {
                return true;
            }
        }

        // 2. PROXIMITY CHECK
        boolean isTornadoNearby = false;
        WeatherHandler handler = GameBusEvents.MANAGERS.get(level.dimension());
        if (handler != null) {
            for (Storm storm : handler.getStorms()) {
                // FIXED: Changed '0' to 'StormTypes.SUPERCELL'
                if (storm.stormType == StormTypes.SUPERCELL && storm.stage >= 3 && !storm.dead) {
                    double distSq = storm.position.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
                    double dangerZone = storm.width + 40.0;
                    if (distSq < dangerZone * dangerZone) {
                        isTornadoNearby = true;
                        break;
                    }
                }
            }
        }

        // 3. FATAL LOGIC (Ambient Wind near Tornado)
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

        // 4. HEALING LOGIC (Global Wind Events)
        if (oldState.is(BlockTags.LEAVES)) {
            level.setBlock(pos, ModBlocks.HEALING_LEAF.get().defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof HealingBlockEntity healer) {
                healer.setStates(oldState, Blocks.AIR.defaultBlockState());
            }
            return true;
        }

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