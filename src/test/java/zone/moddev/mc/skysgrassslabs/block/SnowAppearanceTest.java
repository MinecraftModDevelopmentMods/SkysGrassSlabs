package zone.moddev.mc.skysgrassslabs.block;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import zone.moddev.mc.skysgrassslabs.MinecraftTestBootstrap;
import zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi;

class SnowAppearanceTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.registerVanilla();
    }

    @Test
    void snowOnTheHigherTerraceReachesTheInterveningPiece() {
        for (EnumFacing direction : EnumFacing.HORIZONTALS) {
            Ground world = new Ground();
            BlockPos neighbour = BlockPos.ORIGIN.offset(direction);
            world.put(neighbour, Blocks.GRASS.getDefaultState());
            world.put(neighbour.up(), Blocks.SNOW_LAYER.getDefaultState());
            assertTrue(SnowySlabAppearance.hasNearbySnow(world, BlockPos.ORIGIN), direction.toString());
            world.put(neighbour.up(), Blocks.AIR.getDefaultState());
            assertFalse(SnowySlabAppearance.hasNearbySnow(world, BlockPos.ORIGIN));
        }
    }

    @Test
    void derivedSnowFlagsDoNotKeepEachOtherSnowyAfterTheSnowIsGone() {
        Ground world = new Ground();
        for (EnumFacing direction : EnumFacing.HORIZONTALS) {
            world.put(BlockPos.ORIGIN.offset(direction), Blocks.GRASS.getDefaultState()
                    .withProperty(BlockGrass.SNOWY, true));
        }
        assertFalse(SnowySlabAppearance.hasNearbySnow(world, BlockPos.ORIGIN));
    }

    @Test
    void registeredPhysicalSnowIsRecognisedWithoutDependingOnAContentMod() {
        Block snowPiece = new Block(Material.CRAFTED_SNOW);
        IBlockState snow = snowPiece.getDefaultState();
        GrassSlabsApi.registerSnowForm(snow);
        assertTrue(GrassSlabsApi.isSnowForm(snow));
        assertThrows(IllegalArgumentException.class, () -> GrassSlabsApi.registerSnowForm(snow));
        assertThrows(IllegalArgumentException.class, () -> GrassSlabsApi.registerSnowForm(null));
        assertThrows(IllegalArgumentException.class,
                () -> GrassSlabsApi.registerSnowForm(Blocks.AIR.getDefaultState()));
        assertThrows(IllegalArgumentException.class,
                () -> GrassSlabsApi.registerSnowForm(Blocks.CHEST.getDefaultState()));

        Ground world = new Ground();
        world.put(BlockPos.ORIGIN.east(), snow);
        assertTrue(GrassSlabsApi.hasSnowAppearance(world, BlockPos.ORIGIN));
        world.put(BlockPos.ORIGIN.east(), Blocks.AIR.getDefaultState());
        assertFalse(GrassSlabsApi.hasSnowAppearance(world, BlockPos.ORIGIN));
    }

    static final class Ground implements IBlockAccess {
        private final Map<BlockPos, IBlockState> blocks = new HashMap<>();

        void put(BlockPos position, IBlockState state) {
            blocks.put(position.toImmutable(), state);
        }

        public IBlockState getBlockState(BlockPos position) {
            return blocks.getOrDefault(position, Blocks.AIR.getDefaultState());
        }

        public TileEntity getTileEntity(BlockPos position) {
            return null;
        }

        public int getCombinedLight(BlockPos position, int minimum) {
            return 15 << 20;
        }

        public boolean isAirBlock(BlockPos position) {
            return getBlockState(position).getBlock() == Blocks.AIR;
        }

        public Biome getBiome(BlockPos position) {
            return Biomes.PLAINS;
        }

        public int getStrongPower(BlockPos position, EnumFacing direction) {
            return 0;
        }

        public WorldType getWorldType() {
            return WorldType.DEFAULT;
        }

        public boolean isSideSolid(BlockPos position, EnumFacing direction, boolean fallback) {
            return false;
        }
    }
}
