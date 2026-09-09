package zone.moddev.mc.skysgrassslabs.world;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo.BiomeInfo;

/** Adds the smoother before vegetation in every non-Nether, non-End biome. */
public final class SmoothingBiomeModifier implements BiomeModifier {
    public static final SmoothingBiomeModifier INSTANCE = new SmoothingBiomeModifier();
    public static final MapCodec<SmoothingBiomeModifier> CODEC = MapCodec.unit(INSTANCE);

    private SmoothingBiomeModifier() {
    }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, BiomeInfo.Builder builder) {
        Holder<PlacedFeature> smoothing = WorldgenBootstrap.placedFeature();
        if (phase != Phase.AFTER_EVERYTHING || smoothing == null
                || biome.is(BiomeTags.IS_NETHER) || biome.is(BiomeTags.IS_END)) {
            return;
        }
        BiomeGenerationSettings.PlainBuilder generation = builder.getGenerationSettings();
        List<Holder<PlacedFeature>> features = generation.getFeatures(
                GenerationStep.Decoration.VEGETAL_DECORATION);
        if (features.stream().noneMatch(existing -> existing.value() == smoothing.value())) {
            features.add(0, smoothing);
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
