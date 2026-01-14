package net.axes.naturalregrowth.block;

import net.axes.naturalregrowth.Config;
import net.axes.naturalregrowth.ModBlocks;
import net.axes.naturalregrowth.block.entity.HealingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

import java.util.Collections;
import java.util.List;

public class HealingBlock extends Block implements EntityBlock {

    public HealingBlock(Properties properties) {
        super(properties);
    }
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // If it's a Leaf, return EMPTY (No hitbox, no outline)
        if (state.is(ModBlocks.HEALING_LEAF.get())) {
            return Shapes.empty();
        }
        // If it's a Log, return a FULL CUBE (Standard block)
        return Shapes.block();
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {

        // NEW: Configurable Chance
        // Default 0.2, 20% chance per tick
        if (random.nextFloat() < Config.COMMON.healingChance.get()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HealingBlockEntity healer) {
                attemptHealOrDecay(level, pos, state, healer);
            }
        }
    }

    private void attemptHealOrDecay(ServerLevel level, BlockPos pos, BlockState currentState, HealingBlockEntity healer) {
        BlockState original = healer.getOriginalState();

        // 1. If LOG, just heal.
        if (currentState.is(ModBlocks.HEALING_LOG.get())) {
            level.setBlockAndUpdate(pos, original);
            return;
        }

        // 2. If LEAF, check for support
        if (currentState.is(ModBlocks.HEALING_LEAF.get())) {
            if (isSafeToHeal(level, pos)) {
                // Connected to a tree -> Regrow!
                level.setBlockAndUpdate(pos, original);
            } else {
                // Floating in air -> Decay! (Vanish)
                level.removeBlock(pos, false);
            }
        }
    }

    // --- BFS SEARCH FOR "STABLE LOG" ---
    private boolean isSafeToHeal(LevelReader level, BlockPos startPos) {
        // Range: 6 blocks (Standard vanilla leaf distance)
        int range = 6;

        // Simple optimization: Check immediate neighbors first
        for (Direction d : Direction.values()) {
            if (isStableLog(level.getBlockState(startPos.relative(d)))) return true;
        }

        // BFS (Breadth-First Search)
        // We look for a path from 'startPos' to any 'Stable Log' passing only through 'Healing Leaves' or 'Leaves'.
        // To keep performance high, we won't implement a full recursive BFS here unless necessary.
        // Instead, we will do a simplified check:
        // "Is there a Log within range?"

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    cursor.setWithOffset(startPos, x, y, z);
                    BlockState state = level.getBlockState(cursor);

                    // Found a support beam!
                    if (isStableLog(state)) {
                        // In a perfect world, we check connectivity.
                        // For a "Random Tick" optimization, distance check is usually "Good Enough".
                        if (cursor.distSqr(startPos) <= range * range) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isStableLog(BlockState state) {
        // A "Stable Log" is any Log, Stump, or Healing Log.
        // It is NOT a Healing Leaf.
        if (state.is(ModBlocks.HEALING_LEAF.get())) return false;
        if (state.is(ModBlocks.HEALING_LOG.get())) return true;
        if (state.is(ModBlocks.REGROWING_STUMP.get())) return true;

        return state.is(BlockTags.LOGS);
    }

    // --- SURVIVAL DROPS ---

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // Get the TileEntity to see what we are holding
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        if (be instanceof HealingBlockEntity healer) {
            BlockState original = healer.getOriginalState();

            // If it was a Log, drop the Item of that Log
            if (state.is(ModBlocks.HEALING_LOG.get())) {
                return Collections.singletonList(new ItemStack(original.getBlock()));
            }

            // If it was a Leaf, drop NOTHING (Ghost block behavior)
            if (state.is(ModBlocks.HEALING_LEAF.get())) {
                return Collections.emptyList();
            }
        }

        return super.getDrops(state, params);
    }

    // Handle Pick Block (Creative Mode Middle Click)
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof HealingBlockEntity healer) {
            return new ItemStack(healer.getOriginalState().getBlock());
        }
        return super.getCloneItemStack(level, pos, state);
    }


    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HealingBlockEntity(pos, state);
    }
}