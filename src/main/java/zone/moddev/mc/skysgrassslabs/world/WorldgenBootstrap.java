package zone.moddev.mc.skysgrassslabs.world;

import java.util.Collections;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;

/** Registers and installs the smoothing feature at the first vegetation slot. */
public final class WorldgenBootstrap {
    public static final String FEATURE_NAME = "grass_slab_smoothing";
    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, SkysGrassSlabs.MOD_ID);
    private static final RegistryObject<GrassSlabSmoothingFeature> SMOOTHING =
            FEATURES.register(FEATURE_NAME,
                    () -> new GrassSlabSmoothingFeature(NoneFeatureConfiguration.CODEC));
    private static Holder<PlacedFeature> placedFeature;

    private WorldgenBootstrap() {
    }

    public static void register(IEventBus modBus) {
        FEATURES.register(modBus);
        modBus.addListener(WorldgenBootstrap::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ResourceLocation id = new ResourceLocation(SkysGrassSlabs.MOD_ID, FEATURE_NAME);
            Holder<ConfiguredFeature<?, ?>> configured = BuiltinRegistries.register(
                    BuiltinRegistries.CONFIGURED_FEATURE, id,
                    new ConfiguredFeature<>(SMOOTHING.get(), NoneFeatureConfiguration.INSTANCE));
            placedFeature = BuiltinRegistries.register(BuiltinRegistries.PLACED_FEATURE, id,
                    new PlacedFeature(configured, Collections.emptyList()));
        });
    }

    public static void onBiomeLoading(BiomeLoadingEvent event) {
        if (placedFeature == null) {
            return;
        }
        List<Holder<PlacedFeature>> features = event.getGeneration()
                .getFeatures(GenerationStep.Decoration.VEGETAL_DECORATION);
        if (features.stream().noneMatch(existing -> existing.value() == placedFeature.value())) {
            features.add(0, placedFeature);
        }
    }
}
