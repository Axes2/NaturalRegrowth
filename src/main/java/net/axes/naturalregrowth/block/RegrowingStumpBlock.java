package net.axes.naturalregrowth.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class RegrowingStumpBlock extends RotatedPillarBlock {

    // We use a number to remember which tree type this was (0=Oak, 1=Spruce, etc.)
    public static final IntegerProperty TREE_TYPE = IntegerProperty.create("type", 0, 8);

    public RegrowingStumpBlock() {
        // Copy the properties of a regular Oak Log (Strength, Sound, Flammability)
        super(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).randomTicks());
        this.registerDefaultState(this.stateDefinition.any().setValue(TREE_TYPE, 0));
    }

    // 1. Tell the block to accept "Random Ticks" (like crops growing)
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    // 2. The Logic: What happens when it ticks?
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 10% chance per tick to "heal" (adjust this to make it faster/slower)
        if (random.nextFloat() < 0.10f) {
            BlockState sapling = getSaplingFromType(state.getValue(TREE_TYPE));
            level.setBlock(pos, sapling, 3);
        }
    }

    // 3. Register the property so the game knows to save it
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TREE_TYPE);
    }

    // Helper: Convert our Number back into a Sapling
    private BlockState getSaplingFromType(int type) {
        switch (type) {
            case 1: return Blocks.SPRUCE_SAPLING.defaultBlockState();
            case 2: return Blocks.BIRCH_SAPLING.defaultBlockState();
            case 3: return Blocks.JUNGLE_SAPLING.defaultBlockState();
            case 4: return Blocks.ACACIA_SAPLING.defaultBlockState();
            case 5: return Blocks.DARK_OAK_SAPLING.defaultBlockState();
            case 6: return Blocks.CHERRY_SAPLING.defaultBlockState();
            case 7: return Blocks.MANGROVE_PROPAGULE.defaultBlockState();
            default: return Blocks.OAK_SAPLING.defaultBlockState(); // Case 0
        }
    }
}