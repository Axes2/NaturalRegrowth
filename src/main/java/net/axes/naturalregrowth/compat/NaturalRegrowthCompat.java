package net.axes.naturalregrowth.compat;

import net.axes.naturalregrowth.ModBlocks;
import net.axes.naturalregrowth.block.RegrowingStumpBlock;
import net.axes.naturalregrowth.block.entity.RegrowingStumpBlockEntity;
import net.axes.naturalregrowth.util.TreeUtils; // Uses your existing TreeUtils to avoid duplicate code
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

    /**
     * Attempts to remove a block using Natural Regrowth's logic.
     * If the block is a tree part, it may be replaced by a Stump or trigger tree infection.
     *
     * @param level    The level
     * @param pos      The position of the block to remove
     * @param isMoving Whether the block is moving (standard removeBlock param)
     * @return True if the block was removed (or replaced by a stump), False otherwise.
     */
    public static boolean removeBlockWithRegrowth(Level level, BlockPos pos, boolean isMoving) {
        BlockState state = level.getBlockState(pos);
        // Don't destroy stumps
        if (state.getBlock() instanceof RegrowingStumpBlock) return false;

        // LOG LOGIC
        if (state.is(BlockTags.LOGS)) {
            if (isNaturalTree(level, pos)) {
                BlockPos stumpPos = findStump(level, pos);
                if (level.getBlockState(stumpPos).getBlock() instanceof RegrowingStumpBlock) {
                    return level.removeBlock(pos, isMoving);
                }
                infectTreeBase(level, stumpPos, state);
                return true;
            }
        }

        // LEAF LOGIC
        if (state.is(BlockTags.LEAVES)) {
            BlockPos below = pos.below();
            BlockState stateBelow = level.getBlockState(below);
            if (stateBelow.is(BlockTags.LOGS)) {
                BlockPos stumpPos = findStump(level, below);
                BlockState stumpState = level.getBlockState(stumpPos);
                if (!(stumpState.getBlock() instanceof RegrowingStumpBlock)) {
                    infectTreeBase(level, stumpPos, stateBelow);
                }
            }
        }

        // Standard vanilla removal
        return level.removeBlock(pos, isMoving);
    }

    // --- INTERNAL HELPERS ---

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
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);

        // HARDENED CHECK:
        // 1. Must be DIRT (Vanilla check)
        // 2. Must NOT be LEAVES (Prevents canopy stumps)
        // 3. Must NOT be LOGS (Prevents stacking stumps)
        // 4. Must NOT be AIR (Prevents floating stumps)
        if (ground.is(BlockTags.DIRT)
                && !ground.is(BlockTags.LEAVES)
                && !ground.is(BlockTags.LOGS)
                && !ground.isAir()) {

            // Prevent overwrite of an existing stump
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

    public static boolean isNaturalTree(Level level, BlockPos pos) {
        BlockPos cursor = pos;
        int height = 0; // Effectively "Height Above"
        boolean hasTrunkVegetation = false;

        // 1. Scan UP (Your existing logic)
        while (level.getBlockState(cursor.above()).is(BlockTags.LOGS) && height < 60) {
            cursor = cursor.above();
            height++;
            if (!hasTrunkVegetation) {
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockState sideState = level.getBlockState(cursor.relative(dir));
                    if (sideState.is(Blocks.VINE) || sideState.is(Blocks.COCOA)) {
                        hasTrunkVegetation = true;
                        break;
                    }
                }
            }
        }

        // --- NEW: BUSH FILTER ---

        BlockPos downCursor = pos;
        int heightBelow = 0;
        // Only need 1 block down to prove the total height is at least 2
        while (level.getBlockState(downCursor.below()).is(BlockTags.LOGS) && heightBelow < 1) {
            downCursor = downCursor.below();
            heightBelow++;
        }

        // If the total height is less than 2 ignore
        if ((height + heightBelow + 1) < 2) {
            return false;
        }

        // 2. 2x2 check
        if (height > 20) {
            if (!tryFind2x2Base(level, pos, level.getBlockState(pos).getBlock()).isEmpty()) return true;
        }

        // 3. Vegetation Backup
        if (hasTrunkVegetation && height > 6) return true;

        // 4. Crown Scanning
        BlockPos top = cursor;
        int searchDown = 5;
        int searchUp = 2;

        for (int y = -searchDown; y <= searchUp; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    BlockState state = level.getBlockState(top.offset(x, y, z));
                    if (state.is(BlockTags.LEAVES) && !state.getValue(LeavesBlock.PERSISTENT)) return true;
                }
            }
        }

        // 5. Outer Ring Scan
        int radiusXZ = 3;
        for (int y = -searchDown; y <= searchUp; y++) {
            for (int x = -radiusXZ; x <= radiusXZ; x++) {
                for (int z = -radiusXZ; z <= radiusXZ; z++) {
                    if (Math.abs(x) <= 1 && Math.abs(z) <= 1) continue;
                    BlockPos leafPos = top.offset(x, y, z);
                    BlockState state = level.getBlockState(leafPos);
                    if (state.is(BlockTags.LEAVES) && !state.getValue(LeavesBlock.PERSISTENT)) {
                        for (Direction d : Direction.values()) {
                            if (level.getBlockState(leafPos.relative(d)).is(BlockTags.LOGS)) return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static BlockPos findStump(Level level, BlockPos pos) {
        BlockPos cursor = pos;
        int safety = 0;
        while (level.getBlockState(cursor.below()).is(BlockTags.LOGS) && safety < 30) {
            cursor = cursor.below();
            safety++;
        }
        return cursor;
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