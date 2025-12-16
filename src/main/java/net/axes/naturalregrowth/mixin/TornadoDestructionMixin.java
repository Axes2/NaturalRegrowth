package net.axes.naturalregrowth.mixin;

import dev.protomanly.pmweather.weather.Storm;
import net.axes.naturalregrowth.ModBlocks;
import net.axes.naturalregrowth.block.RegrowingStumpBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Storm.class)
public class TornadoDestructionMixin {

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    public boolean onStormRemoveBlock(Level level, BlockPos pos, boolean isMoving) {

        BlockState state = level.getBlockState(pos);

        // 1. INVINCIBILITY: Save our stumps!
        if (state.getBlock() instanceof RegrowingStumpBlock) {
            return false;
        }

        // 2. LOG LOGIC: Direct Hits (Center of Storm)
        if (state.is(BlockTags.LOGS)) {
            if (isNaturalTree(level, pos)) {
                BlockPos stumpPos = findStump(level, pos);

                // If the base is already infected, let the storm destroy this specific log block
                if (level.getBlockState(stumpPos).getBlock() instanceof RegrowingStumpBlock) {
                    return level.removeBlock(pos, isMoving);
                }

                // Place the stump and "Vaporize" the trunk immediately (Direct Hit Logic)
                infectTreeBase(level, stumpPos, state);
                breakTrunk(level, stumpPos.above()); // Clean up immediately for direct hits

                return true;
            }
        }

        // 3. LEAF LOGIC: Glancing Blows (Edge of Storm)
        // If the storm eats a leaf, check if it's attached to a log
        if (state.is(BlockTags.LEAVES)) {
            BlockPos below = pos.below();
            BlockState stateBelow = level.getBlockState(below);

            // If we just exposed a log, this tree is "dying"
            if (stateBelow.is(BlockTags.LOGS)) {

                BlockPos stumpPos = findStump(level, below);
                BlockState stumpState = level.getBlockState(stumpPos);

                // Only infect if it hasn't been infected yet
                if (!(stumpState.getBlock() instanceof RegrowingStumpBlock)) {
                    // We ONLY place the stump. We do NOT break the trunk.
                    // The tree stays standing (with a scarred base) until it regrows later.
                    infectTreeBase(level, stumpPos, stateBelow);
                }
            }
        }

        return level.removeBlock(pos, isMoving);
    }

    // Helper to swap the base block for a Stump
    private void infectTreeBase(Level level, BlockPos stumpPos, BlockState originalLog) {
        if (level.getBlockState(stumpPos.below()).is(BlockTags.DIRT)) {
            int typeId = getTypeIdForLog(originalLog);

            BlockState stumpState = ModBlocks.REGROWING_STUMP.get()
                    .defaultBlockState()
                    .setValue(RegrowingStumpBlock.TREE_TYPE, typeId);

            level.setBlock(stumpPos, stumpState, 3);
        }
    }

    // Helper used ONLY for Direct Hits (Center of storm)
    private void breakTrunk(Level level, BlockPos startPos) {
        BlockPos cursor = startPos;
        int safety = 0;
        while (level.getBlockState(cursor).is(BlockTags.LOGS) && safety < 30) {
            level.destroyBlock(cursor, false);
            cursor = cursor.above();
            safety++;
        }
    }

    private boolean isNaturalTree(Level level, BlockPos pos) {
        BlockPos cursor = pos;
        int height = 0;
        while (level.getBlockState(cursor.above()).is(BlockTags.LOGS) && height < 20) {
            cursor = cursor.above();
            height++;
        }
        BlockState topBlock = level.getBlockState(cursor.above());
        if (topBlock.is(BlockTags.LEAVES)) {
            return !topBlock.getValue(LeavesBlock.PERSISTENT);
        }
        // Fallback: If top is air, check neighbors
        if (topBlock.isAir()) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x==0 && z==0) continue;
                    BlockState neighbor = level.getBlockState(cursor.offset(x, 0, z));
                    if (neighbor.is(BlockTags.LEAVES) && !neighbor.getValue(LeavesBlock.PERSISTENT)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private BlockPos findStump(Level level, BlockPos pos) {
        BlockPos cursor = pos;
        int safety = 0;
        while (level.getBlockState(cursor.below()).is(BlockTags.LOGS) && safety < 30) {
            cursor = cursor.below();
            safety++;
        }
        return cursor;
    }

    private int getTypeIdForLog(BlockState logState) {
        Block log = logState.getBlock();
        if (log == Blocks.SPRUCE_LOG) return 1;
        if (log == Blocks.BIRCH_LOG) return 2;
        if (log == Blocks.JUNGLE_LOG) return 3;
        if (log == Blocks.ACACIA_LOG) return 4;
        if (log == Blocks.DARK_OAK_LOG) return 5;
        if (log == Blocks.CHERRY_LOG) return 6;
        if (log == Blocks.MANGROVE_LOG) return 7;
        return 0;
    }
}