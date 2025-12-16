package net.axes.naturalregrowth;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {
    public static final ModConfigSpec SPEC;
    public static final Common COMMON;

    public static class Common {
        public final ModConfigSpec.DoubleValue regrowthChance;
        public final ModConfigSpec.BooleanValue dropLogItems;

        public Common(ModConfigSpec.Builder builder) {
            builder.push("general");

            regrowthChance = builder
                    .comment("The chance (0.0 to 1.0) that a stump will regrow into a sapling per random tick.",
                            "Higher numbers = Faster regrowth. Default: 0.03 (3%). Average 38 minutes for a tree to grow back")
                    .defineInRange("regrowthChance", 0.03, 0.0, 1.0);

            dropLogItems = builder
                    .comment("If true, logs destroyed by the storm or falling trees will drop wood items.",
                            "WARNING: Setting this to true may cause significant lag during large storms.",
                            "Default: false")
                    .define("dropLogItems", false);

            builder.pop();
        }
    }

    static {
        Pair<Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Common::new);
        SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }
}