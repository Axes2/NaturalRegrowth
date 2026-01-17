package net.axes.naturalregrowth.compat;

import net.axes.naturalregrowth.ModBlocks;
import net.axes.naturalregrowth.block.RegrowingStumpBlock;
import net.axes.naturalregrowth.block.entity.RegrowingStumpBlockEntity;
import net.axes.naturalregrowth.util.TreeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NaturalRegrowthCompat {

    public static boolean removeBlockWithRegrowth(Level level, BlockPos pos, boolean isMoving) {
        BlockState state = level.getBlockState(pos);

        // Safety: Don't break our own stumps
        if (state.getBlock() instanceof RegrowingStumpBlock) return false;

        // 1. LOG LOGIC (Direct Hit)
        if (state.is(BlockTags.LOGS)) {
            // Find the bottom of this specific log column (handling diagonals)
            BlockPos bottom = getTrueTreeBottom(level, pos);
            if (isNaturalTree(level, bottom)) {
                // Check if already infected
                if (level.getBlockState(bottom).getBlock() instanceof RegrowingStumpBlock) {
                    return level.removeBlock(pos, isMoving);
                }
                infectTreeBase(level, bottom, state);
                return true;
            }
        }

        // 2. LEAF LOGIC (The "Spider" Search)
        if (state.is(BlockTags.LEAVES)) {
            // NEW: Instead of just checking below, scan for ANY nearby log
            BlockPos nearbyLog = findNeighborLog(level, pos);

            if (nearbyLog != null) {
                // Trace that log down to the stump
                BlockPos stumpPos = getTrueTreeBottom(level, nearbyLog);
                BlockState stumpState = level.getBlockState(stumpPos);

                // If valid tree & not yet infected
                if (isNaturalTree(level, stumpPos) && !(stumpState.getBlock() instanceof RegrowingStumpBlock)) {
                    // We found the log this leaf belonged to!
                    infectTreeBase(level, stumpPos, level.getBlockState(nearbyLog));
                }
            }
        }

        return level.removeBlock(pos, isMoving);
    }

    // --- NEW HELPERS ---

    /**
     * Scans a 3x3x3 area around the broken leaf to find a connecting log.
     * Crucial for Acacia/Dark Oak where leaves are offset from the trunk.
     */
    private static BlockPos findNeighborLog(Level level, BlockPos center) {
        int radius = 2; // Look 2 blocks out (covers most branch structures)

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    // Skip center
                    if (x == 0 && y == 0 && z == 0) continue;

                    BlockPos target = center.offset(x, y, z);
                    if (level.getBlockState(target).is(BlockTags.LOGS)) {
                        return target;
                    }
                }
            }
        }
        return null;
    }

    /**
     * "The Crawler" - Follows logs DOWN (including diagonals) to find the true base.
     * Solves the Acacia "Bending Trunk" issue.
     */
    private static BlockPos getTrueTreeBottom(Level level, BlockPos startPos) {
        BlockPos cursor = startPos;
        int safety = 0;

        // Keep moving down as long as we can find a connected log below
        while (safety < 40) {
            // 1. Check directly below
            if (level.getBlockState(cursor.below()).is(BlockTags.LOGS)) {
                cursor = cursor.below();
            }
            // 2. Check diagonals below (for Acacia/Dark Oak)
            else {
                boolean foundDiagonal = false;
                // Scan the 8 blocks around the block BELOW us
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x==0 && z==0) continue;
                        BlockPos diag = cursor.offset(x, -1, z);
                        if (level.getBlockState(diag).is(BlockTags.LOGS)) {
                            cursor = diag;
                            foundDiagonal = true;
                            break;
                        }
                    }
                    if (foundDiagonal) break;
                }

                // If no log below or diagonal-below, we hit the bottom
                if (!foundDiagonal) {
                    return cursor;
                }
            }
            safety++;
        }
        return cursor;
    }

    /**
     * Updated Validator: Checks criteria starting from the KNOWN bottom.
     */
    public static boolean isNaturalTree(Level level, BlockPos bottomPos) {
        // 1. Ground Check
        BlockState ground = level.getBlockState(bottomPos.below());
        boolean isValidGround = ground.is(BlockTags.DIRT) ||
                ground.is(BlockTags.SAND) ||
                ground.is(Blocks.MANGROVE_ROOTS) ||
                ground.is(Blocks.CLAY) ||
                ground.is(Blocks.MOSS_BLOCK) ||
                ground.is(BlockTags.NYLIUM) ||
                ground.is(Blocks.END_STONE);
        if (!isValidGround) return false;

        // 2. Height Check (Scan UP from bottom using the same Crawler logic)
        int height = measureTreeHeight(level, bottomPos);
        if (height < 3) return false;

        // 3. Crown Check
        // If it's a Mega Tree (tall + thick), we are lenient
        if (height > 10 && !tryFind2x2Base(level, bottomPos, level.getBlockState(bottomPos).getBlock()).isEmpty()) {
            return true;
        }

        // Otherwise, scan for leaves near the top
        // (We estimate the top is at bottom.y + height)
        BlockPos estimatedTop = bottomPos.above(height);
        return hasLeavesNearby(level, estimatedTop);
    }

    private static int measureTreeHeight(Level level, BlockPos bottom) {
        BlockPos cursor = bottom;
        int height = 1;
        int safety = 0;

        while (safety < 60) {
            // Try UP
            if (level.getBlockState(cursor.above()).is(BlockTags.LOGS)) {
                cursor = cursor.above();
                height++;
            }
            // Try Diagonals UP
            else {
                boolean foundDiag = false;
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x==0 && z==0) continue;
                        BlockPos diag = cursor.offset(x, 1, z);
                        if (level.getBlockState(diag).is(BlockTags.LOGS)) {
                            cursor = diag;
                            height++;
                            foundDiag = true;
                            break;
                        }
                    }
                    if (foundDiag) break;
                }
                if (!foundDiag) break; // End of tree
            }
            safety++;
        }
        return height;
    }

    private static boolean hasLeavesNearby(Level level, BlockPos topPos) {
        // Scan a box around the estimated top
        int radius = 3;
        for (int y = -2; y <= 2; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos p = topPos.offset(x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (s.is(BlockTags.LEAVES) && !s.getValue(LeavesBlock.PERSISTENT)) return true;
                    if (s.is(Blocks.VINE) || s.is(Blocks.MANGROVE_LEAVES)) return true;
                }
            }
        }
        return false;
    }

    // --- EXISTING HELPERS (Unchanged) ---
    private static void infectTreeBase(Level level, BlockPos stumpPos, BlockState originalLog) {
        Set<BlockPos> base2x2 = tryFind2x2Base(level, stumpPos, originalLog.getBlock());

        if (!base2x2.isEmpty()) {
            for (BlockPos pos : base2x2) {
                placeSingleStump(level, pos, level.getBlockState(pos));
            }
        } else {
            placeSingleStump(level, stumpPos, originalLog);
        }
    }

    private static void placeSingleStump(Level level, BlockPos pos, BlockState originalState) {
        if (level.getBlockState(pos).getBlock() instanceof RegrowingStumpBlock) return;

        BlockState disguise = TreeUtils.getStrippedLog(level, pos, originalState);
        BlockState sapling = getSaplingFromLog(originalState);

        level.setBlock(pos, ModBlocks.REGROWING_STUMP.get().defaultBlockState(), 3);

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RegrowingStumpBlockEntity stump) {
            stump.setMimic(disguise, sapling);
            stump.setCreationTime(level.getGameTime());
        }
    }

    private static Set<BlockPos> tryFind2x2Base(Level level, BlockPos origin, Block logBlock) {
        int[][] offsets = {
                {0, 0,  1, 0,  0, 1,  1, 1},
                {0, 0, -1, 0,  0, 1, -1, 1},
                {0, 0,  1, 0,  0,-1,  1,-1},
                {0, 0, -1, 0,  0,-1, -1,-1}
        };

        for (int[] off : offsets) {
            Set<BlockPos> candidates = new HashSet<>();
            boolean match = true;
            for (int i = 0; i < 8; i += 2) {
                BlockPos target = origin.offset(off[i], 0, off[i+1]);
                BlockState state = level.getBlockState(target);
                if (!state.is(logBlock) && !(state.getBlock() instanceof RegrowingStumpBlock)) {
                    match = false;
                    break;
                }
                candidates.add(target);
            }
            if (match) return candidates;
        }
        return Collections.emptySet();
    }

    private static BlockState getSaplingFromLog(BlockState logState) {
        ResourceLocation logId = BuiltInRegistries.BLOCK.getKey(logState.getBlock());
        String namespace = logId.getNamespace();
        String path = logId.getPath().replace("stripped_", "");
        String saplingPath = path.replace("_log", "_sapling").replace("_wood", "_sapling");
        if (!saplingPath.endsWith("_sapling")) saplingPath = saplingPath + "_sapling";

        ResourceLocation saplingId = ResourceLocation.fromNamespaceAndPath(namespace, saplingPath);
        return BuiltInRegistries.BLOCK.getOptional(saplingId)
                .map(Block::defaultBlockState)
                .orElse(Blocks.OAK_SAPLING.defaultBlockState());
    }
}