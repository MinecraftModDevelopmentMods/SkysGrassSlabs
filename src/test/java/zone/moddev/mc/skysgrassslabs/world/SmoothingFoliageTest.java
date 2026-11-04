package zone.moddev.mc.skysgrassslabs.world;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import zone.moddev.mc.skysgrassslabs.MinecraftTestBootstrap;

import static org.junit.jupiter.api.Assertions.*;

class SmoothingFoliageTest {
    @BeforeAll
    static void registerVanilla() {
        MinecraftTestBootstrap.registerVanilla();
    }

    @Test
    void nativePlantsCanGiveWayButCropsFluidsAndOccupiedBlocksCannot() {
        ProtoChunk chunk = new ProtoChunk(new ChunkPos(0, 0), UpgradeData.EMPTY, MinecraftTestBootstrap.overworldHeight(), MinecraftTestBootstrap.biomes(), null);
        BlockPos target = new BlockPos(8, 65, 8);
        for (net.minecraft.world.level.block.Block plant : new net.minecraft.world.level.block.Block[] {
                Blocks.AIR, Blocks.GRASS, Blocks.FERN, Blocks.DANDELION, Blocks.POPPY}) {
            chunk.setBlockState(target, plant.defaultBlockState(), false);
            assertTrue(SmoothingFoliage.canReplace(null, chunk, target), plant.toString());
        }
        for (net.minecraft.world.level.block.Block protectedBlock : new net.minecraft.world.level.block.Block[] {
                Blocks.WHEAT, Blocks.OAK_SAPLING, Blocks.OAK_LOG, Blocks.WATER,
                Blocks.LAVA, Blocks.CHEST, Blocks.STONE}) {
            chunk.setBlockState(target, protectedBlock.defaultBlockState(), false);
            assertFalse(SmoothingFoliage.canReplace(null, chunk, target), protectedBlock.toString());
        }
    }

    @Test
    void aDoublePlantNeedsBothMatchingHalvesAndCannotBeReplacedFromItsUpperHalf() {
        ProtoChunk chunk = new ProtoChunk(new ChunkPos(0, 0), UpgradeData.EMPTY, MinecraftTestBootstrap.overworldHeight(), MinecraftTestBootstrap.biomes(), null);
        BlockPos target = new BlockPos(8, 65, 8);
        chunk.setBlockState(target, Blocks.TALL_GRASS.defaultBlockState()
                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), false);
        assertFalse(SmoothingFoliage.canReplace(null, chunk, target));
        chunk.setBlockState(target.above(), Blocks.TALL_GRASS.defaultBlockState()
                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), false);
        assertTrue(SmoothingFoliage.canReplace(null, chunk, target));
        assertFalse(SmoothingFoliage.canReplace(null, chunk, target.above()));
        chunk.setBlockState(target.above(), Blocks.LARGE_FERN.defaultBlockState()
                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), false);
        assertFalse(SmoothingFoliage.canReplace(null, chunk, target));
    }

    @Test
    void liveAndQueuedBlockEntitiesProtectOtherwiseReplaceableTargets() {
        BlockPos target = new BlockPos(8, 65, 8);
        ProtoChunk live = new ProtoChunk(new ChunkPos(0, 0), UpgradeData.EMPTY, MinecraftTestBootstrap.overworldHeight(), MinecraftTestBootstrap.biomes(), null);
        live.setBlockState(target, Blocks.GRASS.defaultBlockState(), false);
        live.setBlockEntity(new net.minecraft.world.level.block.entity.ChestBlockEntity(target, Blocks.CHEST.defaultBlockState()));
        assertFalse(SmoothingFoliage.canReplace(null, live, target),
                "A live block entity must not be lost even if its block state is replaceable");

        ProtoChunk queued = new ProtoChunk(new ChunkPos(0, 0), UpgradeData.EMPTY, MinecraftTestBootstrap.overworldHeight(), MinecraftTestBootstrap.biomes(), null);
        queued.setBlockState(target, Blocks.GRASS.defaultBlockState(), false);
        net.minecraft.nbt.CompoundTag stored = new net.minecraft.nbt.CompoundTag();
        stored.putString("id", "minecraft:chest");
        stored.putInt("x", target.getX());
        stored.putInt("y", target.getY());
        stored.putInt("z", target.getZ());
        queued.setBlockEntityNbt(stored);
        assertFalse(SmoothingFoliage.canReplace(null, queued, target),
                "A queued block entity must remain protected before it is instantiated");

        ProtoChunk upper = new ProtoChunk(new ChunkPos(0, 0), UpgradeData.EMPTY, MinecraftTestBootstrap.overworldHeight(), MinecraftTestBootstrap.biomes(), null);
        upper.setBlockState(target, Blocks.TALL_GRASS.defaultBlockState()
                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), false);
        upper.setBlockState(target.above(), Blocks.TALL_GRASS.defaultBlockState()
                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), false);
        upper.setBlockEntity(new net.minecraft.world.level.block.entity.ChestBlockEntity(target.above(), Blocks.CHEST.defaultBlockState()));
        assertFalse(SmoothingFoliage.canReplace(null, upper, target),
                "Replacing a double plant must also preserve a live block entity in its upper half");
    }
}
