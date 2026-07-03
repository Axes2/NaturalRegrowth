package net.axes.naturalregrowth.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.axes.naturalregrowth.block.entity.RegrowingStumpBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

public class RegrowingStumpRenderer implements BlockEntityRenderer<RegrowingStumpBlockEntity> {

    public RegrowingStumpRenderer(BlockEntityRendererProvider.Context context) {
        // Constructor needed
    }

    @Override
    public void render(RegrowingStumpBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // 1. Ask the Brain: "What do you look like?"
        BlockState mimicState = entity.getMimicState();

        // 2. Prepare the Matrix
        poseStack.pushPose();

        // 3. Render the Block
        // Use the Minecraft BlockDispatcher to render the "Mimic" state exactly as if it were placed in the world.
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                mimicState,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }

    // --- ADDED TO PREVENT DISTANCE CULLING ---
    @Override
    public int getViewDistance() {
        return 512; // The default is 64. 512 supports 32 chunk render distances.
    }
}