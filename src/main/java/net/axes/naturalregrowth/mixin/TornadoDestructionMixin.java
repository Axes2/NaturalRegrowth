package net.axes.naturalregrowth.mixin;

import dev.protomanly.pmweather.weather.Storm; // <--- The CORRECT Import found in line 6
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Storm.class)
public class TornadoDestructionMixin {

    // We target the "removeBlock" call inside the "tick" method.
    // This catches both simple destruction AND when blocks are sucked up as entities.
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    public boolean onStormRemoveBlock(Level level, BlockPos pos, boolean isMoving) {

        // 1. Get the block BEFORE it is removed
        BlockState state = level.getBlockState(pos);

        // 2. Is it a Log?
        if (state.is(BlockTags.LOGS)) {
            // 3. Perform the "Smart Scan" to distinguish Trees from Houses
            if (isNaturalTree(level, pos)) {
                handleTreeRegeneration(level, pos);
            }
        }

        // 4. Proceed with the original removal (So the storm still destroys/sucks up the block)
        return level.removeBlock(pos, isMoving);
    }

    private boolean isNaturalTree(Level level, BlockPos pos) {
        // Since the storm destroys Top-Down, the leaves ABOVE might be gone.
        // We scan a 3x3 area horizontally around the log to find context.

        boolean foundNaturalLeaves = false;

        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 1; y++) { // Check same level and one up
                for (int z = -2; z <= 2; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    BlockPos checkPos = pos.offset(x, y, z);
                    BlockState neighbor = level.getBlockState(checkPos);

                    // A. BAD SIGNS (It's a house)
                    if (neighbor.is(Blocks.COBBLESTONE) ||
                            neighbor.is(Blocks.OAK_PLANKS) ||
                            neighbor.is(Blocks.GLASS) ||
                            neighbor.is(Blocks.GLASS_PANE) ||
                            neighbor.is(Blocks.BRICKS) ||
                            neighbor.is(Blocks.STONE_BRICKS)) {
                        return false; // ABORT! It's likely a player build.
                    }

                    // B. GOOD SIGNS (It's a tree)
                    if (neighbor.is(BlockTags.LEAVES)) {
                        if (!neighbor.getValue(LeavesBlock.PERSISTENT)) {
                            foundNaturalLeaves = true;
                        } else {
                            return false; // Persistent leaves = Player placed hedge/decoration
                        }
                    }
                }
            }
        }
        return foundNaturalLeaves;
    }

    private void handleTreeRegeneration(Level level, BlockPos pos) {
        // 1. Find the ground (Dirt/Grass) below the tree
        BlockPos groundPos = pos;
        int safety = 0;

        // Scan down until we hit something that ISN'T a log or Air
        while ((level.getBlockState(groundPos).is(BlockTags.LOGS) || level.getBlockState(groundPos).isAir()) && safety < 20) {
            groundPos = groundPos.below();
            safety++;
        }

        // 2. If we found dirt, plant a sapling
        if (level.getBlockState(groundPos).is(BlockTags.DIRT)) {
            // We plant the sapling ONE block above the dirt
            level.setBlock(groundPos.above(), Blocks.OAK_SAPLING.defaultBlockState(), 3);

            // OPTIONAL: You can add logic here to pick Birch/Spruce saplings based on the log type!
        }
    }
}