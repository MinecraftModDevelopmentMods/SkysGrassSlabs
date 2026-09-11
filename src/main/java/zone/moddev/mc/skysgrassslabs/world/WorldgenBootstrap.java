package zone.moddev.mc.skysgrassslabs.world;

import com.mojang.serialization.MapCodec;
import java.util.Collections;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;

/** Registers the smoothing feature and its data-driven biome modifier. */
public final class WorldgenBootstrap {
    public static final String FEATURE_NAME = "grass_slab_smoothing";

    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(BuiltInRegistries.FEATURE, SkysGrassSlabs.MOD_ID);
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS,
                    SkysGrassSlabs.MOD_ID);
    private static final DeferredHolder<Feature<?>, GrassSlabSmoothingFeature> SMOOTHING =
            FEATURES.register(FEATURE_NAME,
                    () -> new GrassSlabSmoothingFeature(NoneFeatureConfiguration.CODEC));

    private static volatile Holder<PlacedFeature> placedFeature;

    static {
        BIOME_MODIFIERS.register(FEATURE_NAME, () -> SmoothingBiomeModifier.CODEC);
    }

    private WorldgenBootstrap() {
    }

    public static void register(IEventBus modBus) {
        FEATURES.register(modBus);
        BIOME_MODIFIERS.register(modBus);
        modBus.addListener(WorldgenBootstrap::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(WorldgenBootstrap::placedFeature);
    }

    static synchronized Holder<PlacedFeature> placedFeature() {
        if (placedFeature == null) {
            Holder<ConfiguredFeature<?, ?>> configured = Holder.direct(
                    new ConfiguredFeature<NoneFeatureConfiguration, GrassSlabSmoothingFeature>(
                            SMOOTHING.get(), NoneFeatureConfiguration.INSTANCE));
            placedFeature = Holder.direct(new PlacedFeature(configured, Collections.emptyList()));
        }
        return placedFeature;
    }
}
