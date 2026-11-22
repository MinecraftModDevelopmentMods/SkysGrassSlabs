package zone.moddev.mc.skysgrassslabs.integrationtest;

import java.lang.reflect.Field;
import net.minecraft.world.level.storage.LevelStorageSource.LevelDirectory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.WorldData;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.apache.logging.log4j.LogManager;
import zone.moddev.mc.skysgrassslabs.compat.LegacyWorldDataHook;

/** Check queued listener registration and Forge 47's native level-data entry points. */
public final class PersistenceRegistrationChecks {
    public static void register(IEventBus bus) {
        bus.addListener(PersistenceRegistrationChecks::beforeQueuedWork);
        bus.addListener(PersistenceRegistrationChecks::afterQueuedWork);
    }

    private static void beforeQueuedWork(FMLCommonSetupEvent event) {
        require(!registered(), "Legacy listeners must wait for queued common setup");
        LogManager.getLogger().info("SKY_PERSISTENCE_SETUP_BEFORE_PASS grass_listener=false");
    }

    private static void afterQueuedWork(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            require(!Thread.currentThread().getName().startsWith("modloading-worker"),
                    "Queued verification must run outside parallel mod loading");
            require(registered(), "The legacy server listener was not registered");
            try {
                ForgeHooks.class.getDeclaredMethod("readAdditionalLevelSaveData", CompoundTag.class, LevelDirectory.class);
                ForgeHooks.class.getDeclaredMethod("writeAdditionalLevelSaveData", WorldData.class, CompoundTag.class);
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("Forge's level-data hook signatures changed", exception);
            }
            LogManager.getLogger().info("SKY_PERSISTENCE_SETUP_AFTER_PASS grass_listener=true mechanism=forge_level_hooks");
        });
    }

    private static boolean registered() {
        try {
            Field field = LegacyWorldDataHook.class.getDeclaredField("registered");
            field.setAccessible(true);
            return field.getBoolean(null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Cannot inspect the legacy listener's registration", exception);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private PersistenceRegistrationChecks() {
    }
}
