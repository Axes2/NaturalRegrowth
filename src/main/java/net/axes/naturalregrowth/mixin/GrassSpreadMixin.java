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
        // 1. Original Vanilla Logic (Standard Spread)
        if (targetState.is(lookedForBlock)) {
            return true;
        }

        // 2. Custom Logic: Healing PM Weather Blocks
        if (lookedForBlock == Blocks.DIRT) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(targetState.getBlock());

            // Check namespace to ensure we only affect PM Weather blocks
            if (id.getNamespace().equals("pmweather")) {
                String path = id.getPath();

                boolean isScoured = path.equals("scoured_grass");
                boolean isBurnt = path.contains("charred_dirt") || path.contains("burnt_grass");

                // Config Check: Scoured Grass (Tornadoes)
                if (isScoured && !Config.COMMON.healScouredGrass.get()) {
                    return false;
                }

                // Config Check: Burnt Grass (Wildfires) - Defaults to FALSE
                if (isBurnt && !Config.COMMON.healBurntGrass.get()) {
                    return false;
                }

                // If either is valid, apply the rate limiter
                if (isScoured || isBurnt) {
                    // Shared chance config allows user to control overall healing speed
                    if (Math.random() < Config.COMMON.healScouredGrassChance.get()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}