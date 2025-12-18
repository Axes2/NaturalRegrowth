package net.axes.naturalregrowth.block;

import net.axes.naturalregrowth.Config;
import net.axes.naturalregrowth.block.entity.RegrowingStumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
// CHANGE 1: We implement "EntityBlock" to attach the Block Entity
public class RegrowingStumpBlock extends Block implements EntityBlock {

    public RegrowingStumpBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.0f)
                .sound(SoundType.WOOD)
                .noOcclusion() // FIX 1: Let light pass through!
                .randomTicks());
    }

    // CHANGE 2: Create the "Brain" when placed
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RegrowingStumpBlockEntity(pos, state);
    }

    // CHANGE 3: The New Growth Logic
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isClientSide) return;

        // check config chance
        if (random.nextFloat() > Config.COMMON.regrowthChance.get()) {
            return;
        }

        // Get our Block Entity
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RegrowingStumpBlockEntity stump) {

            // ASK THE BRAIN: "What should I turn into?"
            BlockState saplingToGrow = stump.getFutureSapling();

            // Turn into that sapling
            level.setBlock(pos, saplingToGrow, 3);
        }
    }
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        // Get the "Brain"
        if (level.getBlockEntity(pos) instanceof RegrowingStumpBlockEntity stump) {
            // Give the player the block it is mimicking (e.g., Stripped Mahogany)
            return new ItemStack(stump.getMimicState().getBlock());
        }
        // Fallback if something goes wrong
        return new ItemStack(net.minecraft.world.level.block.Blocks.STRIPPED_OAK_WOOD);
    }
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
}