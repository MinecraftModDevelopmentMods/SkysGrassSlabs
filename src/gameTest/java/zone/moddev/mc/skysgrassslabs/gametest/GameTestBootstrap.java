package zone.moddev.mc.skysgrassslabs.gametest;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.gametest.ForgeGameTestHooks;
import net.minecraftforge.gametest.ForgeGameTestHooks.TestReference;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.RegisterEvent;
import zone.moddev.mc.skysgrassslabs.compat.GrassSlabsMigrationGameTests;

/** Registers test-only classes without including them in the production jar. */
public final class GameTestBootstrap {
    private static final Map<Identifier, TestReference> TESTS = gatherTests();

    private GameTestBootstrap() {
    }

    public static void register(BusGroup modBusGroup) {
        RegisterEvent.getBus(modBusGroup).addListener(GameTestBootstrap::registerTestFunctions);
    }

    private static Map<Identifier, TestReference> gatherTests() {
        Map<Identifier, TestReference> tests = new LinkedHashMap<>();
        tests.putAll(ForgeGameTestHooks.gatherTests(SlabGameTests.class, null));
        tests.putAll(ForgeGameTestHooks.gatherTests(WorldgenGameTests.class, null));
        tests.putAll(ForgeGameTestHooks.gatherTests(GrassSlabsMigrationGameTests.class, null));
        return Map.copyOf(tests);
    }

    private static void registerTestFunctions(RegisterEvent event) {
        if (event.getRegistryKey() != Registries.TEST_FUNCTION) {
            return;
        }

        TESTS.forEach((id, reference) ->
                event.register(Registries.TEST_FUNCTION, id, reference::consumer));
    }

}
