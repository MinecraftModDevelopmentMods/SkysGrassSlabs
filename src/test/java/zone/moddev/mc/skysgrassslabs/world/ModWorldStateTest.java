package zone.moddev.mc.skysgrassslabs.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.nbt.CompoundNBT;
import org.junit.jupiter.api.Test;

class ModWorldStateTest {
    @Test
    void schemaAndMigrationCountersRoundTrip() {
        ModWorldState original = new ModWorldState(ModWorldState.DATA_NAME);
        original.recordChunk();
        original.recordGrassBlocks(3, 0);
        original.recordGrassBlocks(1, 1);
        original.recordDirtBlocks(2, 1);
        original.recordGrassItems(3);
        original.recordDirtItems(5);
        original.recordUnsupported("block:buildingbricks:oak_step", 7);

        CompoundNBT nbt = original.write(new CompoundNBT());
        ModWorldState restored = new ModWorldState(ModWorldState.DATA_NAME);
        restored.read(nbt);

        assertEquals(1, nbt.getInt("schema_version"));
        assertEquals(1, nbt.getInt("buildingbricks_migration_version"));
        assertEquals(1, restored.migratedChunks());
        assertEquals(4, restored.migratedGrassBlocks());
        assertEquals(3, restored.migratedGrassBlocksTop());
        assertEquals(1, restored.migratedGrassBlocksBottom());
        assertEquals(2, restored.migratedDirtBlocks());
        assertEquals(0, restored.migratedDirtBlocksTop());
        assertEquals(2, restored.migratedDirtBlocksBottom());
        assertEquals(3, restored.migratedGrassItems());
        assertEquals(5, restored.migratedDirtItems());
        assertEquals(7, restored.unsupported().get("block:buildingbricks:oak_step"));
    }
}
