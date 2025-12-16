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

        // --- NEW: INVINCIBILITY CHECK ---
        // If the storm tries to eat our special stump, we stop it immediately.
        // returning 'false' means "The block was NOT removed."
        if (state.getBlock() instanceof RegrowingStumpBlock) {
            return false;
        }
        // --------------------------------

        // Optimization: Only run regeneration logic on logs
        if (state.is(BlockTags.LOGS)) {

            // 1. Quick "Vertical Scan" to see if this log has leaves on top
            if (isNaturalTree(level, pos)) {

                // 2. Identify the base of the tree
                BlockPos stumpPos = findStump(level, pos);

                // 3. Trigger Regeneration Logic (Place Stump)
                handleTreeRegeneration(level, stumpPos, state);

                // Return true because we handled the removal (by turning it into a stump)
                return true;
            }
        }

        // Default behavior for everything else (let the storm destroy it)
        return level.removeBlock(pos, isMoving);
    }

    // New "Vertical Trace" Scanner (Optimized for Performance)
    private boolean isNaturalTree(Level level, BlockPos pos) {
        BlockPos cursor = pos;
        int height = 0;

        // Trace UP until we find the top of the log column
        while (level.getBlockState(cursor.above()).is(BlockTags.LOGS) && height < 20) {
            cursor = cursor.above();
            height++;
        }

        // Now 'cursor' is the highest log. Check the block directly ABOVE it.
        BlockState topBlock = level.getBlockState(cursor.above());

        // A. Is it leaves? (Natural)
        if (topBlock.is(BlockTags.LEAVES)) {
            return !topBlock.getValue(LeavesBlock.PERSISTENT);
        }

        // B. Is it a house roof? (Man-made)
        if (topBlock.is(Blocks.OAK_PLANKS) || topBlock.is(Blocks.COBBLESTONE) || topBlock.is(Blocks.STONE_BRICKS)) {
            return false;
        }

        // C. If top is Air (leaves stripped?), check immediate neighbors of the top log
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
        // Scan DOWN until we hit dirt/grass
        while (level.getBlockState(cursor.below()).is(BlockTags.LOGS) && safety < 30) {
            cursor = cursor.below();
            safety++;
        }
        return cursor;
    }

    private void handleTreeRegeneration(Level level, BlockPos stumpPos, BlockState destroyedLog) {
        // Check if we found dirt below the stump
        if (level.getBlockState(stumpPos.below()).is(BlockTags.DIRT)) {

            // 1. Determine the tree type ID (0=Oak, 1=Spruce, etc.)
            int typeId = getTypeIdForLog(destroyedLog);

            // 2. Create the Stump Block State with that ID
            BlockState stumpState = ModBlocks.REGROWING_STUMP.get()
                    .defaultBlockState()
                    .setValue(RegrowingStumpBlock.TREE_TYPE, typeId);

            // 3. Place the Stump (Replacing the old log block at the base)
            level.setBlock(stumpPos, stumpState, 3);

            // 4. "Vaporize" the rest of the tree (No items = No lag)
            breakTrunk(level, stumpPos.above());
        }
    }

    private void breakTrunk(Level level, BlockPos startPos) {
        BlockPos cursor = startPos;
        int safety = 0;
        // Break logs going up.
        // OPTIMIZED: false = "Vaporize" (No items dropped, drastically reduces lag)
        while (level.getBlockState(cursor).is(BlockTags.LOGS) && safety < 30) {
            level.destroyBlock(cursor, false);
            cursor = cursor.above();
            safety++;
        }
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
        // Default (Oak) is 0
        return 0;
    }
}