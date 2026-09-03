package zone.moddev.mc.skysgrassslabs.world;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class GrassSlabSmoothingHandler {
    public static final String FEATURE_ID = "skysgrassslabs:grass_slab_smoothing";
    private static final ThreadLocal<boolean[]> DECISIONS =
            new ThreadLocal<boolean[]>() {
                @Override
                protected boolean[] initialValue() {
                    return new boolean[256];
                }
            };

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void beforeDecoration(DecorateBiomeEvent.Pre event) {
        World world = event.getWorld();
        if (world.isRemote || world.provider.getDimension() != 0) {
            return;
        }
        ChunkPos chunkPos = new ChunkPos(event.getPos());
        Chunk owner = loadedChunk(world, chunkPos.x, chunkPos.z);
        if (owner == null) {
            return;
        }

        boolean[] decisions = DECISIONS.get();
        for (int index = 0; index < decisions.length; ++index) {
            decisions[index] = false;
        }

        int startX = chunkPos.getXStart();
        int startZ = chunkPos.getZStart();
        for (int localZ = 0; localZ < 16; ++localZ) {
            for (int localX = 0; localX < 16; ++localX) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                int surfaceY = owner.getHeightValue(localX, localZ) - 1;
                if (surfaceY < 0 || surfaceY >= 255) {
                    continue;
                }
                BlockPos surface = new BlockPos(worldX, surfaceY, worldZ);
                BlockPos target = surface.up();
                IBlockState lower = owner.getBlockState(surface);
                IBlockState targetState = owner.getBlockState(target);
                boolean lowerGrass = lower.getBlock() == Blocks.GRASS;
                boolean clear = targetState.getBlock().isAir(targetState, world, target);
                boolean dry = targetState.getMaterial() != Material.WATER &&
                        targetState.getMaterial() != Material.LAVA;
                boolean supported = lower.isSideSolid(world, surface, EnumFacing.UP);
                boolean hasBlockEntity = owner.getTileEntity(target,
                        Chunk.EnumCreateEntityType.CHECK) != null;
                if (!lowerGrass || !clear || !dry || !supported || hasBlockEntity) {
                    continue;
                }

                if (hasHigherGrassNeighbour(world, chunkPos, localX, localZ, surfaceY)) {
                    decisions[(localZ << 4) | localX] = true;
                }
            }
        }

        boolean changed = false;
        for (int index = 0; index < decisions.length; ++index) {
            if (!decisions[index]) {
                continue;
            }
            int localX = index & 15;
            int localZ = index >>> 4;
            int surfaceY = owner.getHeightValue(localX, localZ) - 1;
            BlockPos target = new BlockPos(startX + localX, surfaceY + 1, startZ + localZ);
            setGenerationState(owner, target, ModBlocks.GRASS_SLAB.getDefaultState());
            BlockPos support = target.down();
            if (owner.getBlockState(support).getBlock() == Blocks.GRASS) {
                setGenerationState(owner, support, Blocks.DIRT.getDefaultState());
            }
            changed = true;
        }
        if (changed) {
            // Chunk#setBlockState performs cross-chunk lighting work in 1.12.
            // Rebuild the owning chunk once after the two-pass write instead.
            owner.generateSkylightMap();
            owner.markDirty();
        }
    }

    private static void setGenerationState(Chunk chunk, BlockPos pos, IBlockState state) {
        ExtendedBlockStorage[] storage = chunk.getBlockStorageArray();
        int sectionIndex = pos.getY() >> 4;
        ExtendedBlockStorage section = storage[sectionIndex];
        if (section == Chunk.NULL_BLOCK_STORAGE) {
            section = new ExtendedBlockStorage(sectionIndex << 4,
                    chunk.getWorld().provider.hasSkyLight());
            storage[sectionIndex] = section;
        }
        section.set(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, state);
    }

    private static boolean hasHigherGrassNeighbour(World world, ChunkPos owner,
            int localX, int localZ, int lowerSurfaceY) {
        if (localX > 0 && isHigherGrass(world, owner.x, owner.z,
                localX - 1, localZ, lowerSurfaceY)) {
            return true;
        }
        if (localZ > 0 && isHigherGrass(world, owner.x, owner.z,
                localX, localZ - 1, lowerSurfaceY)) {
            return true;
        }
        if (localX < 15) {
            if (isHigherGrass(world, owner.x, owner.z,
                    localX + 1, localZ, lowerSurfaceY)) {
                return true;
            }
        } else if (isHigherGrass(world, owner.x + 1, owner.z,
                0, localZ, lowerSurfaceY)) {
            return true;
        }
        if (localZ < 15) {
            return isHigherGrass(world, owner.x, owner.z,
                    localX, localZ + 1, lowerSurfaceY);
        }
        return isHigherGrass(world, owner.x, owner.z + 1,
                localX, 0, lowerSurfaceY);
    }

    private static boolean isHigherGrass(World world, int chunkX, int chunkZ,
            int localX, int localZ, int lowerSurfaceY) {
        Chunk chunk = loadedChunk(world, chunkX, chunkZ);
        if (chunk == null) {
            return false;
        }
        int neighbourSurfaceY = chunk.getHeightValue(localX, localZ) - 1;
        BlockPos neighbour = new BlockPos((chunkX << 4) + localX,
                neighbourSurfaceY, (chunkZ << 4) + localZ);
        boolean neighbourGrass = neighbourSurfaceY >= 0 &&
                chunk.getBlockState(neighbour).getBlock() == Blocks.GRASS;
        return SmoothingDecision.shouldPlace(lowerSurfaceY, neighbourSurfaceY,
                true, neighbourGrass, true, true, true, false);
    }

    private static Chunk loadedChunk(World world, int chunkX, int chunkZ) {
        if (!(world.getChunkProvider() instanceof ChunkProviderServer)) {
            return null;
        }
        return ((ChunkProviderServer) world.getChunkProvider()).getLoadedChunk(chunkX, chunkZ);
    }
}
