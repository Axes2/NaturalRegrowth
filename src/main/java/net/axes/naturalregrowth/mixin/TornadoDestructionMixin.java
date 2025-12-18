package net.axes.naturalregrowth.mixin;

import dev.protomanly.pmweather.weather.Storm;
import net.axes.naturalregrowth.Config;
import net.axes.naturalregrowth.ModBlocks;
import net.axes.naturalregrowth.block.RegrowingStumpBlock;
import net.axes.naturalregrowth.block.entity.RegrowingStumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries; // Needed for name guessing
import net.minecraft.resources.ResourceLocation;        // Needed for name guessing
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.LinkedList;
import java.util.Queue;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.Optional;

@Mixin(Storm.class)
public class TornadoDestructionMixin {

    // --- EXISTING MIXIN: Protect Stumps & Add Logic ---
    @Redirect(method = "doDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    public boolean onStormRemoveBlock(Level level, BlockPos pos, boolean isMoving) {
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof RegrowingStumpBlock) return false;

        if (state.is(BlockTags.LOGS)) {
            if (isNaturalTree(level, pos)) {
                BlockPos stumpPos = findStump(level, pos);
                if (level.getBlockState(stumpPos).getBlock() instanceof RegrowingStumpBlock) {
                    return level.removeBlock(pos, isMoving);
                }
                infectTreeBase(level, stumpPos, state);
                return true;
            }
        }

        // Leaf logic...
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

    // --- NEW MIXIN: Fix Stripping Logic (Issue #1) ---
    // This intercepts modded trees being converted to Oak
    @Redirect(method = "doDamage", at = @At(value = "INVOKE", target = "Ljava/util/Map;getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    public Object onGetStrippedVariant(Map<Block, Block> map, Object key, Object defaultValue) {
        // 1. Try the mod's original map first
        Object result = map.get(key);
        if (result != null) {
            return result;
        }

        // 2. If the mod failed (returned null/default), try Smart Guessing
        if (key instanceof Block logBlock) {
            Block guessed = tryGuessStrippedLog(logBlock);
            if (guessed != null) {
                return guessed;
            }
        }

        // 3. Fallback to the original default (Stripped Oak)
        return defaultValue;
    }


    // --- NEW SHIELD: Protect Stumps from being "Stripped" or Replaced ---
    @Redirect(method = "doDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    public boolean onStormUpdateBlock(Level level, BlockPos pos, BlockState newState) {

        // No stump overwrite
        if (level.getBlockState(pos).getBlock() instanceof RegrowingStumpBlock) {
            return false;
        }

        // Otherwise,storm do what it wants
        return level.setBlockAndUpdate(pos, newState);
    }
    // --- HELPER METHODS ---

    private Block tryGuessStrippedLog(Block logBlock) {
        // Logic: "natures_spirit:redwood_log" -> "natures_spirit:stripped_redwood_log"
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(logBlock);
        String namespace = id.getNamespace();
        String path = id.getPath();

        // Standard convention: Prepend "stripped_"
        String strippedPath = "stripped_" + path;

        ResourceLocation guessedId = ResourceLocation.fromNamespaceAndPath(namespace, strippedPath);
        Optional<Block> strippedBlock = BuiltInRegistries.BLOCK.getOptional(guessedId);

        return strippedBlock.orElse(null);
    }

    private void infectTreeBase(Level level, BlockPos stumpPos, BlockState originalLog) {
        if (level.getBlockState(stumpPos.below()).is(BlockTags.DIRT)) {
            BlockState disguise = getStrippedLog(level, stumpPos, originalLog);
            BlockState sapling = getSaplingFromLog(originalLog);

            // 1. Place Stump
            level.setBlock(stumpPos, ModBlocks.REGROWING_STUMP.get().defaultBlockState(), 3);

            // 2. Configure Brain
            BlockEntity be = level.getBlockEntity(stumpPos);
            if (be instanceof RegrowingStumpBlockEntity stump) {
                stump.setMimic(disguise, sapling);
                stump.setCreationTime(level.getGameTime());
            }


        }
    }

    private BlockState getSaplingFromLog(BlockState logState) {
        ResourceLocation logId = BuiltInRegistries.BLOCK.getKey(logState.getBlock());
        String namespace = logId.getNamespace();
        String path = logId.getPath();

        // FIX 2: Clean the name. If it is "stripped_larch_log", remove "stripped_" first.
        path = path.replace("stripped_", "");

        // Now guess the sapling
        String saplingPath = path.replace("_log", "_sapling").replace("_wood", "_sapling");

        if (!saplingPath.endsWith("_sapling")) {
            saplingPath = saplingPath + "_sapling";
        }

        ResourceLocation saplingId = ResourceLocation.fromNamespaceAndPath(namespace, saplingPath);
        return BuiltInRegistries.BLOCK.getOptional(saplingId)
                .map(Block::defaultBlockState)
                .orElse(Blocks.OAK_SAPLING.defaultBlockState());
    }


    private BlockState getStrippedLog(Level level, BlockPos pos, BlockState originalLog) {
        // DEBUG
        String logName = BuiltInRegistries.BLOCK.getKey(originalLog.getBlock()).toString();
        // System.out.println("[NATURAL REGROWTH DEBUG] Processing Log: " + logName);

        // Return if already stripped
        if (logName.contains("stripped")) {
            // System.out.println(" -> Already stripped. Using as-is.");
            return originalLog;
        }

        // (Simulate Axe)
        if (level instanceof ServerLevel serverLevel) {
            var fakePlayer = FakePlayerFactory.getMinecraft(serverLevel);
            ItemStack axe = new ItemStack(Items.IRON_AXE);

            // Aim at the center to avoid snow/vines
            Vec3 hitPos = Vec3.atCenterOf(pos);

            UseOnContext context = new UseOnContext(
                    level,
                    fakePlayer,
                    InteractionHand.MAIN_HAND,
                    axe,
                    new BlockHitResult(hitPos, Direction.UP, pos, false)
            );

            BlockState stripped = originalLog.getToolModifiedState(context, ItemAbilities.AXE_STRIP, true);
            if (stripped != null) {
                return stripped;
            }
        }

        // 3. Try "Smart Guess" Way
        Block guessedBlock = tryGuessStrippedLog(originalLog.getBlock());
        if (guessedBlock != null) {
            return guessedBlock.defaultBlockState();
        }

        // 4. Fail
        // System.out.println(" -> FALLBACK TRIGGERED: Returning Stripped Oak");
        return Blocks.STRIPPED_OAK_WOOD.defaultBlockState();
    }

    private boolean isNaturalTree(Level level, BlockPos pos) {
        BlockPos cursor = pos;
        int height = 0;
        while (level.getBlockState(cursor.above()).is(BlockTags.LOGS) && height < 20) {
            cursor = cursor.above();
            height++;
        }
        BlockState topBlock = level.getBlockState(cursor.above());
        if (topBlock.is(BlockTags.LEAVES)) return !topBlock.getValue(LeavesBlock.PERSISTENT);
        if (topBlock.isAir()) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    BlockState neighbor = level.getBlockState(cursor.offset(x, 0, z));
                    if (neighbor.is(BlockTags.LEAVES) && !neighbor.getValue(LeavesBlock.PERSISTENT)) return true;
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