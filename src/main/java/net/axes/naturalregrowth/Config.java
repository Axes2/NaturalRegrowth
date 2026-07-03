package net.axes.naturalregrowth;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {
    public static final ModConfigSpec SPEC;
    public static final Common COMMON;

    public static class Common {
        // Wind Settings
        public final ModConfigSpec.DoubleValue regrowthChance;
        public final ModConfigSpec.IntValue regrowthDelay;
        public final ModConfigSpec.BooleanValue healScouredGrass;
        public final ModConfigSpec.DoubleValue healingChance;
        public final ModConfigSpec.IntValue windRadius;

        // Wildfire Settings (NEW SECTION)
        public final ModConfigSpec.BooleanValue enableFireRegrowth;
        public final ModConfigSpec.DoubleValue fireRegrowthChance;
        public final ModConfigSpec.IntValue fireRegrowthDelay; // <--- NEW
        public final ModConfigSpec.BooleanValue healBurntGrass;

        // Shared / Performance
        public final ModConfigSpec.DoubleValue healScouredGrassChance; // Shared rate limiter
        public final ModConfigSpec.BooleanValue catchUpGrowth;
        public final ModConfigSpec.DoubleValue floralRegrowthChance; // <--- NEW
        public final ModConfigSpec.BooleanValue instantCatchUp;
        public final ModConfigSpec.BooleanValue dropLogItems;

        public Common(ModConfigSpec.Builder builder) {

            // --- SECTION: WIND REGROWTH ---
            builder.comment("Settings for Tornado and Wind damage regrowth.").push("wind_regrowth");

            regrowthDelay = builder
                    .comment("The minimum time (in ticks) a tornado stump must wait after creation before it can START growing.",
                            "6000 Ticks = 5 Minutes.",
                            "Default: 6000.")
                    .defineInRange("regrowthDelay", 6000, 0, 72000);

            regrowthChance = builder
                    .comment("The chance (0.0 to 1.0) that a tornado stump will turn into a sapling per random tick.",
                            "Default: 0.04 (1.5 Hours to full heal).")
                    .defineInRange("regrowthChance", 0.04, 0.0, 1.0);

            healScouredGrass = builder
                    .comment("If true, Vanilla Grass will naturally spread onto and heal 'Scoured Grass'.",
                            "Default: true")
                    .define("healScouredGrass", true);

            floralRegrowthChance = builder
                    .comment("The chance (0.0 to 1.0) that tall grass or flowers will sprout when a PMWeather dirt scar is healed.",
                            "Default: 0.15 (15% chance). Lower this to reduce flora density.")
                    .defineInRange("floralRegrowthChance", 0.15, 0.0, 1.0);

            healingChance = builder
                    .comment("The chance (0.0 to 1.0) that a wind-damaged log or leaf will heal per random tick.",
                            "Default: 0.2 (20% chance, or 1.5 hours ). Higher = Faster healing.")
                    .defineInRange("healingChance", 0.2, 0.0, 1.0);

            windRadius = builder
                    .comment("The radius (in blocks) around the player where wind damage occurs.",
                            "Higher values create more destructive wind events but check more blocks. May impact performance.",
                            "Default: 64")
                    .defineInRange("windRadius", 64, 16, 256);

            builder.pop(); // Close Wind


            // --- SECTION: WILDFIRE REGROWTH ---
            builder.comment("Settings for Wildfire damage regrowth.").push("wildfire_regrowth");

            enableFireRegrowth = builder
                    .comment("If true, trees burnt by PM Weather wildfires will turn into regrowing stumps instead of charred logs.",
                            "Default: true")
                    .define("enableFireRegrowth", true);

            fireRegrowthDelay = builder
                    .comment("The minimum time (in ticks) a burnt stump must wait before it can START growing.",
                            "This prevents trees from trying to regrow while the fire is still burning.",
                            "24000 Ticks = 20 Minutes (1 Full Minecraft Day).",
                            "Default: 24000.")
                    .defineInRange("fireRegrowthDelay", 24000, 0, 240000);

            fireRegrowthChance = builder
                    .comment("The chance (0.0 to 1.0) that a wildfire stump will turn into a sapling per random tick.",
                            "0.03 = ~2 Hours (Real Time)",
                            "Default: 0.03 (2-3 Hours to full heal).")
                    .defineInRange("fireRegrowthChance", 0.03, 0.0, 1.0);

            healBurntGrass = builder
                    .comment("If true, Vanilla Grass will naturally spread onto and heal 'Charred Dirt' and 'Burnt Grass'.",
                            "This allows wildfire scars to heal over time.",
                            "Default: false")
                    .define("healBurntGrass", true);

            builder.pop(); // Close Wildfire


            // --- SECTION: SHARED & PERFORMANCE ---
            builder.comment("Shared settings and performance tweaks.").push("performance");

            healScouredGrassChance = builder
                    .comment("The probability (0.0 to 1.0) that grass will successfully spread to Scoured OR Charred dirt per tick.",
                            "This controls the healing speed for both tornado and fire scars.",
                            "Default: 0.30 (Slower healing, 2-3 hours for full heal).")
                    .defineInRange("healScouredGrassChance", 0.30, 0.0, 1.0);

            dropLogItems = builder
                    .comment("If true, logs destroyed by the falling tree logic will drop item stacks.",
                            "Default: false")
                    .define("dropLogItems", false);

            catchUpGrowth = builder
                    .comment("If true, stumps in unloaded chunks will simulate growth when the chunk is reloaded.",
                            "Default: true")
                    .define("catchUpGrowth", true);

            instantCatchUp = builder
                    .comment("If true, the 'Catch-Up' mechanic will force the tree to grow INSTANTLY instead of just placing a sapling.",
                            "WARNING: Can cause lag spikes.",
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