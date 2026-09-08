package zone.moddev.mc.skysgrassslabs.gametest;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import zone.moddev.mc.skysgrassslabs.compat.GrassSlabsMigrationGameTests;

/** Registers test-only classes without including them in the production jar. */
public final class GameTestBootstrap {
    private GameTestBootstrap() {
    }

    public static void register(IEventBus modBus) {
        writeEmptyStructure();
        modBus.addListener(GameTestBootstrap::registerTests);
    }

    private static void writeEmptyStructure() {
        CompoundTag root = new CompoundTag();
        NbtUtils.addCurrentDataVersion(root);

        ListTag size = new ListTag();
        size.add(IntTag.valueOf(8));
        size.add(IntTag.valueOf(4));
        size.add(IntTag.valueOf(8));
        root.put("size", size);

        CompoundTag air = new CompoundTag();
        air.putString("Name", "minecraft:air");
        ListTag palette = new ListTag();
        palette.add(air);
        root.put("palette", palette);
        root.put("blocks", new ListTag());
        root.put("entities", new ListTag());

        Path structure = Path.of("world", "generated", "skysgrassslabs",
                "structures", "empty.nbt");
        try {
            Files.createDirectories(structure.getParent());
            try (OutputStream output = Files.newOutputStream(structure)) {
                NbtIo.writeCompressed(root, output);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not prepare the GameTest structure", exception);
        }
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        event.register(SlabGameTests.class);
        event.register(WorldgenGameTests.class);
        event.register(GrassSlabsMigrationGameTests.class);
    }
}
