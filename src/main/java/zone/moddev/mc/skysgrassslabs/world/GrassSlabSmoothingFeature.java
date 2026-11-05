package zone.moddev.mc.skysgrassslabs.world;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi;
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
        if (GrassSlabsApi.hasSmoothingOwner() || !SkysGrassSlabsConfig.isSmoothingActive()
                || level.getLevel().dimension() != Level.OVERWORLD) {
            return false;
        }
        ChunkAccess owner = level instanceof WorldGenRegion region
                ? level.getChunk(region.getCenter().x, region.getCenter().z)
                : level.getChunk(context.origin());
        ChunkPos ownerPos = owner.getPos();
        if (LegacyWorldDataHook.isSavedOverworldChunk(ownerPos.x, ownerPos.z)) {
            return false;
        }
        StructureProtection structures = StructureProtection.read(owner, position ->
                level.hasChunk(position.x, position.z) ? level.getChunk(position.x, position.z) : null);
        if (!structures.canInspectSafely()) {
            return false;
        }

        DecisionBuffer buffer = BUFFERS.get();
        Arrays.fill(buffer.heights, MISSING);
        Arrays.fill(buffer.grass, false);
        Arrays.fill(buffer.candidates, false);
        Arrays.fill(buffer.originalTargets, null);
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
                    BlockState surface = chunk.getBlockState(cursor);
                    buffer.grass[index] = surface.is(Blocks.GRASS_BLOCK)
                            || surface.is(Blocks.DIRT) && isGrassCap(chunk.getBlockState(cursor.above()));
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
                if (structures.contains(cursor) || structures.contains(cursor.below())
                        || target.getBlock() instanceof DoublePlantBlock && structures.contains(cursor.above())) {
                    continue;
                }
                boolean clear = SmoothingFoliage.canReplace(level, owner, cursor);
                boolean dry = target.getFluidState().isEmpty();
                cursor.setY(y);
                BlockState support = owner.getBlockState(cursor);
                boolean supported = support.isCollisionShapeFullBlock(level, cursor);
                int north = haloIndex(localX + 1, localZ);
                int south = haloIndex(localX + 1, localZ + 2);
                int west = haloIndex(localX, localZ + 1);
                int east = haloIndex(localX + 2, localZ + 1);
                int index = localZ << 4 | localX;
                buffer.candidates[index] = SmoothingDecision.shouldPlace(
                        y, buffer.heights[north], buffer.heights[south],
                        buffer.heights[west], buffer.heights[east], buffer.grass[center],
                        buffer.grass[north], buffer.grass[south], buffer.grass[west],
                        buffer.grass[east], clear, dry, supported);
                if (buffer.candidates[index]) {
                    buffer.originalTargets[index] = target;
                }
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
            BlockState target = buffer.originalTargets[index];
            boolean tallPlant = target.getBlock() instanceof DoublePlantBlock;
            if (!level.ensureCanWrite(cursor) || !level.ensureCanWrite(cursor.below())
                    || tallPlant && !level.ensureCanWrite(cursor.above())
                    || owner.getBlockState(cursor) != target
                    || !SmoothingFoliage.canReplace(level, owner, cursor)) {
                continue;
            }
            owner.setBlockState(cursor, slab, false);
            if (owner.getBlockState(cursor) != slab) {
                continue;
            }
            if (tallPlant) {
                owner.setBlockState(cursor.above(), Blocks.AIR.defaultBlockState(), false);
            }
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
        // The generation map is not retained on every finished chunk at the border.
        // The live surface map counts the same blocks and is already up to date there.
        Heightmap.Types heightmap = Heightmap.Types.WORLD_SURFACE_WG;
        if (!chunk.hasPrimedHeightmap(heightmap)) {
            heightmap = Heightmap.Types.WORLD_SURFACE;
            if (!chunk.hasPrimedHeightmap(heightmap)) {
                return MISSING;
            }
        }
        int y = chunk.getHeight(heightmap, localX, localZ);
        int minY = chunk.getMinBuildHeight();
        int x = chunk.getPos().getMinBlockX() + localX;
        int z = chunk.getPos().getMinBlockZ() + localZ;
        while (y >= minY) {
            BlockState state = chunk.getBlockState(cursor.set(x, y, z));
            if (!state.isAir() && !SmoothingFoliage.isPlant(state)) {
                break;
            }
            --y;
        }
        if (y > minY && isGrassCap(chunk.getBlockState(cursor.set(x, y, z)))
                && chunk.getBlockState(cursor.below()).is(Blocks.DIRT)) {
            --y;
        }
        return y >= minY ? y : MISSING;
    }

    private static boolean isGrassCap(BlockState state) {
        return state.is(ModBlocks.GRASS_SLAB.get())
                && state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM
                && !state.getValue(SlabBlock.WATERLOGGED);
    }

    private static int haloIndex(int x, int z) {
        return z * HALO_WIDTH + x;
    }

    private static final class DecisionBuffer {
        private final int[] heights = new int[HALO_WIDTH * HALO_WIDTH];
        private final boolean[] grass = new boolean[HALO_WIDTH * HALO_WIDTH];
        private final boolean[] candidates = new boolean[256];
        private final BlockState[] originalTargets = new BlockState[256];
        private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    }
}
