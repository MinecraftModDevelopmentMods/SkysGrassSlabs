package zone.moddev.mc.skysgrassslabs.compat;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.IForgeRegistryEntry;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacyRegistryWarningsTest {
    private static final String GRASS = "buildingbricks:grass_slab";
    private static final StackTraceElement[] FORGE_ORIGIN = {
            new StackTraceElement("net.minecraftforge.fml.common.FMLLog", "bigWarning", "Forge.java", 82),
            new StackTraceElement("net.minecraftforge.fml.common.registry.IForgeRegistryEntry$Impl", "setRegistryName", "IForgeRegistryEntry.java", 90)
    };

    @Test
    void realLoggingKeepsUnrelatedWarningsErrorsAndOutOfScopeMessages() {
        org.apache.logging.log4j.Logger apiLogger = net.minecraftforge.fml.common.FMLLog.getLogger();
        if (apiLogger == null) {
            apiLogger = LogManager.getLogger("FML");
        }
        org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) apiLogger;
        java.util.List<String> captured = new java.util.ArrayList<>();
        AbstractAppender appender = new AbstractAppender("LegacyRegistryWarningTest", null, null) {
            @Override
            public void append(LogEvent event) {
                captured.add(event.getMessage().getFormattedMessage());
            }
        };
        Level previousLevel = logger.getLevel();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.ALL);
        try {
            Entry entry = new Entry(() -> {
                logger.warn("Unexpected registration warning");
                logger.error("Actual registration error");
                logger.warn(message(GRASS), new IllegalStateException("A real exception"));
            });
            LegacyRegistryWarnings.setName(entry, new ResourceLocation(GRASS));
            logger.warn(message(GRASS));
            assertEquals(4, captured.size());
            assertEquals("Unexpected registration warning", captured.get(0));
            assertEquals("Actual registration error", captured.get(1));
            assertEquals(message(GRASS), captured.get(2));
            assertEquals(message(GRASS), captured.get(3));
        } finally {
            logger.removeAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }
    }

    @Test
    void onlyTheThreeExactRecoveryNamesQualify() {
        for (String identity : new String[] {
                GRASS, "buildingbricks:dirt_slab", "buildingbrickscompatvanilla:grass_slab" }) {
            assertTrue(matches(identity, message(identity)), identity);
        }
        assertFalse(matches("buildingbricks:stone_slab", message("buildingbricks:stone_slab")));
        assertFalse(matches("othermod:grass_slab", message("othermod:grass_slab")));
        assertFalse(matches(null, message(GRASS)));
    }

    @Test
    void anotherNameOrRegisteringModIsNotHidden() {
        assertFalse(matches(GRASS, message("buildingbricks:dirt_slab")));
        assertFalse(matches(GRASS, message(GRASS).replace("skysgrassslabs", "anothermod")));
        assertFalse(matches(GRASS, message(GRASS) + " Additional registry error"));
        assertFalse(matches(GRASS, "A different warning"));
    }

    @Test
    void otherLevelsLoggersExceptionsAndCallSitesStayVisible() {
        assertFalse(LegacyRegistryWarnings.matches(GRASS, "anothermod", Level.WARN,
                message(GRASS), null, FORGE_ORIGIN));
        for (Level level : new Level[] { Level.INFO, Level.ERROR, Level.FATAL }) {
            assertFalse(LegacyRegistryWarnings.matches(GRASS, "FML", level,
                    message(GRASS), null, FORGE_ORIGIN));
        }
        assertFalse(LegacyRegistryWarnings.matches(GRASS, "FML", Level.WARN,
                message(GRASS), new IllegalStateException("real failure"), FORGE_ORIGIN));
        assertFalse(LegacyRegistryWarnings.matches(GRASS, "FML", Level.WARN,
                message(GRASS), null, new StackTraceElement[0]));
        assertFalse(LegacyRegistryWarnings.matches(GRASS, "FML", Level.WARN,
                message(GRASS), null, new StackTraceElement[] { FORGE_ORIGIN[1] }));
    }

    @Test
    void theScopeEndsAfterSuccessOrFailure() {
        Entry successful = new Entry(() -> assertTrue(active(GRASS)));
        LegacyRegistryWarnings.setName(successful, new ResourceLocation(GRASS));
        assertEquals(GRASS, successful.getRegistryName().toString());
        assertFalse(active(GRASS));

        Entry failing = new Entry(() -> {
            assertTrue(active(GRASS));
            throw new IllegalStateException("registration failed");
        });
        assertThrows(IllegalStateException.class,
                () -> LegacyRegistryWarnings.setName(failing, new ResourceLocation(GRASS)));
        assertFalse(active(GRASS));
    }

    @Test
    void nestedNamesRestoreTheOuterScopeAndOtherThreadsAreUnaffected() {
        String dirt = "buildingbricks:dirt_slab";
        Entry outer = new Entry(() -> {
            LegacyRegistryWarnings.setName(new Entry(() -> assertTrue(active(dirt))),
                    new ResourceLocation(dirt));
            assertTrue(active(GRASS));
            AtomicBoolean hiddenElsewhere = new AtomicBoolean(true);
            Thread worker = new Thread(() -> hiddenElsewhere.set(active(GRASS)));
            worker.start();
            try {
                worker.join();
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(failure);
            }
            assertFalse(hiddenElsewhere.get());
        });
        LegacyRegistryWarnings.setName(outer, new ResourceLocation(GRASS));
        assertFalse(active(GRASS));
    }

    @Test
    void unknownNamesNeverEnterTheFilterScope() {
        Entry entry = new Entry(() -> fail("Unknown registration must not be invoked"));
        assertThrows(IllegalArgumentException.class,
                () -> LegacyRegistryWarnings.setName(entry, new ResourceLocation("buildingbricks:stone_slab")));
        assertNull(entry.getRegistryName());
    }

    @Test
    void diagnosticRowsAreOnlyHiddenAtTheKnownForgeCallSite() {
        assertTrue(matches(GRASS, "****************************************"));
        assertTrue(matches(GRASS,
                "*  at net.minecraftforge.fml.common.registry.IForgeRegistryEntry$Impl.setRegistryName(Forge.java:82)"));
        assertFalse(matches(GRASS, "* A real unrelated warning"));
        assertFalse(LegacyRegistryWarnings.matches(GRASS, "FML", Level.WARN,
                "****************************************", null, new StackTraceElement[0]));
    }

    private static boolean matches(String identity, String text) {
        return LegacyRegistryWarnings.matches(identity, "FML", Level.WARN, text, null, FORGE_ORIGIN);
    }

    private static boolean active(String identity) {
        return LegacyRegistryWarnings.hideForCurrentRegistration(
                "FML", Level.WARN, message(identity), null, FORGE_ORIGIN);
    }

    private static String message(String identity) {
        String[] parts = identity.split(":", 2);
        return "* Dangerous alternative prefix `" + parts[0] + "` for name `" + parts[1]
                + "`, expected `skysgrassslabs` invalid registry invocation/invalid name?";
    }

    private static final class Entry implements IForgeRegistryEntry<Entry> {
        private final Runnable duringRegistration;
        private ResourceLocation name;

        private Entry(Runnable duringRegistration) {
            this.duringRegistration = duringRegistration;
        }

        @Override
        public Entry setRegistryName(ResourceLocation name) {
            duringRegistration.run();
            this.name = name;
            return this;
        }

        @Override
        public ResourceLocation getRegistryName() {
            return name;
        }

        @Override
        public Class<Entry> getRegistryType() {
            return Entry.class;
        }
    }
}
