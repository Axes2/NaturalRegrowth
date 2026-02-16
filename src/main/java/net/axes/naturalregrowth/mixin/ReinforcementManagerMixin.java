package net.axes.naturalregrowth.mixin;

import dev.protomanly.pmweather.data.ReinforcementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ReinforcementManager.class)
public class ReinforcementManagerMixin {

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, remap = false)
    private void onUpdate(CallbackInfo ci) {
        // Stop PM Weather from processing regrowth.
        // Natural Regrowth will handle this via its own TileEntities.
        ci.cancel();
    }
}