package zone.moddev.mc.skysgrassslabs;

import java.lang.reflect.Field;
import net.minecraft.init.Bootstrap;

public final class MinecraftTestBootstrap {
    private static boolean initialized;

    public static synchronized void registerVanilla() {
        if (initialized) return;
        try {
            Class<?> loader = Class.forName("net.minecraftforge.fml.loading.FMLLoader");
            set(loader, "mcVersion", "1.13.2");
            set(loader, "mcpVersion", "20190213.203750");
            set(loader, "forgeVersion", "25.0.223");
            set(loader, "forgeGroup", "net.minecraftforge");
            Bootstrap.register();
            initialized = true;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to initialize the Forge 25 test runtime",
                    exception);
        }
    }

    private static void set(Class<?> owner, String name, String value)
            throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private MinecraftTestBootstrap() {
    }
}
