package zone.moddev.mc.skysgrassslabs.world;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import zone.moddev.mc.skysgrassslabs.MinecraftTestBootstrap;

class StructureProtectionTest {
    @BeforeAll
    static void registerVanilla() {
        MinecraftTestBootstrap.registerVanilla();
    }

    @Test
    void ownStructureBoundsAreInclusiveAndDoNotProtectUnrelatedTerrain() {
        ProtoChunk owner = chunk(0, 0);
        owner.setStartForStructure(MinecraftTestBootstrap.structure(net.minecraft.world.level.levelgen.structure.BuiltinStructures.MINESHAFT), start(new BoundingBox(2, 60, 3, 9, 70, 10)));
        StructureProtection protection = StructureProtection.read(owner, position -> {
            throw new AssertionError("An owner start must not request another chunk");
        });
        assertTrue(protection.canInspectSafely());
        assertTrue(protection.contains(new BlockPos(2, 60, 3)));
        assertTrue(protection.contains(new BlockPos(9, 70, 10)));
        assertFalse(protection.contains(new BlockPos(10, 65, 5)));
        assertFalse(protection.contains(new BlockPos(5, 71, 5)));
    }

    @Test
    void aReferencedStartInALoadedNeighbourIsReadOnce() {
        ProtoChunk owner = chunk(0, 0);
        ProtoChunk neighbour = chunk(1, 0);
        neighbour.setStartForStructure(MinecraftTestBootstrap.structure(net.minecraft.world.level.levelgen.structure.BuiltinStructures.MINESHAFT), start(new BoundingBox(12, 60, 0, 22, 75, 15)));
        owner.addReferenceForStructure(MinecraftTestBootstrap.structure(net.minecraft.world.level.levelgen.structure.BuiltinStructures.MINESHAFT), neighbour.getPos().toLong());
        owner.addReferenceForStructure(MinecraftTestBootstrap.structure(net.minecraft.world.level.levelgen.structure.BuiltinStructures.VILLAGE_PLAINS), neighbour.getPos().toLong());
        AtomicInteger lookups = new AtomicInteger();
        StructureProtection protection = StructureProtection.read(owner, position -> {
            assertEquals(neighbour.getPos(), position);
            lookups.incrementAndGet();
            return neighbour;
        });
        assertEquals(1, lookups.get());
        assertTrue(protection.canInspectSafely());
        assertTrue(protection.contains(new BlockPos(15, 65, 4)));
    }

    @Test
    void anUnavailableReferencedChunkPreventsWritesWithoutTryingToLoadIt() {
        ProtoChunk owner = chunk(0, 0);
        owner.addReferenceForStructure(MinecraftTestBootstrap.structure(net.minecraft.world.level.levelgen.structure.BuiltinStructures.MINESHAFT), new ChunkPos(8, 0).toLong());
        assertFalse(StructureProtection.read(owner, position -> null).canInspectSafely());
        ProtoChunk empty = chunk(0, 0);
        empty.setStartForStructure(MinecraftTestBootstrap.structure(net.minecraft.world.level.levelgen.structure.BuiltinStructures.MINESHAFT), StructureStart.INVALID_START);
        assertTrue(StructureProtection.read(empty, position -> null).canInspectSafely());
    }

    private static ProtoChunk chunk(int chunkX, int chunkZ) {
        return new ProtoChunk(new ChunkPos(chunkX, chunkZ), UpgradeData.EMPTY, MinecraftTestBootstrap.overworldHeight(), MinecraftTestBootstrap.biomes(), null);
    }

    private static StructureStart start(BoundingBox bounds) {
        net.minecraft.world.level.levelgen.structure.StructurePiece piece =
                new net.minecraft.world.level.levelgen.structure.StructurePiece(
                        net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType.MINE_SHAFT_ROOM,
                        0, bounds) {
                    @Override
                    protected void addAdditionalSaveData(
                            net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext context,
                            net.minecraft.nbt.CompoundTag tag) {
                    }

                    @Override
                    public void postProcess(net.minecraft.world.level.WorldGenLevel world,
                            net.minecraft.world.level.StructureManager structures,
                            ChunkGenerator generator, net.minecraft.util.RandomSource random, BoundingBox clip,
                            ChunkPos chunk, BlockPos position) {
                    }
                };
        return new StructureStart(MinecraftTestBootstrap.structure(net.minecraft.world.level.levelgen.structure.BuiltinStructures.MINESHAFT),
                new ChunkPos(0, 0), 0,
                new net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer(java.util.List.of(piece)));
    }
}
