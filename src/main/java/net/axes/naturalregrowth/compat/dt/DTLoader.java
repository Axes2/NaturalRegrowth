package net.axes.naturalregrowth.compat.dt;

import net.neoforged.fml.ModList;
import net.neoforged.bus.api.IEventBus;

public class DTLoader {
    public static final String DT_MOD_ID = "dynamictrees";

    public static void init(IEventBus bus) {
        if (ModList.get().isLoaded(DT_MOD_ID)) {
            DTRegistries.register(bus);
        }
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(DT_MOD_ID);
    }
}