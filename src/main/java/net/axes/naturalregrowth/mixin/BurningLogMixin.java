package net.axes.naturalregrowth.mixin;

import dev.protomanly.pmweather.data.ReinforcementManager;
import dev.protomanly.pmweather.event.GameBusEvents;
import net.axes.naturalregrowth.Config; // <--- Import Config
import net.axes.naturalregrowth.ModBlocks;
import net.axes.naturalregrowth.block.entity.RegrowingStumpBlockEntity;
import net.axes.naturalregrowth.compat.NaturalRegrowthCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.protomanly.pmweather.block.BurningLogBlock")
public abstract class BurningLogMixin {

    @Shadow(remap = false) private Block burnsInto;
    @Shadow(remap = false) public abstract int getBurnChance();

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void onRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (random.nextInt(this.getBurnChance()) == 0) {

            // --- 1. FEATURE TOGGLE CHECK ---
            if (!Config.COMMON.enableFireRegrowth.get()) {
                return; // Logic disabled? Do nothing (let it become a Charred Log)
            }

            // --- 2. FILTER LOGIC ---

            // Vertical Check
            if (state.hasProperty(BlockStateProperties.AXIS)) {
                if (state.getValue(BlockStateProperties.AXIS) != Direction.Axis.Y) {
                    return;
                }
            }

            // Base Check
            BlockState below = level.getBlockState(pos.below());
            if (!NaturalRegrowthCompat.isValidGround(below)) {
                return;
            }

            // Structure Check
            if (NaturalRegrowthCompat.hasArtificialNeighbors(level, pos)) {
                return;
            }

            // --- 3. PLACEMENT LOGIC ---

            Block saplingBlock = Blocks.OAK_SAPLING;
            ReinforcementManager manager = (ReinforcementManager) GameBusEvents.REINFORCEMENTMANAGERS.get(level.dimension());

            if (manager != null && manager.hasSaplingData(pos)) {
                saplingBlock = manager.getSaplingType(pos);
            }

            BlockState visualState = this.burnsInto.defaultBlockState();
            if (visualState.hasProperty(BlockStateProperties.AXIS) && state.hasProperty(BlockStateProperties.AXIS)) {
                visualState = visualState.setValue(BlockStateProperties.AXIS, state.getValue(BlockStateProperties.AXIS));
            }

            level.setBlock(pos, ModBlocks.REGROWING_STUMP.get().defaultBlockState(), 3);

            if (level.getBlockEntity(pos) instanceof RegrowingStumpBlockEntity stump) {
                stump.setMimic(visualState, saplingBlock.defaultBlockState());
                stump.setCreationTime(level.getGameTime());
                stump.setIsFireStump(true); // <--- FLAG AS FIRE STUMP
            }

            ci.cancel();
        }
    }
}