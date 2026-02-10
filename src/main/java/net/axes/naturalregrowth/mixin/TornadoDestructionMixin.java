package net.axes.naturalregrowth.mixin;

import dev.protomanly.pmweather.weather.Storm;
import net.axes.naturalregrowth.block.RegrowingStumpBlock;
import net.axes.naturalregrowth.compat.NaturalRegrowthCompat;
import net.axes.naturalregrowth.compat.dt.DTLoader;
import net.axes.naturalregrowth.compat.dt.DTIntegration;
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

    @Redirect(method = "doDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    public boolean onStormRemoveBlock(Level level, BlockPos pos, boolean isMoving) {
        // 1. DYNAMIC TREES CHECK
        if (DTLoader.isLoaded()) {
            BlockState state = level.getBlockState(pos);
            if (DTIntegration.handleStormDamage(level, pos, state)) {
                return true;
            }
        }

        // 2. VANILLA REGROWTH CHECK
        return NaturalRegrowthCompat.removeBlockWithRegrowth(level, pos, isMoving);
    }

    @Redirect(method = "doDamage", at = @At(value = "INVOKE", target = "Ljava/util/Map;getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    public Object onGetStrippedVariant(Map<Block, Block> map, Object key, Object defaultValue) {
        Object result = map.get(key);
        if (result != null) return result;

        if (key instanceof Block logBlock) {
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