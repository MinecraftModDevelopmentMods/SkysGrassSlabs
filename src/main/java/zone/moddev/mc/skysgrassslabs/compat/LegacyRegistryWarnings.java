package zone.moddev.mc.skysgrassslabs.compat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.filter.CompositeFilter;

/** Keeps intentional recovery names readable without hiding other registry problems. */
final class LegacyRegistryWarnings {
    private static final Set<String> RECOVERY_IDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "buildingbricks:grass_slab",
            "buildingbricks:dirt_slab",
            "buildingbrickscompatvanilla:grass_slab")));
    private static final ThreadLocal<String> CURRENT_NAME = new ThreadLocal<>();
    private static final Filter FILTER = new RegistrationFilter();

    private LegacyRegistryWarnings() {
    }

    static void setName(IForgeRegistryEntry<?> entry, ResourceLocation name) {
        String identity = name.toString();
        if (!RECOVERY_IDS.contains(identity)) {
            throw new IllegalArgumentException("Not a supported legacy recovery name: " + identity);
        }

        installFilter();
        // The installed filter is neutral everywhere except this thread's naming call.
        String previous = CURRENT_NAME.get();
        CURRENT_NAME.set(identity);
        try {
            entry.setRegistryName(name);
        } finally {
            if (previous == null) {
                CURRENT_NAME.remove();
            } else {
                CURRENT_NAME.set(previous);
            }
        }
    }

    private static synchronized void installFilter() {
        // Forge's logger can belong to a different context from the mod's logger.
        org.apache.logging.log4j.Logger logger = net.minecraftforge.fml.common.FMLLog.log;
        if (logger == null) {
            logger = LogManager.getLogger("FML");
        }
        if (!(logger instanceof org.apache.logging.log4j.core.Logger)) {
            // An unfamiliar logging backend keeps Forge's original warning.
            return;
        }

        org.apache.logging.log4j.core.Logger forgeLogger = (org.apache.logging.log4j.core.Logger) logger;
        Configuration configuration = forgeLogger.getContext().getConfiguration();
        // Configuration filters run before a message reaches an asynchronous queue.
        if (!containsFilter(configuration.getFilter())) {
            configuration.addFilter(FILTER);
        }
    }

    private static boolean containsFilter(Filter filter) {
        if (filter == FILTER) {
            return true;
        }
        if (filter instanceof CompositeFilter) {
            for (Filter child : (CompositeFilter) filter) {
                if (containsFilter(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean hideForCurrentRegistration(String logger, Level level, String message,
            Throwable thrown, StackTraceElement[] stack) {
        return matches(CURRENT_NAME.get(), logger, level, message, thrown, stack);
    }

    static boolean matches(String identity, String logger, Level level, String message,
            Throwable thrown, StackTraceElement[] stack) {
        if (!RECOVERY_IDS.contains(identity) || !"FML".equals(logger) || !Level.WARN.equals(level)
                || message == null || thrown != null || stack == null) {
            return false;
        }

        boolean namingEntry = false;
        boolean checkingPrefix = false;
        for (StackTraceElement frame : stack) {
            namingEntry |= "net.minecraftforge.registries.IForgeRegistryEntry$Impl".equals(frame.getClassName())
                    && "setRegistryName".equals(frame.getMethodName());
            checkingPrefix |= "net.minecraftforge.registries.GameData".equals(frame.getClassName())
                    && "checkPrefix".equals(frame.getMethodName());
        }
        if (!namingEntry || !checkingPrefix) {
            return false;
        }

        String[] parts = identity.split(":", 2);
        String expected = "Potentially Dangerous alternative prefix `" + parts[0] + "` for name `" + parts[1]
                + "`, expected `skysgrassslabs`. This could be a intended override, but in most cases indicates a broken mod.";
        return expected.equals(message);
    }

    private static final class RegistrationFilter extends AbstractFilter {
        @Override
        public Result filter(org.apache.logging.log4j.core.Logger logger, Level level,
                Marker marker, Message message, Throwable thrown) {
            if (CURRENT_NAME.get() == null || !Level.WARN.equals(level)
                    || !"FML".equals(logger.getName()) || thrown != null || message == null
                    || message.getThrowable() != null) {
                return Result.NEUTRAL;
            }
            return hideForCurrentRegistration(logger.getName(), level, message.getFormattedMessage(), null,
                    Thread.currentThread().getStackTrace()) ? Result.DENY : Result.NEUTRAL;
        }

        @Override
        public Result filter(org.apache.logging.log4j.core.Logger logger, Level level,
                Marker marker, String message, Object... parameters) {
            if (CURRENT_NAME.get() == null || !Level.WARN.equals(level) || !"FML".equals(logger.getName())) {
                return Result.NEUTRAL;
            }
            return filter(logger, level, marker, logger.getMessageFactory().newMessage(message, parameters), null);
        }

        @Override
        public Result filter(org.apache.logging.log4j.core.Logger logger, Level level,
                Marker marker, Object message, Throwable thrown) {
            if (CURRENT_NAME.get() == null || !Level.WARN.equals(level)
                    || !"FML".equals(logger.getName()) || thrown != null) {
                return Result.NEUTRAL;
            }
            return filter(logger, level, marker,
                    logger.getMessageFactory().newMessage(message), thrown);
        }
    }
}
