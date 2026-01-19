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
        // 1. Get the Block Entity involved in this break event
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        if (be instanceof RegrowingStumpBlockEntity stump) {
            // 2. Drop the "Mimic" block (e.g. Stripped Mahogany Log)
            return List.of(new ItemStack(stump.getMimicState().getBlock()));
        }

        // 3. Fallback (Safe default)
        return List.of(new ItemStack(Blocks.STRIPPED_OAK_LOG));
    }

    // --- THE REGROWTH LOGIC ---

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isClientSide) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RegrowingStumpBlockEntity stump) {

            // CHECK 1: Are we old enough yet?
            long age = level.getGameTime() - stump.getCreationTime();
            if (age < Config.COMMON.regrowthDelay.get()) {
                return;
            }

            // CHECK 2: The Lottery (Random Chance)
            if (random.nextFloat() > Config.COMMON.regrowthChance.get()) {
                return;
            }

            // CHECK 3: Delegate to BlockEntity (Handles 2x2 Logic)
            stump.performRegrowth(level, pos);
        }
    }

    // --- HELPER METHODS ---

    public static void destroyTreeFloodFill(ServerLevel level, BlockPos startPos) {
        int maxLogs = 300;
        int currentLogs = 0;
        boolean shouldDrop = Config.COMMON.dropLogItems.get();

        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        // Helper check for the first block
        if (level.getBlockState(startPos).is(BlockTags.LOGS)) {
            breakLogWithExtras(level, startPos, shouldDrop); // <--- Updated call
            currentLogs++;
        }

        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty() && currentLogs < maxLogs) {
            BlockPos currentPos = queue.poll();

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;

                        BlockPos targetPos = currentPos.offset(x, y, z);

                        if (!visited.contains(targetPos)) {
                            BlockState targetState = level.getBlockState(targetPos);

                            // target LOGS, EXCLUDE STUMPS.
                            if (targetState.is(BlockTags.LOGS) && !(targetState.getBlock() instanceof RegrowingStumpBlock)) {
                                breakLogWithExtras(level, targetPos, shouldDrop); // <--- Updated call
                                visited.add(targetPos);
                                queue.add(targetPos);
                                currentLogs++;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Safely breaks a log AND its dependent neighbors (Cocoa, Vines) to prevent floating blocks.
     */
    private static void breakLogWithExtras(ServerLevel level, BlockPos pos, boolean drop) {
        // 1. Check all 6 sides for attached "extras"
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockState state = level.getBlockState(neighbor);

            //Cocoa Pod or Vine attached to this log, break it too
            if (state.getBlock() instanceof CocoaBlock || state.is(Blocks.VINE)) {
                level.destroyBlock(neighbor, drop);
            }
        }

        // 2. Break the log itself
        level.destroyBlock(pos, drop);
    }
}