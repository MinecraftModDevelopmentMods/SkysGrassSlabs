package zone.moddev.mc.skysgrassslabs.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import zone.moddev.mc.skysgrassslabs.MinecraftTestBootstrap;

class ExpandedHeightSmoothingTest {
    @BeforeAll
    static void registerVanilla() {
        MinecraftTestBootstrap.registerVanilla();
    }

    @ParameterizedTest
    @ValueSource(ints = {-63, -33, -1, 0, 255, 318})
    void heightmapSurfaceScanSupportsTheWholeOverworld(int height) throws Exception {
        ProtoChunk chunk = new ProtoChunk(new ChunkPos(0, 0), UpgradeData.EMPTY,
                MinecraftTestBootstrap.overworldHeight(), MinecraftTestBootstrap.biomes(), null);
        BlockPos surface = new BlockPos(8, height, 8);
        chunk.setBlockState(surface, Blocks.GRASS_BLOCK.defaultBlockState(), false);
        chunk.setBlockState(surface.above(), Blocks.FERN.defaultBlockState(), false);
        Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.WORLD_SURFACE_WG));

        var scan = GrassSlabSmoothingFeature.class.getDeclaredMethod("surfaceY",
                ChunkAccess.class, int.class, int.class, BlockPos.MutableBlockPos.class);
        scan.setAccessible(true);
        assertEquals(height, scan.invoke(null, chunk, 8, 8, new BlockPos.MutableBlockPos()));
    }
}
