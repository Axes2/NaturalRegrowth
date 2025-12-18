package net.axes.naturalregrowth.block;

import net.axes.naturalregrowth.Config;
import net.axes.naturalregrowth.block.entity.RegrowingStumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class RegrowingStumpBlock extends Block implements EntityBlock {

    public RegrowingStumpBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.0f)
                .sound(SoundType.WOOD)
                .noOcclusion() // Fixes the lighting/shadow issue
                .randomTicks());
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
        return new ItemStack(net.minecraft.world.level.block.Blocks.STRIPPED_OAK_WOOD);
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
        return List.of(new ItemStack(net.minecraft.world.level.block.Blocks.STRIPPED_OAK_LOG));
    }

    // --- THE REGROWTH LOGIC ---

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isClientSide) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RegrowingStumpBlockEntity stump) {

            // CHECK 1: Are we old enough yet?
            // If the delay is 5 minutes, and we are only 2 minutes old, STOP HERE.
            long age = level.getGameTime() - stump.getCreationTime();
            if (age < Config.COMMON.regrowthDelay.get()) {
                return;
            }

            // CHECK 2: The Lottery (Random Chance)
            // Even if we are old enough, we still have to roll the dice.
            // This ensures the forest heals gradually, not all at once.
            if (random.nextFloat() > Config.COMMON.regrowthChance.get()) {
                return;
            }

            // If both passed, Grow!
            destroyTreeFloodFill(level, pos.above());
            BlockState saplingToGrow = stump.getFutureSapling();
            level.setBlock(pos, saplingToGrow, 3);
        }
    }

    // --- HELPER METHODS ---

    private void destroyTreeFloodFill(ServerLevel level, BlockPos startPos) {
        int maxLogs = 300;
        int currentLogs = 0;
        boolean shouldDrop = Config.COMMON.dropLogItems.get();

        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        // FIX: Explicitly check and destroy the STARTING block (the one directly above the stump)
        if (level.getBlockState(startPos).is(BlockTags.LOGS)) {
            level.destroyBlock(startPos, shouldDrop);
            currentLogs++;
        }

        // Now add it to the queue so we can find its branches
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

                            if (targetState.is(BlockTags.LOGS)) {
                                level.destroyBlock(targetPos, shouldDrop);
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
}