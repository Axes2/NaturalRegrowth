package net.axes.naturalregrowth.block;

import net.axes.naturalregrowth.Config;
import net.axes.naturalregrowth.block.entity.RegrowingStumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class RegrowingStumpBlock extends Block implements EntityBlock {

    public RegrowingStumpBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RegrowingStumpBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof RegrowingStumpBlockEntity stump) {
            return new ItemStack(stump.getMimicState().getBlock());
        }
        return new ItemStack(Blocks.STRIPPED_OAK_WOOD);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof RegrowingStumpBlockEntity stump) {
            return List.of(new ItemStack(stump.getMimicState().getBlock()));
        }
        return List.of(new ItemStack(Blocks.STRIPPED_OAK_LOG));
    }

    // --- THE REGROWTH LOGIC ---

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isClientSide) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RegrowingStumpBlockEntity stump) {
            long age = level.getGameTime() - stump.getCreationTime();
            if (age < Config.COMMON.regrowthDelay.get()) return;

            if (random.nextFloat() > Config.COMMON.regrowthChance.get()) return;

            stump.performRegrowth(level, pos);
        }
    }

    // --- HELPER METHODS ---

    public static void destroyTreeFloodFill(ServerLevel level, BlockPos startPos) {
        int maxLogs = 300;
        boolean shouldDrop = Config.COMMON.dropLogItems.get();

        // --- PHASE 1: SAFE SCAN ---
        List<BlockPos> logsToBreak = new ArrayList<>();
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        int minObservedY = startPos.getY();

        if (level.getBlockState(startPos).is(BlockTags.LOGS)) {
            queue.add(startPos);
            visited.add(startPos);
        }

        while (!queue.isEmpty() && logsToBreak.size() < maxLogs) {
            BlockPos currentPos = queue.poll();

            if (currentPos.getY() < minObservedY) minObservedY = currentPos.getY();

            // SECURITY CHECK: Is this log touching a House Block?
            if (hasCivilizedNeighbor(level, currentPos)) {
                return; // ABORT: Attached to a house.
            }

            logsToBreak.add(currentPos);

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;

                        BlockPos targetPos = currentPos.offset(x, y, z);
                        if (!visited.contains(targetPos)) {
                            BlockState targetState = level.getBlockState(targetPos);

                            if (targetState.is(BlockTags.LOGS) && !(targetState.getBlock() instanceof RegrowingStumpBlock)) {
                                visited.add(targetPos);
                                queue.add(targetPos);
                            }
                        }
                    }
                }
            }
        }

        // --- PHASE 2: GEOMETRY VERDICT (The "Footprint" Check) ---
        int baseCount = 0;
        for (BlockPos p : logsToBreak) {
            if (p.getY() == minObservedY) {
                baseCount++;
            }
        }
        if (baseCount > 4) return; // ABORT: Base is too wide (Wall/Foundation)

        // --- PHASE 3: DESTRUCTION ---
        for (BlockPos pos : logsToBreak) {
            breakLogWithExtras(level, pos, shouldDrop);
        }
    }

    private static boolean hasCivilizedNeighbor(ServerLevel level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(dir));
            if (isCivilized(neighbor)) return true;
        }
        return false;
    }

    /**
     * The expanded list of checks to detect player structures.
     */
    private static boolean isCivilized(BlockState state) {
        // 1. Check Tags (Covers all wood types/colors)
        if (state.is(BlockTags.DOORS) ||
                state.is(BlockTags.TRAPDOORS) ||
                state.is(BlockTags.FENCES) ||
                state.is(BlockTags.FENCE_GATES) ||
                state.is(BlockTags.STAIRS) ||
                state.is(BlockTags.SLABS) ||
                state.is(BlockTags.WALLS) ||
                state.is(BlockTags.BEDS) ||
                state.is(BlockTags.SIGNS) ||
                state.is(BlockTags.BANNERS) ||
                state.is(BlockTags.CANDLES) ||
                state.is(BlockTags.BUTTONS) ||
                state.is(BlockTags.PRESSURE_PLATES) ||
                state.is(BlockTags.WOOL) ||
                state.is(BlockTags.WOOL_CARPETS) ||
                state.is(BlockTags.RAILS) ||
                state.is(BlockTags.ANVIL) ||
                state.is(BlockTags.SHULKER_BOXES)) {
            return true;
        }

        // 2. Check Specific Structural Blocks
        if (state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE) ||
                state.is(Blocks.TINTED_GLASS) ||
                state.is(Blocks.BRICKS) || state.is(Blocks.STONE_BRICKS) ||
                state.is(Blocks.NETHER_BRICKS) || state.is(Blocks.QUARTZ_BRICKS) ||
                state.is(Blocks.COBBLESTONE) || state.is(Blocks.MOSSY_COBBLESTONE) ||
                state.is(Blocks.IRON_BARS) || state.is(Blocks.CHAIN) ||
                state.is(Blocks.LADDER) || state.is(Blocks.SCAFFOLDING) ||
                state.is(Blocks.FLOWER_POT) || state.is(Blocks.BOOKSHELF)) {
            return true;
        }

        // 3. Check Utilities/Redstone
        if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) ||
                state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN) ||
                state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.BARREL) ||
                state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.SMOKER) ||
                state.is(Blocks.CRAFTING_TABLE) || state.is(Blocks.ENCHANTING_TABLE) ||
                state.is(Blocks.BREWING_STAND) || state.is(Blocks.JUKEBOX) || state.is(Blocks.NOTE_BLOCK) ||
                state.is(Blocks.PISTON) || state.is(Blocks.STICKY_PISTON) || state.is(Blocks.OBSERVER) ||
                state.is(Blocks.HOPPER) || state.is(Blocks.DISPENSER) || state.is(Blocks.DROPPER) ||
                state.is(Blocks.REDSTONE_WIRE) || state.is(Blocks.REPEATER) || state.is(Blocks.COMPARATOR) ||
                state.is(Blocks.LEVER) || state.is(Blocks.TRIPWIRE_HOOK) || state.is(Blocks.BELL)) {
            return true;
        }

        return false;
    }

    private static void breakLogWithExtras(ServerLevel level, BlockPos pos, boolean drop) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockState state = level.getBlockState(neighbor);
            if (state.getBlock() instanceof CocoaBlock || state.is(Blocks.VINE)) {
                level.destroyBlock(neighbor, drop);
            }
        }
        level.destroyBlock(pos, drop);
    }
}