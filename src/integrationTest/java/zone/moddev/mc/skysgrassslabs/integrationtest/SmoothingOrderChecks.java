package zone.moddev.mc.skysgrassslabs.integrationtest;

import java.lang.reflect.Proxy;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.server.level.ServerLevel;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.world.GrassSlabSmoothingFeature;

/** Exercises the production feature on native chunks with a fixed loaded halo. */
final class SmoothingOrderChecks {
    private static final int BASE = 1024;
    private static int surfaceBaseY = 80;
    private static final long SEED = 4815162342L;

    static GrassSlabSmoothingFeature feature() {
        return (GrassSlabSmoothingFeature) net.minecraftforge.registries.ForgeRegistries.FEATURES
                .getValue(new net.minecraft.resources.ResourceLocation(
                        "skysgrassslabs", "grass_slab_smoothing"));
    }

    static void run(ServerLevel level) throws Exception {
        try {
            verifyAtHeight(level, 80);
            verifyAtHeight(level, -40);
        } finally {
            surfaceBaseY = 80;
        }
    }

    private static void verifyAtHeight(ServerLevel level, int baseY) throws Exception {
        surfaceBaseY = baseY;
        Map<Long, ChunkAccess> forward = terrain(level);
        Map<Long, ChunkAccess> reverse = terrain(level);
        List<ChunkPos> owners = new ArrayList<>();
        for (int chunkZ = 1; chunkZ <= 9; chunkZ++) {
            for (int chunkX = 1; chunkX <= 9; chunkX++) {
                owners.add(new ChunkPos(BASE + chunkX, BASE + chunkZ));
            }
        }
        byte[] untouchedHalo = fingerprint(forward, true);
        smooth(level, forward, owners);
        Collections.reverse(owners);
        smooth(level, reverse, owners);
        require(java.util.Arrays.equals(fingerprint(forward, false), fingerprint(reverse, false)),
                "The 9 by 9 result must not depend on decoration order");
        require(java.util.Arrays.equals(untouchedHalo, fingerprint(forward, true)),
                "The production feature must write only to its owning chunks");
        int slabs = 0;
        for (ChunkAccess chunk : forward.values()) {
            for (int localZ = 0; localZ < 16; localZ++) {
                for (int localX = 0; localX < 16; localX++) {
                    int blockX = chunk.getPos().getMinBlockX() + localX;
                    int blockZ = chunk.getPos().getMinBlockZ() + localZ;
                    if (chunk.getBlockState(new BlockPos(blockX, height(blockX, blockZ) + 1, blockZ))
                            .getBlock() == ModBlocks.GRASS_SLAB) slabs++;
                }
            }
        }
        require(slabs > 0, "The order comparison must include actual smoothing writes");

        WorldGenLevel world = loadedRegion(level, forward);
        ChunkPos center = new ChunkPos(BASE + 5, BASE + 5);
        long elapsed = 0;
        for (int pass = 0; pass < 256; pass++) {
            reset(forward.get(center.toLong()));
            long start = System.nanoTime();
            SmoothingOrderChecks.feature().place(new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(java.util.Optional.empty(), world, null, RandomSource.create(SEED),
                    new BlockPos(center.getMinBlockX(), 0, center.getMinBlockZ()), NoneFeatureConfiguration.INSTANCE));
            elapsed += System.nanoTime() - start;
        }
        System.out.println("GRASS_ORDER_PASS chunks=81 orders=2 slabs=" + slabs
                + " owner_only=true surface_base_y=" + baseY);
        System.out.println("GRASS_BENCHMARK_PASS columns=256 passes=256 mean_ns=" + elapsed / 256);
    }

    private static Map<Long, ChunkAccess> terrain(ServerLevel level) {
        Map<Long, ChunkAccess> chunks = new LinkedHashMap<>();
        for (int chunkZ = 0; chunkZ < 11; chunkZ++) {
            for (int chunkX = 0; chunkX < 11; chunkX++) {
                ChunkPos position = new ChunkPos(BASE + chunkX, BASE + chunkZ);
                ChunkAccess chunk = new ProtoChunk(position, UpgradeData.EMPTY, level, level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME), null);
                reset(chunk);
                chunks.put(position.toLong(), chunk);
            }
        }
        return chunks;
    }

    private static void reset(ChunkAccess chunk) {
        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                int blockX = chunk.getPos().getMinBlockX() + localX;
                int blockZ = chunk.getPos().getMinBlockZ() + localZ;
                BlockPos surface = new BlockPos(blockX, height(blockX, blockZ), blockZ);
                chunk.setBlockState(surface.below(), Blocks.DIRT.defaultBlockState(), false);
                chunk.setBlockState(surface, Blocks.GRASS_BLOCK.defaultBlockState(), false);
                chunk.setBlockState(surface.above(), Blocks.AIR.defaultBlockState(), false);
            }
        }
        Heightmap.primeHeightmaps(chunk, java.util.EnumSet.of(Heightmap.Types.WORLD_SURFACE_WG));
    }

    private static int height(int blockX, int blockZ) {
        long patch = ((blockX - BASE * 16) / 7 * 341873128712L
                + (blockZ - BASE * 16) / 11 * 132897987541L) ^ SEED;
        return surfaceBaseY + (int) Math.floorMod(patch, 5L);
    }

    private static void smooth(ServerLevel level, Map<Long, ChunkAccess> chunks, List<ChunkPos> owners) {
        WorldGenLevel world = loadedRegion(level, chunks);
        for (ChunkPos owner : owners) {
            SmoothingOrderChecks.feature().place(new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(java.util.Optional.empty(), world, null, RandomSource.create(SEED),
                    new BlockPos(owner.getMinBlockX(), 0, owner.getMinBlockZ()), NoneFeatureConfiguration.INSTANCE));
        }
    }

    private static WorldGenLevel loadedRegion(ServerLevel level, Map<Long, ChunkAccess> chunks) {
        return (WorldGenLevel) Proxy.newProxyInstance(SmoothingOrderChecks.class.getClassLoader(),
                new Class<?>[] {WorldGenLevel.class}, (proxy, method, arguments) -> {
                    // Match signatures so the same probe works with development and packaged names.
                    if (method.getReturnType() == ServerLevel.class) return level;
                    if (method.getReturnType() == boolean.class && arguments.length == 2
                            && arguments[0] instanceof Integer && arguments[1] instanceof Integer) {
                        return chunks.containsKey(ChunkPos.asLong((int) arguments[0], (int) arguments[1]));
                    }
                    if (method.getReturnType() == boolean.class && arguments.length == 1
                            && arguments[0] instanceof BlockPos position) {
                        return chunks.containsKey(new ChunkPos(position).toLong());
                    }
                    if (ChunkAccess.class.isAssignableFrom(method.getReturnType())) {
                        long key = arguments[0] instanceof BlockPos position
                                ? new ChunkPos(position).toLong()
                                : ChunkPos.asLong((int) arguments[0], (int) arguments[1]);
                        ChunkAccess chunk = chunks.get(key);
                        require(chunk != null, "A halo read must never load an unavailable chunk");
                        return chunk;
                    }
                    if (method.getReturnType() == net.minecraft.world.level.block.state.BlockState.class) {
                        BlockPos position = (BlockPos) arguments[0];
                        return chunks.get(new ChunkPos(position).toLong()).getBlockState(position);
                    }
                    throw new AssertionError("Unexpected loaded-region access: " + method);
                });
    }

    private static byte[] fingerprint(Map<Long, ChunkAccess> chunks, boolean halo) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (ChunkAccess chunk : chunks.values()) {
            int chunkX = chunk.getPos().x - BASE;
            int chunkZ = chunk.getPos().z - BASE;
            boolean outside = chunkX == 0 || chunkX == 10 || chunkZ == 0 || chunkZ == 10;
            if (outside != halo) continue;
            for (int localZ = 0; localZ < 16; localZ++) {
                for (int localX = 0; localX < 16; localX++) {
                    int blockX = chunk.getPos().getMinBlockX() + localX;
                    int blockZ = chunk.getPos().getMinBlockZ() + localZ;
                    for (int offset = -1; offset <= 1; offset++) {
                        int state = Block.getId(chunk.getBlockState(new BlockPos(blockX, height(blockX, blockZ) + offset, blockZ)));
                        digest.update((byte) (state >>> 8));
                        digest.update((byte) state);
                    }
                }
            }
        }
        return digest.digest();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
