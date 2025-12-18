package net.axes.naturalregrowth;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {
    public static final ModConfigSpec SPEC;
    public static final Common COMMON;

    public static class Common {
        // --- 1. Define the Variables ---
        public final ModConfigSpec.DoubleValue regrowthChance;
        public final ModConfigSpec.IntValue regrowthDelay;
        public final ModConfigSpec.BooleanValue dropLogItems;

        public Common(ModConfigSpec.Builder builder) {

            // --- SECTION: REGROWTH SETTINGS ---
            builder.comment("Settings controlling how fast and how often trees grow back.").push("regrowth");

            regrowthDelay = builder
                    .comment("The minimum time (in ticks) a stump must wait after creation before it can START trying to grow.",
                            "This prevents trees from popping back up instantly while a tornado is still nearby.",
                            "20 Ticks = 1 Second.",
                            "1200 Ticks = 1 Minute.",
                            "6000 Ticks = 5 Minutes.",
                            "Default: 1200 (1 minute).")
                    .defineInRange("regrowthDelay", 1200, 0, 72000); // Max is 1 hour (72000 ticks)

            regrowthChance = builder
                    .comment("The chance (0.0 to 1.0) that a stump will turn into a sapling per random tick.",
                            "This check only runs AFTER the 'regrowthDelay' time has passed.",
                            "Higher numbers = Faster regrowth.",
                            "Default: 0.03 (3% chance per tick) ~38 minutes on average.")
                    .defineInRange("regrowthChance", 0.03, 0.0, 1.0);

            builder.pop(); // Close the "regrowth" section


            // --- SECTION: PERFORMANCE & DROPS ---
            builder.comment("Settings related to items, drops, and server performance.").push("performance");

            dropLogItems = builder
                    .comment("If true, logs destroyed by the falling tree logic will drop item stacks.",
                            "WARNING: Setting this to TRUE may cause significant lag during large storms due to hundreds of items spawning at once.",
                            "Default: false (Items are deleted to save FPS).")
                    .define("dropLogItems", false);

            builder.pop(); // Close the "performance" section
        }
    }

    // --- Boilerplate to build the config ---
    static {
        Pair<Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Common::new);
        SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }
}