package zone.moddev.mc.skysgrassslabs;

import java.lang.reflect.Field;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

public final class MinecraftTestBootstrap {
    private static boolean initialized;
    private static net.minecraft.core.HolderLookup.Provider worldgen;
    private static net.minecraft.core.Registry<net.minecraft.world.level.biome.Biome> biomes;

    public static net.minecraft.core.Registry<net.minecraft.world.level.biome.Biome> biomes() {
        if (biomes == null) {
            net.minecraft.core.MappedRegistry<net.minecraft.world.level.biome.Biome> registry =
                    new net.minecraft.core.MappedRegistry<>(
                            net.minecraft.core.registries.Registries.BIOME,
                            com.mojang.serialization.Lifecycle.stable());
            worldgen().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME)
                    .listElements().forEach(holder -> net.minecraft.core.Registry.register(
                            registry, holder.key(), holder.value()));
            biomes = registry.freeze();
        }
        return biomes;
    }

    public static net.minecraft.world.level.levelgen.structure.Structure structure(
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> key) {
        return worldgen().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .getOrThrow(key).value();
    }

    private static net.minecraft.core.HolderLookup.Provider worldgen() {
        if (worldgen == null) {
            worldgen = net.minecraft.data.registries.VanillaRegistries.createLookup();
        }
        return worldgen;
    }

    public static net.minecraft.world.level.LevelHeightAccessor overworldHeight() {
        return new net.minecraft.world.level.LevelHeightAccessor() {
            @Override
            public int getHeight() {
                return 384;
            }

            @Override
            public int getMinBuildHeight() {
                return -64;
            }
        };
    }

    public static synchronized void registerVanilla() {
        if (initialized) return;
        try {
            Class<?> loader = Class.forName("net.minecraftforge.fml.loading.FMLLoader");
            Class<?> versionInfo = Class.forName("net.minecraftforge.fml.loading.VersionInfo");
            Object forgeVersion = versionInfo
                    .getConstructor(String.class, String.class, String.class, String.class)
                    .newInstance("47.1.106", "1.20.1", "20230612.114412", "net.neoforged");
            set(loader, "versionInfo", forgeVersion);
            SharedConstants.tryDetectVersion();
            // Plain JUnit has no ModLauncher event transformation. Build the
            // registries here; real Forge launches cover the networking bootstrap.
            set(Bootstrap.class, "isBootstrapped", true);
            net.minecraft.core.registries.BuiltInRegistries.BLOCK.size();
            net.minecraft.core.registries.BuiltInRegistries.bootStrap();
            net.minecraftforge.registries.GameData.vanillaSnapshot();
            // Focused tests create blocks outside Forge's normal registration event.
            ((net.minecraft.core.MappedRegistry<?>)
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK).unfreeze();
            ((net.minecraft.core.MappedRegistry<?>)
                    net.minecraft.core.registries.BuiltInRegistries.ITEM).unfreeze();
            ((net.minecraftforge.registries.ForgeRegistry<?>)
                    net.minecraftforge.registries.ForgeRegistries.BLOCKS).unfreeze();
            ((net.minecraftforge.registries.ForgeRegistry<?>)
                    net.minecraftforge.registries.ForgeRegistries.ITEMS).unfreeze();
            initialized = true;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to initialize the Forge 47 test runtime",
                    exception);
        }
    }

    private static void set(Class<?> owner, String name, Object value)
            throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private MinecraftTestBootstrap() {
    }
}
