package net.axes.naturalregrowth.mixin;

import dev.protomanly.pmweather.block.CharredLogBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CharredLogBlock.class)
public class CharredLogMixin {

    /**
     * PM Weather's CharredLogBlock has a hardcoded 1/40 chance per tick
     * to turn into a Rotted Log.
     * * cancel this method entirely to preserve the Charred Log.
     * This allows Natural Regrowth to handle the "healing" later.
     */
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void onRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        ci.cancel();
    }
}