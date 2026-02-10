package net.axes.naturalregrowth.block;

import net.axes.naturalregrowth.Config;
import net.axes.naturalregrowth.ModBlocks;
import net.axes.naturalregrowth.block.entity.HealingBlockEntity;
import net.axes.naturalregrowth.compat.dt.DTIntegration; // Import Compat
import net.axes.naturalregrowth.compat.dt.DTLoader;     // Import Loader
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
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

        // 1. If LOG, just heal. (Logs are structural, they don't need support)
        if (currentState.is(ModBlocks.HEALING_LOG.get())) {
            level.setBlockAndUpdate(pos, original);
            return;
        }

        // 2. If LEAF, check for IMMEDIATE support
        if (currentState.is(ModBlocks.HEALING_LEAF.get())) {
            if (isSafeToHeal(level, pos)) {
                // Connected to a tree -> Regrow!
                level.setBlockAndUpdate(pos, original);
            } else {
                // Floating in air with no support -> Decay! (Vanish)
                // Note: If you want them to wait longer instead of decaying, remove this else block.
                // But decaying cleans up "magic floating leaves".
                level.removeBlock(pos, false);
            }
        }
    }

    // --- REPLACED: NEW "CONTACT" CHECK ---
    private boolean isSafeToHeal(LevelReader level, BlockPos pos) {
        // We only check immediate neighbors (touching faces).
        // This forces the "Ripple Effect": Trunk -> Inner Leaves -> Outer Leaves.

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);

            // 1. Is it a Log? (Vanilla Log, Healing Log, Stump)
            if (isStableLog(neighborState)) return true;

            // 2. Is it a Real Leaf? (Vanilla Leaves)
            // We EXCLUDE healing leaves here. A leaf cannot heal off another ghost leaf.
            // It must wait for the neighbor to become real first.
            if (isStableLeaf(neighborState)) return true;

            // 3. Dynamic Trees Support
            if (DTLoader.isLoaded()) {
                if (DTIntegration.isDTBranch(neighborState)) return true;
                if (DTIntegration.isDTLeaf(neighborState)) return true;
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

    private boolean isStableLeaf(BlockState state) {
        // A "Stable Leaf" is a REAL leaf block.
        // It is NOT a Healing Leaf (Ghost).
        if (state.is(ModBlocks.HEALING_LEAF.get())) return false;

        return state.is(BlockTags.LEAVES);
    }

    // --- SURVIVAL DROPS ---

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        if (be instanceof HealingBlockEntity healer) {
            BlockState original = healer.getOriginalState();

            if (state.is(ModBlocks.HEALING_LOG.get())) {
                return Collections.singletonList(new ItemStack(original.getBlock()));
            }

            if (state.is(ModBlocks.HEALING_LEAF.get())) {
                return Collections.emptyList();
            }
        }

        return super.getDrops(state, params);
    }

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