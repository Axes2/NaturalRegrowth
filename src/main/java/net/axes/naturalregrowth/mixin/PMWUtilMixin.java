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
     * PM Weather uses this method to check if a tree should "Rot".
     * Cancel it to prevent Rotted Logs from appearing entirely.
     */

    // Method 1: The recursive check
    @Inject(method = "checkLogs(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void onCheckLogs(BlockState state, ServerLevel level, BlockPos pos, CallbackInfo ci) {
        ci.cancel();
    }

    // Method 2: The detailed check (Actual logic lives here)
    @Inject(method = "checkLogs(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;I)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void onCheckLogsDetailed(BlockState state, ServerLevel level, BlockPos pos, int y, CallbackInfo ci) {
        ci.cancel();
    }
}