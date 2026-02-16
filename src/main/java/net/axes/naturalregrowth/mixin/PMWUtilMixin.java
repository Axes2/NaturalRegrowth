package net.axes.naturalregrowth.mixin;

import dev.protomanly.pmweather.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Util.class)
public class PMWUtilMixin {

    /**
     * PM Weather uses this method to check if a tree should "Rot" (turn into Rotted Logs).
     * This method performs a heavy recursive scan (causing lag) and converts logs into
     * blocks that compete with Natural Regrowth's stumps.
     *
     * By cancelling it, we:
     * 1. Fix the lag spike during storms.
     * 2. Prevent Rotted Logs from appearing.
     * 3. Ensure Natural Regrowth controls 100% of the tree destruction/regrowth lifecycle.
     */
    @Inject(method = "checkLogs(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onCheckLogs(BlockState state, ServerLevel level, BlockPos pos, CallbackInfo ci) {
        ci.cancel();
    }

    // Also cancel the overloaded version to be safe
    @Inject(method = "checkLogs(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;I)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onCheckLogsDetailed(BlockState state, ServerLevel level, BlockPos pos, int y, CallbackInfo ci) {
        ci.cancel();
    }
}