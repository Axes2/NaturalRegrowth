package net.axes.naturalregrowth.mixin;

import net.axes.naturalregrowth.Config;
import net.axes.naturalregrowth.block.entity.RegrowingStumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpreadingSnowyDirtBlock.class)
public class GrassSpreadMixin {

    //Allows Grass to spread onto PMWeather soil
    @Redirect(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private boolean onCheckIsDirt(BlockState targetState, Block lookedForBlock) {
        if (targetState.is(lookedForBlock)) {
            return true;
        }

        if (lookedForBlock == Blocks.DIRT) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(targetState.getBlock());

            if (id.getNamespace().equals("pmweather")) {
                String path = id.getPath();
                boolean isScoured = path.equals("scoured_grass");
                boolean isBurnt = path.contains("charred_dirt") || path.contains("burnt_grass");

                if (isScoured && !Config.COMMON.healScouredGrass.get()) return false;
                if (isBurnt && !Config.COMMON.healBurntGrass.get()) return false;

                if (isScoured || isBurnt) {
                    return Math.random() < Config.COMMON.healScouredGrassChance.get();
                }
            }
        }

        return false;
    }

    // 2. 4.1.0 Logic: Intercept placement to calculate floral regrowth
    @Redirect(
            method = "randomTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z")
    )
    private boolean onGrassSpreadPlacement(ServerLevel level, BlockPos pos, BlockState newGrassState) {
        BlockState oldState = level.getBlockState(pos);
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(oldState.getBlock());

        // A. PMWeather Scoured/Charred Soil Check
        boolean wasPMWSoil = id.getNamespace().equals("pmweather") &&
                (id.getPath().contains("scour") || id.getPath().contains("charred"));

        // B. Weak Tornado Check (Vanilla Dirt near a Stump)
        boolean isStormDirt = false;
        if (oldState.is(Blocks.DIRT)) {
            int searchRadius = 12;
            int radiusSqr = searchRadius * searchRadius;

            int minChunkX = (pos.getX() - searchRadius) >> 4;
            int maxChunkX = (pos.getX() + searchRadius) >> 4;
            int minChunkZ = (pos.getZ() - searchRadius) >> 4;
            int maxChunkZ = (pos.getZ() + searchRadius) >> 4;

            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                    if (level.hasChunk(cx, cz)) {
                        LevelChunk chunk = level.getChunk(cx, cz);
                        for (BlockEntity be : chunk.getBlockEntities().values()) {
                            if (be instanceof RegrowingStumpBlockEntity) {
                                if (be.getBlockPos().distSqr(pos) <= radiusSqr) {
                                    isStormDirt = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (isStormDirt) break;
            }
        }

        boolean isValidScar = wasPMWSoil || isStormDirt;
        boolean placedSuccessfully = level.setBlockAndUpdate(pos, newGrassState);

        // C. Floral Regrowth Logic
        if (placedSuccessfully && isValidScar && newGrassState.getBlock() instanceof BonemealableBlock growable) {

            if (Math.random() < Config.COMMON.floralRegrowthChance.get()) {

                int radius = 3;
                int grassCount = 0;
                int foliageCount = 0;

                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos scanPos = pos.offset(x, 0, z);

                        if (level.getBlockState(scanPos).is(Blocks.GRASS_BLOCK)) {
                            grassCount++;
                            BlockState aboveState = level.getBlockState(scanPos.above());
                            if (aboveState.is(BlockTags.FLOWERS) || aboveState.is(Blocks.SHORT_GRASS) || aboveState.is(Blocks.TALL_GRASS)) {
                                foliageCount++;
                            }
                        }
                    }
                }

                if (grassCount >= 25 && foliageCount <= 5) {
                    growable.performBonemeal(level, level.random, pos, newGrassState);
                }
            }
        }

        return placedSuccessfully;
    }
}