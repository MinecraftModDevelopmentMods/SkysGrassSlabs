package zone.moddev.mc.skysgrassslabs.world;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.neoforged.neoforge.common.world.BiomeModifier.Phase;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo.BiomeInfo;
import org.junit.Test;

public class BiomeModifierCompatibilityTest {
    @Test
    public void bothLoaderEntryPointsRemainAvailable() throws Exception {
        Class<?> modifier = Class.forName(SmoothingBiomeModifier.class.getName(), false,
                getClass().getClassLoader());
        assertNotNull(modifier.getMethod("modify", Holder.class, Phase.class,
                BiomeInfo.Builder.class));
        assertNotNull(modifier.getMethod("modify", RegistryAccess.class, Holder.class,
                Phase.class, BiomeInfo.Builder.class));
    }

    @Test
    public void newerEntryPointDelegatesWithoutChangingTheSmoothingRules() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/zone/moddev/mc/skysgrassslabs/world/SmoothingBiomeModifier.java"));
        assertTrue(source.contains("modify(biome, phase, builder);"));
        assertTrue(source.contains("phase != Phase.AFTER_EVERYTHING"));
        assertTrue(source.contains("features.add(0, smoothing)"));
        assertTrue(source.contains("features.stream().noneMatch"));
    }
}
