package net.axes.naturalregrowth.compat.dt;

import com.dtteam.dynamictrees.api.network.BranchDestructionData;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.block.leaves.DynamicLeavesBlock;
import com.dtteam.dynamictrees.block.soil.SoilBlock;
import com.dtteam.dynamictrees.entity.FallingTreeEntity;
import com.dtteam.dynamictrees.tree.TreeHelper;
import com.dtteam.dynamictrees.tree.species.Species;
import com.mojang.logging.LogUtils;
import net.axes.naturalregrowth.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
        return markDoomed(level, pos, state, false);
    }

    /**
     * Marks a Dynamic Tree's root as fire-doomed soil so Natural Regrowth can replant
     * after {@link net.axes.naturalregrowth.Config.Common#fireRegrowthDelay}.
     * Call when PMWeather ignites a DT branch (smoldering replacement).
     */
    public static boolean handleWildfireDamage(Level level, BlockPos pos, BlockState state) {
        return markDoomed(level, pos, state, true);
    }

    private static boolean markDoomed(Level level, BlockPos pos, BlockState state, boolean isFire) {
        if (!(state.getBlock() instanceof BranchBlock branch)) return false;

        BlockPos rootPos = TreeHelper.findRootNode(level, pos);
        if (rootPos == BlockPos.ZERO) {
            // Network may already be damaged mid-fire — try neighboring branches.
            rootPos = findRootFromNeighbors(level, pos);
        }
        if (rootPos == BlockPos.ZERO) return false;

        BlockState rootState = level.getBlockState(rootPos);

        // Already doomed: upgrade wind→fire flag if needed, otherwise leave alone.
        if (rootState.is(DTRegistries.DOOMED_SOIL.get())) {
            if (isFire && level.getBlockEntity(rootPos) instanceof DoomedSoilBlockEntity doomedInfo) {
                doomedInfo.setIsFireDoomed(true);
            }
            return true;
        }

        // Prefer exact species from rooty soil; fall back to branch family common species.
        // Mid-fire getExactSpecies often fails once the network is partially burned.
        Species species = TreeHelper.getExactSpecies(level, pos);
        if (species == Species.NULL_SPECIES) {
            species = TreeHelper.getBestGuessSpecies(level, pos);
        }
        if (species == Species.NULL_SPECIES) {
            species = branch.getFamily().getCommonSpecies();
        }
        if (species == null || species == Species.NULL_SPECIES || !species.isValid()) {
            return false;
        }

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
            doomedInfo.setIsFireDoomed(isFire);
        }

        return true;
    }

    /**
     * When the ignited block's network is already broken, walk nearby branches for a root.
     */
    private static BlockPos findRootFromNeighbors(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (!(level.getBlockState(neighbor).getBlock() instanceof BranchBlock)) continue;
            BlockPos root = TreeHelper.findRootNode(level, neighbor);
            if (root != BlockPos.ZERO) return root;
        }
        // Thick trunks / vertical gaps after partial burns
        for (int dy = -3; dy <= 3; dy++) {
            if (dy == 0) continue;
            BlockPos neighbor = pos.above(dy);
            if (!(level.getBlockState(neighbor).getBlock() instanceof BranchBlock)) continue;
            BlockPos root = TreeHelper.findRootNode(level, neighbor);
            if (root != BlockPos.ZERO) return root;
        }
        return BlockPos.ZERO;
    }

    /** True if a DT branch (living or NR fire branch) is within a short radius of {@code pos}. */
    public static boolean isNearDTBranch(Level level, BlockPos pos, int horizontalRadius, int yDown, int yUp) {
        for (int r = 1; r <= horizontalRadius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    for (int y = -yDown; y <= yUp; y++) {
                        BlockPos check = pos.offset(x, y, z);
                        BlockState state = level.getBlockState(check);
                        if (state.getBlock() instanceof BranchBlock) {
                            return true;
                        }
                        // Fully burned trees may only have doomed soil left nearby.
                        if (state.is(DTRegistries.DOOMED_SOIL.get())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Clears the doomed tree and replants, matching vanilla stump behavior:
     * - drops only if {@link Config.Common#dropLogItems} is true
     * - applies {@link Config.Common#instantCatchUp} via {@link Species#transitionToTree}
     */
    public static void fellTree(Level level, BlockPos rootPos, Species species) {
        BlockPos cutPos = rootPos.above();
        BlockState cutState = level.getBlockState(cutPos);

        if (cutState.getBlock() instanceof BranchBlock branch) {
            BranchDestructionData destroyData =
                    branch.destroyBranchFromNode(level, cutPos, Direction.DOWN, false, null);

            if (Config.COMMON.dropLogItems.get()) {
                List<ItemStack> drops = destroyData.species.getBranchesDrops(level, destroyData.woodVolume);
                FallingTreeEntity.dropTree(level, destroyData, drops, FallingTreeEntity.DestroyType.HARVEST);
            }
            // When dropLogItems is false: destroyBranchFromNode already cleared the blocks;
            // skip dropTree entirely so no logs/sticks spawn.
        }

        // Root becomes normal dirt; sapling plants one block above.
        level.setBlock(rootPos, Blocks.DIRT.defaultBlockState(), 3);

        if (species == null || species == Species.NULL_SPECIES || !species.isValid()) {
            LOGGER.warn("Natural Regrowth: Invalid DT species while replanting at {}", cutPos);
            return;
        }

        if (!species.plantSapling(level, cutPos, true)) {
            LOGGER.warn("Natural Regrowth: Failed to replant Dynamic Tree at {}", cutPos);
            return;
        }

        // Match RegrowingStumpBlockEntity: optionally force young-tree growth (foliage) immediately.
        if (Config.COMMON.instantCatchUp.get() && level instanceof ServerLevel) {
            try {
                species.transitionToTree(level, cutPos);
            } catch (Exception e) {
                LOGGER.warn("Natural Regrowth: Failed to instant-grow DT sapling at {}: {}", cutPos, e.getMessage());
            }
        }
    }
}
