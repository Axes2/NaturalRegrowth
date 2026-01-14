package net.axes.naturalregrowth.mixin;

import dev.protomanly.pmweather.weather.Storm;
import net.axes.naturalregrowth.block.RegrowingStumpBlock;
import net.axes.naturalregrowth.compat.NaturalRegrowthCompat;
import net.axes.naturalregrowth.util.TreeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.Map;
import java.util.Optional;

@Mixin(Storm.class)
public class TornadoDestructionMixin {

    // --- DELEGATE TO COMPAT CLASS ---
    @Redirect(method = "doDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    public boolean onStormRemoveBlock(Level level, BlockPos pos, boolean isMoving) {
        // API method
        return NaturalRegrowthCompat.removeBlockWithRegrowth(level, pos, isMoving);
    }

    // --- MIXIN-SPECIFIC LOGIC ---

    @Redirect(method = "doDamage", at = @At(value = "INVOKE", target = "Ljava/util/Map;getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    public Object onGetStrippedVariant(Map<Block, Block> map, Object key, Object defaultValue) {
        Object result = map.get(key);
        if (result != null) return result;

        if (key instanceof Block logBlock) {
            // Using TreeUtils now to keep things consistent
            Block guessed = tryGuessStrippedLog(logBlock);
            if (guessed != null) return guessed;
        }
        return defaultValue;
    }

    @Redirect(method = "doDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    public boolean onStormUpdateBlock(Level level, BlockPos pos, BlockState newState) {
        if (level.getBlockState(pos).getBlock() instanceof RegrowingStumpBlock) {
            return false;
        }
        return level.setBlockAndUpdate(pos, newState);
    }

    // Helper for the 'onGetStrippedVariant' redirect above
    private Block tryGuessStrippedLog(Block logBlock) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(logBlock);
        String namespace = id.getNamespace();
        String path = id.getPath();
        if (path.contains("stripped")) return logBlock;

        String guess1 = "stripped_" + path;
        Optional<Block> result1 = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(namespace, guess1));
        if (result1.isPresent()) return result1.get();

        if (path.contains("wood")) {
            String guess2 = "stripped_" + path;
            Optional<Block> result2 = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(namespace, guess2));
            if (result2.isPresent()) return result2.get();
        }
        Optional<Block> result3 = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("minecraft", guess1));
        return result3.orElse(null);
    }
}