package zone.moddev.mc.skysgrassslabs.world;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ModWorldStateTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void keepsTheCountersInAnUnversionedSave() throws Exception {
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
        CompoundTag expected = original.saveTag();
        // Old Grass Slabs versions stored their own data without a vanilla DataVersion.
        savedFile.put("data", expected);
        NbtIo.writeCompressed(savedFile, dataDirectory.resolve(ModWorldState.DATA_NAME + ".dat"));

        // Full world storage is exercised by the packaged upgrade checks,
        // which start NeoForge before Minecraft loads its saved data.
        CompoundTag stored = NbtIo.readCompressed(
                dataDirectory.resolve(ModWorldState.DATA_NAME + ".dat"), NbtAccounter.unlimitedHeap());
        ModWorldState migrated = ModWorldState.TYPE.codec()
                .parse(NbtOps.INSTANCE, stored.getCompoundOrEmpty("data"))
                .getOrThrow();
        assertEquals(expected, migrated.saveTag());
        assertEquals(expected, ModWorldState.TYPE.codec().encodeStart(NbtOps.INSTANCE, migrated).getOrThrow());
    }
}
