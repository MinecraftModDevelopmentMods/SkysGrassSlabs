package zone.moddev.mc.skysgrassslabs.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server-authoritative common configuration for newly generated terrain. */
public final class BetaConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue GENERATE_GRASS_SLABS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("worldgen");
        GENERATE_GRASS_SLABS = builder
                .comment("Place grass slabs on eligible one-block transitions in newly generated Overworld chunks.")
                .define("generateGrassSlabs", true);
        builder.pop();
        SPEC = builder.build();
    }

    private BetaConfig() {
    }
}
