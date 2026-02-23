package net.axes.naturalregrowth.mixin;

import dev.protomanly.pmweather.data.BlockDataHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockDataHandler.class)
public class BlockDataHandlerMixin {

    /**
     *
     * PMWeather 0.16.4 moved data cleanup to chunks. 
     * This update loop deletes sapling data if the block isn't a Rotted Log.
     * Cancelling this protects Wildfire sapling data and stops native leaf regrowth.
     */
    @Inject(method = "update", at = @At("HEAD"), cancellable = true, remap = false)
    private void onUpdate(Level level, ChunkAccess chunk, CallbackInfo ci) {
        ci.cancel();
    }

    /**
     * GHOST LEAVES:
     * Don't want PMWeather tracking dead leaves for its native regrowth.
     */
    @Inject(method = "setLeafData", at = @At("HEAD"), cancellable = true, remap = false)
    private void onSetLeafData(BlockPos pos, Block leafBlock, CallbackInfo ci) {
        ci.cancel();
    }
}