package net.axes.naturalregrowth.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.axes.naturalregrowth.Config;

import java.util.Collections;
import java.util.List;

public class RegrowingStumpBlock extends RotatedPillarBlock {

    public static final IntegerProperty TREE_TYPE = IntegerProperty.create("type", 0, 7);

    public RegrowingStumpBlock() {
        // Properties copied from Oak Log (Strength, Sound, Tool requirement)
        super(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).randomTicks());
        this.registerDefaultState(this.stateDefinition.any().setValue(TREE_TYPE, 0));
    }

    // --- DROP LOGIC ---
    // When broken in Survival, drop the corresponding Stripped Wood item
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        int type = state.getValue(TREE_TYPE);
        Item dropItem;

        switch (type) {
            case 1: dropItem = Items.STRIPPED_SPRUCE_WOOD; break;
            case 2: dropItem = Items.STRIPPED_BIRCH_WOOD; break;
            case 3: dropItem = Items.STRIPPED_JUNGLE_WOOD; break;
            case 4: dropItem = Items.STRIPPED_ACACIA_WOOD; break;
            case 5: dropItem = Items.STRIPPED_DARK_OAK_WOOD; break;
            case 6: dropItem = Items.STRIPPED_CHERRY_WOOD; break;
            case 7: dropItem = Items.STRIPPED_MANGROVE_WOOD; break;
            default: dropItem = Items.STRIPPED_OAK_WOOD; break; // Case 0
        }

        return Collections.singletonList(new ItemStack(dropItem));
    }
    // ------------------

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {

        if (random.nextFloat() < Config.COMMON.regrowthChance.get()) {
            regrow(level, pos, state);
        }
    }

    public void regrow(ServerLevel level, BlockPos pos, BlockState state) {
        // 1. Clean up the dead trunk above
        breakTrunkAbove(level, pos.above());

        // 2. Plant the sapling (No sound/particles, just silent growth)
        BlockState sapling = getSaplingFromType(state.getValue(TREE_TYPE));
        level.setBlock(pos, sapling, 3);
    }

    private void breakTrunkAbove(Level level, BlockPos startPos) {
        BlockPos cursor = startPos;
        int safety = 0;

        // Use Config for drops
        boolean shouldDrop = Config.COMMON.dropLogItems.get();

        while (level.getBlockState(cursor).is(BlockTags.LOGS) && safety < 30) {
            level.destroyBlock(cursor, shouldDrop);
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