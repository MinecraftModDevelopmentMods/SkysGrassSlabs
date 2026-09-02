package zone.moddev.mc.skysgrassslabs;

import net.minecraft.init.Bootstrap;

public final class MinecraftTestBootstrap {
    private static boolean initialized;

    public static synchronized void registerVanilla() {
        if (!initialized) {
            Bootstrap.register();
            initialized = true;
        }
    }

    private MinecraftTestBootstrap() {
    }
}
