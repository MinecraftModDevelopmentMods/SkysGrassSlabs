package zone.moddev.mc.skysgrassslabs.world;

import com.mojang.serialization.Codec;
import java.util.Collections;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;

/** Registers the smoothing feature and its data-driven biome modifier. */
public final class WorldgenBootstrap {
    public static final String FEATURE_NAME = "grass_slab_smoothing";

    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, SkysGrassSlabs.MOD_ID);
    private static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
                    SkysGrassSlabs.MOD_ID);
    private static final RegistryObject<GrassSlabSmoothingFeature> SMOOTHING =
            FEATURES.register(FEATURE_NAME,
                    () -> new GrassSlabSmoothingFeature(NoneFeatureConfiguration.CODEC));

    private static Holder<PlacedFeature> placedFeature;

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
        event.enqueueWork(() -> {
            Holder<ConfiguredFeature<?, ?>> configured = Holder.direct(
                    new ConfiguredFeature<NoneFeatureConfiguration, GrassSlabSmoothingFeature>(
                            SMOOTHING.get(), NoneFeatureConfiguration.INSTANCE));
            placedFeature = Holder.direct(new PlacedFeature(configured, Collections.emptyList()));
        });
    }

    static Holder<PlacedFeature> placedFeature() {
        return placedFeature;
    }
}
