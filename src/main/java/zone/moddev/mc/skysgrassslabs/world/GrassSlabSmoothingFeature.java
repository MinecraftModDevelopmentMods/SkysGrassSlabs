package zone.moddev.mc.skysgrassslabs.world;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import zone.moddev.mc.skysgrassslabs.compat.LegacyWorldDataHook;
import zone.moddev.mc.skysgrassslabs.config.SkysGrassSlabsConfig;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Deterministic two-pass slope smoothing for newly generated Overworld chunks. */
public final class GrassSlabSmoothingFeature extends Feature<NoneFeatureConfiguration> {
    private static final int HALO_WIDTH = 18;
    private static final int MISSING = Integer.MIN_VALUE;
    private static final ThreadLocal<DecisionBuffer> BUFFERS =
            ThreadLocal.withInitial(DecisionBuffer::new);

    public GrassSlabSmoothingFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!SkysGrassSlabsConfig.isSmoothingActive()
                || level.getLevel().dimension() != Level.OVERWORLD) {
            return false;
        }
        ChunkAccess owner = level.getChunk(context.origin());
        ChunkPos ownerPos = owner.getPos();
        if (LegacyWorldDataHook.isLegacyChunk(ownerPos.x, ownerPos.z)) {
            return false;
        }

        DecisionBuffer buffer = BUFFERS.get();
        Arrays.fill(buffer.heights, MISSING);
        Arrays.fill(buffer.grass, false);
        Arrays.fill(buffer.candidates, false);
        BlockPos.MutableBlockPos cursor = buffer.cursor;

        for (int haloZ = 0; haloZ < HALO_WIDTH; ++haloZ) {
            for (int haloX = 0; haloX < HALO_WIDTH; ++haloX) {
                int localX = haloX - 1;
                int localZ = haloZ - 1;
                int chunkX = ownerPos.x + Math.floorDiv(localX, 16);
                int chunkZ = ownerPos.z + Math.floorDiv(localZ, 16);
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ);
                int x = Math.floorMod(localX, 16);
                int z = Math.floorMod(localZ, 16);
                int index = haloIndex(haloX, haloZ);
                int surfaceY = surfaceY(chunk, x, z, cursor);
                buffer.heights[index] = surfaceY;
                if (surfaceY != MISSING) {
                    cursor.set(chunk.getPos().getMinBlockX() + x, surfaceY,
                            chunk.getPos().getMinBlockZ() + z);
                    buffer.grass[index] = chunk.getBlockState(cursor).is(Blocks.GRASS_BLOCK);
                }
            }
        }

        int minX = ownerPos.getMinBlockX();
        int minZ = ownerPos.getMinBlockZ();
        for (int localZ = 0; localZ < 16; ++localZ) {
            for (int localX = 0; localX < 16; ++localX) {
                int center = haloIndex(localX + 1, localZ + 1);
                int y = buffer.heights[center];
                if (y == MISSING || y + 1 >= owner.getMaxBuildHeight()) {
                    continue;
                }
                cursor.set(minX + localX, y + 1, minZ + localZ);
                BlockState target = owner.getBlockState(cursor);
                boolean clear = target.isAir() && owner.getBlockEntityNbt(cursor) == null;
                boolean dry = target.getFluidState().isEmpty();
                cursor.setY(y);
                BlockState support = owner.getBlockState(cursor);
                boolean supported = support.isCollisionShapeFullBlock(level, cursor);
                int north = haloIndex(localX + 1, localZ);
                int south = haloIndex(localX + 1, localZ + 2);
                int west = haloIndex(localX, localZ + 1);
                int east = haloIndex(localX + 2, localZ + 1);
                buffer.candidates[localZ << 4 | localX] = SmoothingDecision.shouldPlace(
                        y, buffer.heights[north], buffer.heights[south],
                        buffer.heights[west], buffer.heights[east], buffer.grass[center],
                        buffer.grass[north], buffer.grass[south], buffer.grass[west],
                        buffer.grass[east], clear, dry, supported);
            }
        }

        BlockState slab = ModBlocks.GRASS_SLAB.get().defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.BOTTOM)
                .setValue(SlabBlock.WATERLOGGED, false)
                .setValue(SnowyDirtBlock.SNOWY, false);
        boolean changed = false;
        for (int index = 0; index < buffer.candidates.length; ++index) {
            if (!buffer.candidates[index]) {
                continue;
            }
            int localX = index & 15;
            int localZ = index >>> 4;
            int y = buffer.heights[haloIndex(localX + 1, localZ + 1)];
            cursor.set(minX + localX, y + 1, minZ + localZ);
            if (!level.ensureCanWrite(cursor) || !owner.getBlockState(cursor).isAir()
                    || !owner.getFluidState(cursor).isEmpty()
                    || owner.getBlockEntityNbt(cursor) != null) {
                continue;
            }
            owner.setBlockState(cursor, slab, false);
            cursor.setY(y);
            if (owner.getBlockState(cursor).is(Blocks.GRASS_BLOCK)) {
                owner.setBlockState(cursor, Blocks.DIRT.defaultBlockState(), false);
            }
            changed = true;
        }
        return changed;
    }

    private static int surfaceY(ChunkAccess chunk, int localX, int localZ,
            BlockPos.MutableBlockPos cursor) {
        int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, localZ);
        int minY = chunk.getMinBuildHeight();
        int x = chunk.getPos().getMinBlockX() + localX;
        int z = chunk.getPos().getMinBlockZ() + localZ;
        while (y >= minY && chunk.getBlockState(cursor.set(x, y, z)).isAir()) {
            --y;
        }
        return y >= minY ? y : MISSING;
    }

    private static int haloIndex(int x, int z) {
        return z * HALO_WIDTH + x;
    }

    private static final class DecisionBuffer {
        private final int[] heights = new int[HALO_WIDTH * HALO_WIDTH];
        private final boolean[] grass = new boolean[HALO_WIDTH * HALO_WIDTH];
        private final boolean[] candidates = new boolean[256];
        private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    }
}
