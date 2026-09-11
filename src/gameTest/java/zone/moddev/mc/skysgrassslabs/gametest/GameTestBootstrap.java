package zone.moddev.mc.skysgrassslabs.gametest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;
import zone.moddev.mc.skysgrassslabs.SkysGrassSlabs;
import zone.moddev.mc.skysgrassslabs.compat.GrassSlabsMigrationGameTests;

/** Registers test-only functions without including them in the production jar. */
public final class GameTestBootstrap {
    private static final Map<Identifier, Consumer<GameTestHelper>> TESTS = gatherTests();

    private GameTestBootstrap() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(GameTestBootstrap::registerTestFunctions);
    }

    private static Map<Identifier, Consumer<GameTestHelper>> gatherTests() {
        Map<Identifier, Consumer<GameTestHelper>> tests = new LinkedHashMap<>();
        gatherTests(tests, SlabGameTests.class, "slab");
        gatherTests(tests, WorldgenGameTests.class, "worldgen");
        gatherTests(tests, GrassSlabsMigrationGameTests.class, "migration");
        return Map.copyOf(tests);
    }

    private static void gatherTests(Map<Identifier, Consumer<GameTestHelper>> tests,
            Class<?> testClass, String prefix) {
        for (Method method : testClass.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers())
                    || !Modifier.isStatic(method.getModifiers())
                    || method.getParameterCount() != 1
                    || method.getParameterTypes()[0] != GameTestHelper.class) {
                continue;
            }
            Identifier id = Identifier.fromNamespaceAndPath(SkysGrassSlabs.MOD_ID,
                    prefix + "/" + snakeCase(method.getName()));
            tests.put(id, helper -> invoke(method, helper));
        }
    }

    private static String snakeCase(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toLowerCase(java.util.Locale.ROOT);
    }

    private static void invoke(Method method, GameTestHelper helper) {
        try {
            method.invoke(null, helper);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not invoke GameTest " + method.getName(), exception);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("GameTest failed: " + method.getName(), exception.getCause());
        }
    }

    private static void registerTestFunctions(RegisterEvent event) {
        if (event.getRegistryKey() != Registries.TEST_FUNCTION) {
            return;
        }
        TESTS.forEach((id, test) -> event.register(Registries.TEST_FUNCTION, id, () -> test));
    }
}
