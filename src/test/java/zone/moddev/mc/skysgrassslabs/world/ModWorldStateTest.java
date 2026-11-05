package zone.moddev.mc.skysgrassslabs.world;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ModWorldStateTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void readsUnversionedModDataAndKeepsItsCountersOnReload() throws Exception {
        SharedConstants.tryDetectVersion();

        ModWorldState original = new ModWorldState();
        original.recordChunk();
        original.recordGrassBlocks(11, 0);
        original.recordGrassBlocks(13, 1);
        original.recordDirtBlocks(17, 0);
        original.recordGrassItems(19);
        original.recordDirtItems(23);
        original.recordUnsupported("buildingbricks:vertical_dirt_slab", 2);

        Path dataDirectory = temporary.newFolder("world-data").toPath();
        CompoundTag savedFile = new CompoundTag();
        CompoundTag expected = original.save(new CompoundTag(), RegistryAccess.EMPTY);
        // Old Grass Slabs versions stored their own data without a vanilla DataVersion.
        savedFile.put("data", expected);
        NbtIo.writeCompressed(savedFile, dataDirectory.resolve(ModWorldState.DATA_NAME + ".dat"));

        DimensionDataStorage first = open(dataDirectory);
        ModWorldState migrated = first.computeIfAbsent(ModWorldState.factory(), ModWorldState.DATA_NAME);
        assertEquals(expected, migrated.save(new CompoundTag(), RegistryAccess.EMPTY));

        migrated.setDirty();
        first.save();
        ModWorldState reloaded = open(dataDirectory)
                .computeIfAbsent(ModWorldState.factory(), ModWorldState.DATA_NAME);
        assertEquals(expected, reloaded.save(new CompoundTag(), RegistryAccess.EMPTY));
    }

    private static DimensionDataStorage open(Path dataDirectory) {
        return new DimensionDataStorage(dataDirectory.toFile(), DataFixers.getDataFixer(), RegistryAccess.EMPTY);
    }
}
