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

        if (state.getBlock() instanceof RegrowingStumpBlock) return false;

        // 1. LOG LOGIC
        if (state.is(BlockTags.LOGS) || isPMWLog(state)) {
            BlockPos bottom = getTrueTreeBottom(level, pos);
            if (isNaturalTree(level, bottom)) {
                if (level.getBlockState(bottom).getBlock() instanceof RegrowingStumpBlock) {
                    return level.removeBlock(pos, isMoving);
                }
                infectTreeBase(level, bottom, state);
                return true;
            }
        }

        // 2. LEAF LOGIC
        if (state.is(BlockTags.LEAVES)) {
            BlockPos nearbyLog = findNeighborLog(level, pos);
            if (nearbyLog != null) {
                BlockPos stumpPos = getTrueTreeBottom(level, nearbyLog);
                BlockState stumpState = level.getBlockState(stumpPos);

                if (isNaturalTree(level, stumpPos) && !(stumpState.getBlock() instanceof RegrowingStumpBlock)) {
                    infectTreeBase(level, stumpPos, level.getBlockState(nearbyLog));
                }
            }
        }

        return level.removeBlock(pos, isMoving);
    }

    // --- SHARED HELPERS ---

    /**
     * Checks if the block below is valid "Natural" ground for a tree.
     * UPDATED: Now includes PM Weather's Charred/Smoldering/Scoured soil.
     */
    public static boolean isValidGround(BlockState ground) {
        // 1. Vanilla Tags
        if (ground.is(BlockTags.DIRT) ||
                ground.is(BlockTags.SAND) ||
                ground.is(Blocks.MANGROVE_ROOTS) ||
                ground.is(Blocks.CLAY) ||
                ground.is(Blocks.MOSS_BLOCK) ||
                ground.is(BlockTags.NYLIUM) ||
                ground.is(Blocks.END_STONE)) {
            return true;
        }

        // 2. PM Weather Soil Support (FIXED)
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(ground.getBlock());
        if (id.getNamespace().equals("pmweather")) {
            String path = id.getPath();

            // Critical Fix: Exclude LOGS and WOOD from being seen as dirt
            if (path.contains("log") || path.contains("wood") || path.contains("plank")) {
                return false;
            }

            //Check for soil keywords
            return path.contains("dirt") ||
                    path.contains("grass") ||
                    path.contains("scour") ||
                    path.contains("smolder"); // Will catch smoldering_dirt, but NOT smoldering_log
        }

        return false;
    }

    public static boolean hasArtificialNeighbors(Level level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockState s = level.getBlockState(pos.relative(dir));

            if (s.is(Blocks.COBBLESTONE) || s.is(Blocks.MOSSY_COBBLESTONE) ||
                    s.is(Blocks.BRICKS) || s.is(Blocks.STONE_BRICKS) ||
                    s.is(Blocks.NETHER_BRICKS) || s.is(Blocks.QUARTZ_BRICKS) ||
                    s.is(Blocks.GLASS) || s.is(Blocks.GLASS_PANE) ||
                    s.is(BlockTags.TERRACOTTA) || s.is(BlockTags.WOOL)) {
                return true;
            }

            if (s.is(BlockTags.DOORS) || s.is(BlockTags.TRAPDOORS) ||
                    s.is(BlockTags.BEDS) || s.is(Blocks.TORCH) ||
                    s.is(Blocks.WALL_TORCH) || s.is(Blocks.LANTERN)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNaturalTree(Level level, BlockPos bottomPos) {
        // 1. Ground Check (Uses updated logic)
        if (!isValidGround(level.getBlockState(bottomPos.below()))) return false;

        // 2. Structure Safety Check
        if (hasArtificialNeighbors(level, bottomPos)) return false;

        // 3. Height Check
        int height = measureTreeHeight(level, bottomPos);
        if (height < 3) return false;

        // 4. Crown Check
        if (height > 10 && !tryFind2x2Base(level, bottomPos, level.getBlockState(bottomPos).getBlock()).isEmpty()) {
            return true;
        }
        BlockPos estimatedTop = bottomPos.above(height);
        return hasLeavesNearby(level, estimatedTop);
    }

    // --- PRIVATE HELPERS ---

    private static BlockPos findNeighborLog(Level level, BlockPos center) {
        int radius = 2;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
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

    private static BlockPos getTrueTreeBottom(Level level, BlockPos startPos) {
        BlockPos cursor = startPos;
        int safety = 0;
        while (safety < 40) {
            if (level.getBlockState(cursor.below()).is(BlockTags.LOGS)) {
                cursor = cursor.below();
            } else {
                boolean foundDiagonal = false;
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
                if (!foundDiagonal) return cursor;
            }
            safety++;
        }
        return cursor;
    }

    private static int measureTreeHeight(Level level, BlockPos bottom) {
        BlockPos cursor = bottom;
        int height = 1;
        int safety = 0;
        while (safety < 60) {
            if (level.getBlockState(cursor.above()).is(BlockTags.LOGS)) {
                cursor = cursor.above();
                height++;
            } else {
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
                if (!foundDiag) break;
            }
            safety++;
        }
        return height;
    }

    private static boolean hasLeavesNearby(Level level, BlockPos topPos) {
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

    private static boolean isPMWLog(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!id.getNamespace().equals("pmweather")) return false;
        String path = id.getPath();
        return path.contains("charred_log") || path.contains("smoldering_log") || path.contains("rotted_log") || path.contains("stripped_rotted_log");
    }

    private static BlockState getSaplingFromLog(BlockState logState) {
        ResourceLocation logId = BuiltInRegistries.BLOCK.getKey(logState.getBlock());
        String namespace = logId.getNamespace();
        String path = logId.getPath().replace("stripped_", "");
        String saplingPath = path.replace("_log", "_sapling").replace("_wood", "_sapling");
        if (!saplingPath.endsWith("_sapling")) saplingPath = saplingPath + "_sapling";
        ResourceLocation saplingId = ResourceLocation.fromNamespaceAndPath(namespace, saplingPath);
        return BuiltInRegistries.BLOCK.getOptional(saplingId).map(Block::defaultBlockState).orElse(Blocks.OAK_SAPLING.defaultBlockState());
    }
}