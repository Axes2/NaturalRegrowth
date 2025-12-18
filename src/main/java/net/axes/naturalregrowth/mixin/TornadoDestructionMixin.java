package net.axes.naturalregrowth.mixin;

import dev.protomanly.pmweather.weather.Storm;
import net.axes.naturalregrowth.Config;
import net.axes.naturalregrowth.ModBlocks;
import net.axes.naturalregrowth.block.RegrowingStumpBlock;
import net.axes.naturalregrowth.block.entity.RegrowingStumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities; // NEW IMPORT
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.core.registries.BuiltInRegistries; // To look up block names
import net.minecraft.resources.ResourceLocation;        // To handle "modid:blockname"
import java.util.Optional;                              // For safe checking

@Mixin(Storm.class)
public class TornadoDestructionMixin {

    @Redirect(method = "doDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    public boolean onStormRemoveBlock(Level level, BlockPos pos, boolean isMoving) {

        BlockState state = level.getBlockState(pos);

        // 1. INVINCIBILITY: Save our stumps!
        if (state.getBlock() instanceof RegrowingStumpBlock) {
            return false;
        }

        // 2. LOG LOGIC: Direct Hits
        if (state.is(BlockTags.LOGS)) {
            if (isNaturalTree(level, pos)) {
                BlockPos stumpPos = findStump(level, pos);

                // If the stump is already placed, just let the storm destroy this specific log
                if (level.getBlockState(stumpPos).getBlock() instanceof RegrowingStumpBlock) {
                    return level.removeBlock(pos, isMoving);
                }

                infectTreeBase(level, stumpPos, state);
                breakTrunk(level, stumpPos.above());

                return true;
            }
        }

        // 3. LEAF LOGIC: Glancing Blows
        if (state.is(BlockTags.LEAVES)) {
            BlockPos below = pos.below();
            BlockState stateBelow = level.getBlockState(below);

            if (stateBelow.is(BlockTags.LOGS)) {
                BlockPos stumpPos = findStump(level, below);
                BlockState stumpState = level.getBlockState(stumpPos);

                if (!(stumpState.getBlock() instanceof RegrowingStumpBlock)) {
                    infectTreeBase(level, stumpPos, stateBelow);
                }
            }
        }

        return level.removeBlock(pos, isMoving);
    }

    // --- HELPER METHODS ---

    private void infectTreeBase(Level level, BlockPos stumpPos, BlockState originalLog) {
        if (level.getBlockState(stumpPos.below()).is(BlockTags.DIRT)) {

            // 1. Determine the "Stripped" version (The Disguise)
            // We use our new helper method to simulate an axe strip
            BlockState disguise = getStrippedLog(level, stumpPos, originalLog);

            // 2. Determine the Sapling (The Future)
            // Default to Oak for now (Logic to be improved later)
            BlockState sapling = getSaplingFromLog(originalLog);

            // 3. Place the Stump Block
            level.setBlock(stumpPos, ModBlocks.REGROWING_STUMP.get().defaultBlockState(), 3);

            // 4. Configure the "Brain" (Block Entity)
            BlockEntity be = level.getBlockEntity(stumpPos);
            if (be instanceof RegrowingStumpBlockEntity stump) {
                stump.setMimic(disguise, sapling);
            }
        }
    }

    private void breakTrunk(Level level, BlockPos startPos) {
        BlockPos cursor = startPos;
        int safety = 0;
        // Access config safely
        boolean shouldDrop = Config.COMMON.dropLogItems.get();

        while (level.getBlockState(cursor).is(BlockTags.LOGS) && safety < 30) {
            level.destroyBlock(cursor, shouldDrop);
            cursor = cursor.above();
            safety++;
        }
    }

    private BlockState getStrippedLog(Level level, BlockPos pos, BlockState originalLog) {
        if (level instanceof ServerLevel serverLevel) {
            var fakePlayer = FakePlayerFactory.getMinecraft(serverLevel);
            ItemStack axe = new ItemStack(Items.IRON_AXE);

            // FIX: Target the TOP FACE of the block, not the center.
            // .relative(Direction.UP, 0.5) moves the click point to the top surface.
            Vec3 hitPos = Vec3.atCenterOf(pos).relative(Direction.UP, 0.5);

            UseOnContext context = new UseOnContext(
                    level,
                    fakePlayer,
                    InteractionHand.MAIN_HAND,
                    axe,
                    new BlockHitResult(hitPos, Direction.UP, pos, false)
            );

            // Ask the block what it turns into
            BlockState stripped = originalLog.getToolModifiedState(context, ItemAbilities.AXE_STRIP, true);

            if (stripped != null) {
                return stripped;
            }
        }
        // Debugging: If you see Oak, it means the code above failed (returned null)
        return Blocks.STRIPPED_OAK_WOOD.defaultBlockState();
    }

    private BlockState getSaplingFromLog(BlockState logState) {
        // 1. Get the ID (e.g., "natures_spirit:redwood_log")
        ResourceLocation logId = BuiltInRegistries.BLOCK.getKey(logState.getBlock());
        String namespace = logId.getNamespace();
        String path = logId.getPath();

        // 2. Try to guess the sapling name
        // We remove "_log" or "_wood" and replace it with "_sapling"
        String saplingPath = path.replace("_log", "_sapling").replace("_wood", "_sapling");

        // Handle cases where the log doesn't have a suffix (rare, but possible)
        if (!saplingPath.endsWith("_sapling")) {
            saplingPath = saplingPath + "_sapling";
        }

        // 3. Check if this guessed block actually exists in the game
        ResourceLocation saplingId = ResourceLocation.fromNamespaceAndPath(namespace, saplingPath);
        Optional<Block> saplingBlock = BuiltInRegistries.BLOCK.getOptional(saplingId);

        // 4. If found, return it!
        if (saplingBlock.isPresent()) {
            return saplingBlock.get().defaultBlockState();
        }

        // 5. Fallback: If guessing failed, return Oak Sapling
        return Blocks.OAK_SAPLING.defaultBlockState();
    }

    private boolean isNaturalTree(Level level, BlockPos pos) {
        BlockPos cursor = pos;
        int height = 0;
        while (level.getBlockState(cursor.above()).is(BlockTags.LOGS) && height < 20) {
            cursor = cursor.above();
            height++;
        }
        BlockState topBlock = level.getBlockState(cursor.above());
        if (topBlock.is(BlockTags.LEAVES)) {
            return !topBlock.getValue(LeavesBlock.PERSISTENT);
        }
        if (topBlock.isAir()) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    BlockState neighbor = level.getBlockState(cursor.offset(x, 0, z));
                    if (neighbor.is(BlockTags.LEAVES) && !neighbor.getValue(LeavesBlock.PERSISTENT)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private BlockPos findStump(Level level, BlockPos pos) {
        BlockPos cursor = pos;
        int safety = 0;
        while (level.getBlockState(cursor.below()).is(BlockTags.LOGS) && safety < 30) {
            cursor = cursor.below();
            safety++;
        }
        return cursor;
    }
}