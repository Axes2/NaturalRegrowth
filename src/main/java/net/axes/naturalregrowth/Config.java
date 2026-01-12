package net.axes.naturalregrowth;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {
    public static final ModConfigSpec SPEC;
    public static final Common COMMON;

    public static class Common {
        public final ModConfigSpec.DoubleValue regrowthChance;
        public final ModConfigSpec.IntValue regrowthDelay;
        public final ModConfigSpec.BooleanValue catchUpGrowth;
        public final ModConfigSpec.BooleanValue instantCatchUp;
        public final ModConfigSpec.BooleanValue dropLogItems;

        public Common(ModConfigSpec.Builder builder) {

            // --- SECTION: REGROWTH SETTINGS ---
            builder.comment("Settings controlling how fast and how often trees grow back.").push("regrowth");

            regrowthDelay = builder
                    .comment("The minimum time (in ticks) a stump must wait after creation before it can START trying to grow.",
                            "6000 Ticks = 5 Minutes.",
                            "Default: 6000.")
                    .defineInRange("regrowthDelay", 6000, 0, 72000);

            regrowthChance = builder
                    .comment("The chance (0.0 to 1.0) that a stump will turn into a sapling per random tick.",
                            "Default: 0.03 (3%).")
                    .defineInRange("regrowthChance", 0.03, 0.0, 1.0);

            builder.pop(); // Close Regrowth


            // --- SECTION: PERFORMANCE & DROPS ---
            builder.comment("Settings related to items, drops, and server performance.").push("performance");

            dropLogItems = builder
                    .comment("If true, logs destroyed by the falling tree logic will drop item stacks.",
                            "Default: false")
                    .define("dropLogItems", false);

            catchUpGrowth = builder
                    .comment("If true, stumps in unloaded chunks will simulate growth when the chunk is reloaded.",
                            "If successful, they will turn into saplings immediately.",
                            "Default: true")
                    .define("catchUpGrowth", true);

            instantCatchUp = builder
                    .comment("If true, the 'Catch-Up' mechanic will force the tree to grow INSTANTLY instead of just placing a sapling.",
                            "WARNING: This can cause lag spikes if many trees grow at once (e.g., teleporting back to a destroyed forest).",
                            "Only enable this if you want immediate results and have a strong server/PC.",
                            "Default: false")
                    .define("instantCatchUp", false);

            builder.pop(); // Close Performance
        }
    }

    static {
        Pair<Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Common::new);
        SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }
}