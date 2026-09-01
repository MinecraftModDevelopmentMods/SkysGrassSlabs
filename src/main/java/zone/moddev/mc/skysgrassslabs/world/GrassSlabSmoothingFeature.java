package zone.moddev.mc.skysgrassslabs.world;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import zone.moddev.mc.skysgrassslabs.config.BetaConfig;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Deterministic smoother owned by each chunk for natural grass transitions one block high. */
public final class GrassSlabSmoothingFeature extends Feature<NoneFeatureConfiguration> {
    private static final int HALO_WIDTH = 18;
    private static final int HALO_COLUMNS = HALO_WIDTH * HALO_WIDTH;

    public GrassSlabSmoothingFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!BetaConfig.GENERATE_GRASS_SLABS.get()
                || level.getLevel().dimension() != Level.OVERWORLD) {
            return false;
        }

        ChunkAccess chunk = level.getChunk(context.origin());
        ChunkPos owner = chunk.getPos();
        int minX = owner.getMinBlockX();
        int minZ = owner.getMinBlockZ();

        int[] heights = new int[HALO_COLUMNS];
        long[] grass = new long[(HALO_COLUMNS + 63) >>> 6];
        long[] candidates = new long[4];
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int haloX = 0; haloX < HALO_WIDTH; haloX++) {
            for (int haloZ = 0; haloZ < HALO_WIDTH; haloZ++) {
                int x = minX + haloX - 1;
                int z = minZ + haloZ - 1;
                int index = haloIndex(haloX, haloZ);
                int surfaceY = grassSurfaceY(level, cursor, x, z);

                heights[index] = surfaceY;

                if (surfaceY != Integer.MIN_VALUE) {
                    set(grass, index);
                }
            }
        }

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int haloX = localX + 1;
                int haloZ = localZ + 1;
                int center = haloIndex(haloX, haloZ);
                int y = heights[center];

                if (y == Integer.MIN_VALUE) {
                    continue;
                }

                int x = minX + localX;
                int z = minZ + localZ;

                cursor.set(x, y + 1, z);

                BlockState target = level.getBlockState(cursor);
                boolean clear = target.isAir() && level.getBlockEntity(cursor) == null;
                boolean dry = target.getFluidState().isEmpty();
                boolean supported = supportedOnAllSides(level, cursor, x, y, z);

                int north = haloIndex(haloX, haloZ - 1);
                int south = haloIndex(haloX, haloZ + 1);
                int west = haloIndex(haloX - 1, haloZ);
                int east = haloIndex(haloX + 1, haloZ);

                if (SmoothingDecision.shouldPlace(y, heights[north], heights[south],
                        heights[west], heights[east], true, get(grass, north),
                        get(grass, south), get(grass, west), get(grass, east), clear,
                        dry, supported)) {
                    set(candidates, localX * 16 + localZ);
                }
            }
        }

        BlockState slab = ModBlocks.GRASS_SLAB.get().defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.BOTTOM)
                .setValue(SlabBlock.WATERLOGGED, false);
        boolean changed = false;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int candidate = localX * 16 + localZ;

                if (!get(candidates, candidate)) {
                    continue;
                }

                int y = heights[haloIndex(localX + 1, localZ + 1)];

                cursor.set(minX + localX, y + 1, minZ + localZ);

                if (level.ensureCanWrite(cursor) && level.getBlockState(cursor).isAir()
                        && level.getFluidState(cursor).isEmpty()
                        && level.getBlockEntity(cursor) == null) {
                    chunk.setBlockState(cursor, slab, false);
                    changed = true;
                }
            }
        }

        return changed;
    }

    private static int grassSurfaceY(WorldGenLevel level, BlockPos.MutableBlockPos cursor,
            int x, int z) {
        int height = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);

        cursor.set(x, height, z);

        if (level.getBlockState(cursor).is(Blocks.GRASS_BLOCK)) {
            return height;
        }

        cursor.setY(height - 1);

        return level.getBlockState(cursor).is(Blocks.GRASS_BLOCK)
                ? height - 1 : Integer.MIN_VALUE;
    }

    private static boolean supportedOnAllSides(WorldGenLevel level,
            BlockPos.MutableBlockPos cursor, int x, int y, int z) {
        return solid(level, cursor.set(x - 1, y, z))
                && solid(level, cursor.set(x + 1, y, z))
                && solid(level, cursor.set(x, y, z - 1))
                && solid(level, cursor.set(x, y, z + 1));
    }

    private static boolean solid(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        return state.getFluidState().isEmpty()
                && state.isCollisionShapeFullBlock(level, pos);
    }

    private static int haloIndex(int x, int z) {
        return x * HALO_WIDTH + z;
    }

    private static boolean get(long[] bits, int index) {
        return (bits[index >>> 6] & 1L << (index & 63)) != 0L;
    }

    private static void set(long[] bits, int index) {
        bits[index >>> 6] |= 1L << (index & 63);
    }
}
