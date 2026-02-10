package net.axes.naturalregrowth.compat.dt;

import com.dtteam.dynamictrees.block.soil.SoilBlock;
import com.dtteam.dynamictrees.block.soil.SoilHelper;
import net.axes.naturalregrowth.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

public class DoomedSoilBlock extends SoilBlock {

    public DoomedSoilBlock() {
        super(SoilHelper.getProperties(Blocks.DIRT),
                BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).randomTicks().strength(0.5F));
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DoomedSoilBlockEntity(pos, state);
    }

    @Override
    public net.minecraft.world.level.block.Block getPrimitiveSoilBlock() {
        return Blocks.DIRT;
    }

    @Override
    public BlockState getPrimitiveSoilState(BlockState currentSoilState) {
        return Blocks.DIRT.defaultBlockState();
    }

    @Override
    public BlockState getDecayBlockState(BlockState state, BlockGetter level, BlockPos pos) {
        return Blocks.DIRT.defaultBlockState();
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isClientSide) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof DoomedSoilBlockEntity doomed)) return;

        long age = level.getGameTime() - doomed.getCreationTime();
        int delay = Config.COMMON.regrowthDelay.get();
        double chance = Config.COMMON.regrowthChance.get();

        if (age < delay) return;
        if (random.nextFloat() > chance) return;

        doomed.performRegrowth(level, pos);
    }
}