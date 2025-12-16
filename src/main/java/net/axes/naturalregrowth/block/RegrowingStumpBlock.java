package net.axes.naturalregrowth.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class RegrowingStumpBlock extends RotatedPillarBlock {

    public static final IntegerProperty TREE_TYPE = IntegerProperty.create("type", 0, 7);

    public RegrowingStumpBlock() {
        super(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).randomTicks());
        this.registerDefaultState(this.stateDefinition.any().setValue(TREE_TYPE, 0));
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 10% chance to regrow
        if (random.nextFloat() < 0.10f) {
            regrow(level, pos, state);
        }
    }

    // New helper method to handle the full regrowth process
    public void regrow(ServerLevel level, BlockPos pos, BlockState state) {
        // 1. Clean up the dead trunk above
        breakTrunkAbove(level, pos.above());

        // 2. Plant the sapling
        BlockState sapling = getSaplingFromType(state.getValue(TREE_TYPE));
        level.setBlock(pos, sapling, 3);
    }

    private void breakTrunkAbove(Level level, BlockPos startPos) {
        BlockPos cursor = startPos;
        int safety = 0;
        // Vaporize logs directly above the stump
        while (level.getBlockState(cursor).is(BlockTags.LOGS) && safety < 30) {
            level.destroyBlock(cursor, false);
            cursor = cursor.above();
            safety++;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TREE_TYPE);
    }

    private BlockState getSaplingFromType(int type) {
        switch (type) {
            case 1: return Blocks.SPRUCE_SAPLING.defaultBlockState();
            case 2: return Blocks.BIRCH_SAPLING.defaultBlockState();
            case 3: return Blocks.JUNGLE_SAPLING.defaultBlockState();
            case 4: return Blocks.ACACIA_SAPLING.defaultBlockState();
            case 5: return Blocks.DARK_OAK_SAPLING.defaultBlockState();
            case 6: return Blocks.CHERRY_SAPLING.defaultBlockState();
            case 7: return Blocks.MANGROVE_PROPAGULE.defaultBlockState();
            default: return Blocks.OAK_SAPLING.defaultBlockState();
        }
    }
}