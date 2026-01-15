package net.axes.naturalregrowth.mixin;

import net.axes.naturalregrowth.Config;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpreadingSnowyDirtBlock.class)
public class GrassSpreadMixin {

    @Redirect(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private boolean onCheckIsDirt(BlockState targetState, Block lookedForBlock) {
        // 1. Original Vanilla Logic
        if (targetState.is(lookedForBlock)) {
            return true;
        }

        // 2. Custom Logic: Scoured Grass Healing
        if (lookedForBlock == Blocks.DIRT && Config.COMMON.healScouredGrass.get()) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(targetState.getBlock());

            if (id.toString().equals("pmweather:scoured_grass")) {
               //Rate Limiter
                if (Math.random() < Config.COMMON.healScouredGrassChance.get()) {
                    return true;
                }
            }
        }

        return false;
    }
}