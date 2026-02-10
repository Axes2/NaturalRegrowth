package net.axes.naturalregrowth.compat.dt;

import com.dtteam.dynamictrees.api.network.BranchDestructionData;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.block.leaves.DynamicLeavesBlock;
import com.dtteam.dynamictrees.block.soil.SoilBlock;
import com.dtteam.dynamictrees.entity.FallingTreeEntity;
import com.dtteam.dynamictrees.tree.TreeHelper;
import com.dtteam.dynamictrees.tree.species.Species;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.List;

public class DTIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();

    // --- SPLIT CHECKS ---
    public static boolean isDTBranch(BlockState state) {
        return state.getBlock() instanceof BranchBlock;
    }

    public static boolean isDTLeaf(BlockState state) {
        return state.getBlock() instanceof DynamicLeavesBlock;
    }
    // --------------------

    public static boolean handleStormDamage(Level level, BlockPos pos, BlockState state) {
        // ... (Keep existing handleStormDamage code exactly as is) ...
        if (!(state.getBlock() instanceof BranchBlock)) return false;

        BlockPos rootPos = TreeHelper.findRootNode(level, pos);
        if (rootPos == BlockPos.ZERO) return false;

        BlockState rootState = level.getBlockState(rootPos);

        if (rootState.is(DTRegistries.DOOMED_SOIL.get())) return true;

        Species species = TreeHelper.getExactSpecies(level, rootPos);
        if (species == Species.NULL_SPECIES) return false;

        int fertility = 0;
        if (rootState.getBlock() instanceof SoilBlock) {
            fertility = rootState.getValue(SoilBlock.FERTILITY);
        }

        BlockState doomedState = DTRegistries.DOOMED_SOIL.get().defaultBlockState()
                .setValue(SoilBlock.FERTILITY, fertility);

        level.setBlock(rootPos, doomedState, 3);

        if (level.getBlockEntity(rootPos) instanceof DoomedSoilBlockEntity doomedInfo) {
            doomedInfo.setSpecies(species.getRegistryName());
            doomedInfo.startTimer(level.getGameTime());
        }

        return true;
    }

    public static void fellTree(Level level, BlockPos rootPos, Species species) {
        // ... (Keep existing fellTree code exactly as is) ...
        BlockPos cutPos = rootPos.above();
        BlockState cutState = level.getBlockState(cutPos);

        if (!(cutState.getBlock() instanceof BranchBlock branch)) {
            level.setBlock(rootPos, Blocks.DIRT.defaultBlockState(), 3);
            return;
        }

        BranchDestructionData destroyData = branch.destroyBranchFromNode(level, cutPos, Direction.DOWN, false, null);

        List<ItemStack> drops = destroyData.species.getBranchesDrops(level, destroyData.woodVolume);
        FallingTreeEntity.dropTree(level, destroyData, drops, FallingTreeEntity.DestroyType.HARVEST);

        level.setBlock(rootPos, Blocks.DIRT.defaultBlockState(), 3);

        if (!species.plantSapling(level, cutPos, true)) {
            LOGGER.warn("Natural Regrowth: Failed to replant Dynamic Tree at {}", cutPos);
        }
    }
}