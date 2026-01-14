package net.axes.naturalregrowth.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.Optional;

public class TreeUtils {

    public static BlockState getStrippedLog(Level level, BlockPos pos, BlockState originalLog) {
        String logName = BuiltInRegistries.BLOCK.getKey(originalLog.getBlock()).toString();
        // If it's already stripped, keep it
        if (logName.contains("stripped")) return originalLog;

        // 1. Try Vanilla "Axe Strip" Map
        if (level instanceof ServerLevel serverLevel) {
            var fakePlayer = FakePlayerFactory.getMinecraft(serverLevel);
            ItemStack axe = new ItemStack(Items.IRON_AXE);
            Vec3 hitPos = Vec3.atCenterOf(pos);
            UseOnContext context = new UseOnContext(level, fakePlayer, InteractionHand.MAIN_HAND, axe, new BlockHitResult(hitPos, Direction.UP, pos, false));
            BlockState stripped = originalLog.getToolModifiedState(context, ItemAbilities.AXE_STRIP, true);
            if (stripped != null) return stripped;
        }

        // 2. Fallback: Name Guessing (e.g. "my_mod:funny_log" -> "my_mod:stripped_funny_log")
        Block guessedBlock = tryGuessStrippedLog(originalLog.getBlock());
        return guessedBlock != null ? guessedBlock.defaultBlockState() : Blocks.STRIPPED_OAK_LOG.defaultBlockState();
    }

    private static Block tryGuessStrippedLog(Block logBlock) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(logBlock);
        String namespace = id.getNamespace();
        String path = id.getPath();

        String guess1 = "stripped_" + path;
        Optional<Block> result1 = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(namespace, guess1));
        if (result1.isPresent()) return result1.get();

        if (path.contains("wood")) {
            String guess2 = "stripped_" + path;
            Optional<Block> result2 = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(namespace, guess2));
            if (result2.isPresent()) return result2.get();
        }

        return null;
    }
}