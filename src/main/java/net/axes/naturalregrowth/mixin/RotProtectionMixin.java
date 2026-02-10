package net.axes.naturalregrowth.mixin;

import com.dtteam.dynamictrees.block.branch.BranchBlock;
import net.axes.naturalregrowth.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BranchBlock.class)
public class RotProtectionMixin {

    // We target "rot", which is the void method that actually breaks the block.
    // Unlike "checkForRot", this method is NOT abstract, so we can inject into it safely.
    @Inject(method = "rot", at = @At("HEAD"), cancellable = true, remap = false)
    public void onRot(LevelAccessor level, BlockPos pos, CallbackInfo ci) {

        // --- LIFE SUPPORT LOGIC ---
        // Scan neighbors for Natural Regrowth's Healing Leaves.
        // If found, we prevent the branch from breaking itself.

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);

            if (neighborState.is(ModBlocks.HEALING_LEAF.get())) {
                // Found a healing leaf attached to this branch!
                // Cancel the rot action. The branch stays alive.
                ci.cancel();
                return;
            }
        }
    }
}