package zone.moddev.mc.skysgrassslabs.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LegacyRegistrySnapshotTest {
    @Rule
    public final TemporaryFolder folders = new TemporaryFolder();

    @Test
    public void copiesTheCompleteSnapshotWithoutReplacingCurrentForgeData() throws IOException {
        File world = folders.newFolder("world");
        File levelFile = new File(world, "level.dat");
        CompoundTag registries = new CompoundTag();
        registries.put("minecraft:blocks", new CompoundTag());
        registries.put("minecraft:items", new CompoundTag());
        CompoundTag legacy = new CompoundTag();
        legacy.put("Registries", registries);
        legacy.putString("OtherModData", "Keep this too");
        CompoundTag source = new CompoundTag();
        source.put("FML", legacy);
        try (var output = Files.newOutputStream(levelFile.toPath())) {
            NbtIo.writeCompressed(source, output);
        }

        try {
            LegacyWorldDataHook.prepareLegacyWorld(levelFile);
            CompoundTag current = new CompoundTag();
            current.putString("Marker", "Current Forge data");
            CompoundTag saved = new CompoundTag();
            saved.put("fml", current);
            LegacyWorldDataHook.writeLegacyLevelData(saved);
            assertEquals(legacy, saved.getCompound("FML"));
            assertEquals(current, saved.getCompound("fml"));

            saved.remove("FML");
            try (var output = Files.newOutputStream(levelFile.toPath())) {
                NbtIo.writeCompressed(saved, output);
            }
            LegacyWorldDataHook.prepareLegacyWorld(levelFile);
            LegacyWorldDataHook.writeLegacyLevelData(saved);
            assertEquals(legacy, saved.getCompound("FML"));

            File emptyWorld = folders.newFolder("empty-world");
            LegacyWorldDataHook.prepareLegacyWorld(new File(emptyWorld, "level.dat"));
            CompoundTag unrelatedSave = new CompoundTag();
            LegacyWorldDataHook.writeLegacyLevelData(unrelatedSave);
            assertFalse(unrelatedSave.contains("FML"));
        } finally {
            LegacyWorldDataHook.prepareLegacyWorld(new File(world, "missing-level.dat"));
        }
    }
}
