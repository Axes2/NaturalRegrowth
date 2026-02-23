package net.axes.naturalregrowth.mixin;

import dev.protomanly.pmweather.data.ReinforcementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ReinforcementManager.class)
public class ReinforcementManagerMixin {


    /* This class has been depreciated since 0.16.4. In 0.16.4, PMWeather shifted to a Chunk Attachment system.
    Now, the data is handed off to a new class called BlockDataHandler. This class is currently being saved for compatiblity with older versions.
     */





    // This prevents PMWeather from:
    // a) Regrowing trees on its own
    // b) DELETING saved data (because it cleans up data for blocks that aren't Rotted Logs)
    @Inject(method = "update", at = @At("HEAD"), cancellable = true, remap = false)
    private void onUpdate(CallbackInfo ci) {
        ci.cancel();
    }

    // Block Leaf Data
    // We don't want ghost leaves appearing.
    @Inject(method = "setLeafData", at = @At("HEAD"), cancellable = true, remap = false)
    private void onSetLeafData(BlockPos pos, Block block, CallbackInfo ci) {
        ci.cancel();
    }

}