package zone.moddev.mc.skysgrassslabs;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi;

import static org.junit.jupiter.api.Assertions.*;

class GrassSlabsLifecycleTest {
    @Test
    void anOwnerCanReadItsConfigurationLaterAndRegistrationClosesBeforeWorldsLoad() throws Exception {
        MinecraftTestBootstrap.registerVanilla();
        Field owner = field("smoothingOwner");
        Field enabled = field("ownerEnabled");
        Field configured = field("configurationLoaded");
        Field frozen = field("frozen");
        Object oldOwner = owner.get(null);
        Object oldEnabled = enabled.get(null);
        Object oldConfigured = configured.get(null);
        Object oldFrozen = frozen.get(null);

        try {
            AtomicBoolean setting = new AtomicBoolean(false);
            GrassSlabsApi.claimSmoothing("testterrain", setting::get);
            assertTrue(GrassSlabsApi.hasSmoothingOwner());
            assertFalse(GrassSlabsApi.ownerAllowsGeneration());
            setting.set(true);
            assertTrue(GrassSlabsApi.ownerAllowsGeneration());
            assertThrows(IllegalStateException.class,
                    () -> GrassSlabsApi.claimSmoothing("secondterrain", true));

            GrassSlabsApi.configurationLoaded();
            assertThrows(IllegalStateException.class,
                    () -> GrassSlabsApi.claimSmoothing("lateterrain", true));
            GrassSlabsApi.freeze();
            assertThrows(IllegalStateException.class, () -> GrassSlabsApi.registerGrassForm(
                    Blocks.DIRT.defaultBlockState(), Blocks.GRASS_BLOCK.defaultBlockState()));
            assertThrows(IllegalStateException.class, () -> GrassSlabsApi.registerSpreadRule(
                    "latecontent", (source, dirt) -> null));
        } finally {
            owner.set(null, oldOwner);
            enabled.set(null, oldEnabled);
            configured.set(null, oldConfigured);
            frozen.set(null, oldFrozen);
        }
    }

    private static Field field(String name) throws ReflectiveOperationException {
        Field field = GrassSlabsApi.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
