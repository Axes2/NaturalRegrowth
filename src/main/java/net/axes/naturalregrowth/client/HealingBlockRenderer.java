package net.axes.naturalregrowth.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.axes.naturalregrowth.block.entity.HealingBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class HealingBlockRenderer implements BlockEntityRenderer<HealingBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public HealingBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(HealingBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState renderState = entity.getRenderState();

        // If it's Air (Leaves), don't render anything -> Invisible!
        if (renderState == null || renderState.isAir()) return;

        poseStack.pushPose();
        // Render the "Stripped" version exactly where the healing block is
        blockRenderer.renderSingleBlock(renderState, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}