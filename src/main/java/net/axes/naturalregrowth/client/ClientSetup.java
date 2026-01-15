package net.axes.naturalregrowth.client;

import net.axes.naturalregrowth.ModBlocks;
import net.axes.naturalregrowth.NaturalRegrowth;
import net.axes.naturalregrowth.block.entity.RegrowingStumpBlockEntity; // Import your Block Entity
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = NaturalRegrowth.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // This tells the game: "When you see a REGROWING_STUMP_BE, use the RegrowingStumpRenderer to draw it."
        event.registerBlockEntityRenderer(ModBlocks.REGROWING_STUMP_BE.get(), RegrowingStumpRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.HEALING_BE.get(), HealingBlockRenderer::new);
    }
}