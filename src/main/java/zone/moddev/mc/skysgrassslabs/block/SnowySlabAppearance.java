package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.init.Blocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.ChunkCache;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi;

/** Calculated visuals only; neither snow flags nor slab metadata are written to the world. */
public final class SnowySlabAppearance {
    public static boolean hasNearbySnow(IBlockAccess world, BlockPos pos) {
        if (GrassSlabsApi.isSnowForm(readState(world, pos.up()))) {
            return true;
        }
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            BlockPos neighbourPos = pos.offset(facing);
            IBlockState neighbour = readState(world, neighbourPos);
            // A higher terrace carries its snow one block above our horizontal neighbour.
            if (GrassSlabsApi.isSnowForm(neighbour) || neighbour.isFullCube()
                    && GrassSlabsApi.isSnowForm(readState(world, neighbourPos.up()))) {
                return true;
            }
        }
        return isExposedColdGround(world, pos);
    }

    private static boolean isExposedColdGround(IBlockAccess access, BlockPos pos) {
        World world = weatherWorld(access);
        if (world == null || pos.getY() < 0 || pos.getY() >= 256 || !world.isBlockLoaded(pos)
                || world.provider.hasNoSky() || world.getBiome(pos).getFloatTemperature(pos) >= 0.15F) {
            return false;
        }
        // The precipitation map includes roofs such as glass, unlike the light-opacity height map.
        // It is cached per column, so rendering never scans up through the world.
        return world.getPrecipitationHeight(pos).getY() <= pos.getY() + 1
                && world.getLightFor(EnumSkyBlock.BLOCK, pos.up()) < 10;
    }

    private static World weatherWorld(IBlockAccess access) {
        if (access instanceof World) {
            return (World) access;
        }
        if (access instanceof ChunkCache) {
            try {
                return (World) CacheWorld.WORLD.get(access);
            } catch (IllegalAccessException failure) {
                throw new IllegalStateException("Cannot read weather from Minecraft's chunk cache", failure);
            }
        }
        return null;
    }

    private static final class CacheWorld {
        // Read the cache's own world, including server/pathfinding caches and other dimensions.
        // Keeping the field lookup here avoids linking a render cache during ordinary startup.
        static final java.lang.reflect.Field WORLD = ReflectionHelper.findField(
                ChunkCache.class, "world", "field_72815_e");
    }

    private static IBlockState readState(IBlockAccess world, BlockPos pos) {
        if (pos.getY() < 0 || pos.getY() >= 256
                || world instanceof World && !((World) world).isBlockLoaded(pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return world.getBlockState(pos);
    }

    private SnowySlabAppearance() {
    }
}
