package net.axes.naturalregrowth;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
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

        LOGGER.info("Natural Regrowth loaded successfully.");
    }
}