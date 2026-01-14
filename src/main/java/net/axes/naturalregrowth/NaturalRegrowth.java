package net.axes.naturalregrowth;

import com.mojang.logging.LogUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(NaturalRegrowth.MODID)
public class NaturalRegrowth {
    public static final String MODID = "naturalregrowth";
    private static final Logger LOGGER = LogUtils.getLogger();

    // Updated constructor to accept ModContainer
    public NaturalRegrowth(IEventBus modEventBus, ModContainer modContainer) {

        ModBlocks.register(modEventBus);

        // Register the Config
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(this::commonSetup);

        LOGGER.info("Natural Regrowth loaded successfully.");
    }
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Register Flammability (Catch Chance, Burn Speed)
            // 5, 5 = Like Oak Logs
            // 30, 60 = Like Leaves

            FireBlock fireBlock = (FireBlock) Blocks.FIRE;

            fireBlock.setFlammable(ModBlocks.HEALING_LOG.get(), 5, 5);
            fireBlock.setFlammable(ModBlocks.HEALING_LEAF.get(), 30, 60);
            fireBlock.setFlammable(ModBlocks.REGROWING_STUMP.get(), 5, 5);
        });
    }
}