package zone.moddev.mc.skysgrassslabs.block;

import java.util.List;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirtSnowy;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.CompositeFlowerFeature;
import net.minecraftforge.common.IPlantable;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class GrassSlabBlock extends LegacySlabBlock implements IGrowable {
    public GrassSlabBlock() {
        super(Material.GRASS, SoundType.PLANT, 0.6F, true);
        setDefaultState(getDefaultState().with(BlockDirtSnowy.SNOWY, Boolean.FALSE));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, IBlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(BlockDirtSnowy.SNOWY);
    }

    @Override
    public IBlockState getStateForPlacement(BlockItemUseContext context) {
        IBlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.with(BlockDirtSnowy.SNOWY,
                SnowySlabAppearance.hasNearbySnow(context.getWorld(), context.getPos()));
    }

    @Override
    public void onBlockAdded(IBlockState state, World world, BlockPos pos, IBlockState oldState) {
        super.onBlockAdded(state, world, pos, oldState);
        dirtifyGrassSupport(world, pos);
    }

    @Override
    public IBlockState updatePostPlacement(IBlockState state, EnumFacing facing,
            IBlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos) {
        IBlockState updated = super.updatePostPlacement(state, facing, facingState, world, pos,
                facingPos).with(BlockDirtSnowy.SNOWY,
                        SnowySlabAppearance.hasNearbySnow(world, pos));
        if (world instanceof World) {
            dirtifyGrassSupport((World) world, pos);
        }
        return updated;
    }

    @Override
    public void tick(IBlockState state, World world, BlockPos pos, Random random) {
        if (world.isRemote) {
            return;
        }
        dirtifyGrassSupport(world, pos);
        if (state.get(BlockSlab.WATERLOGGED) || !GrassSpread.canRemainGrass(world, pos)) {
            world.setBlockState(pos, ModBlocks.dirtStateLike(state), 3);
            return;
        }
        IBlockState repaired = state.with(BlockDirtSnowy.SNOWY,
                SnowySlabAppearance.hasNearbySnow(world, pos));
        if (repaired != state) {
            world.setBlockState(pos, repaired, 2);
        }
        GrassSpread.spreadFrom(world, pos, random, pos.down());
    }

    @Override
    public IItemProvider getItemDropped(IBlockState state, World world, BlockPos pos, int fortune) {
        return ModBlocks.DIRT_SLAB;
    }

    @Override
    protected boolean canSilkHarvest() {
        return true;
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(this);
    }

    @Override
    public boolean canSustainPlant(IBlockState state, IBlockReader world, BlockPos pos,
            EnumFacing direction, IPlantable plantable) {
        return direction == EnumFacing.UP && state.get(BlockSlab.TYPE) == SlabType.TOP &&
                !state.get(BlockSlab.WATERLOGGED) && Blocks.GRASS_BLOCK.canSustainPlant(
                        Blocks.GRASS_BLOCK.getDefaultState(), world, pos, direction, plantable);
    }

    @Override
    public boolean canGrow(IBlockReader world, BlockPos pos, IBlockState state,
            boolean isClient) {
        return state.get(BlockSlab.TYPE) == SlabType.TOP &&
                !state.get(BlockSlab.WATERLOGGED) && world.getBlockState(pos.up()).isAir();
    }

    @Override
    public boolean canUseBonemeal(World world, Random random, BlockPos pos, IBlockState state) {
        return state.get(BlockSlab.TYPE) == SlabType.TOP && !state.get(BlockSlab.WATERLOGGED);
    }

    @Override
    public void grow(World world, Random random, BlockPos pos, IBlockState state) {
        if (!canUseBonemeal(world, random, pos, state)) {
            return;
        }
        BlockPos start = pos.up();
        IBlockState grassPlant = Blocks.GRASS.getDefaultState();
        for (int attempt = 0; attempt < 128; ++attempt) {
            BlockPos target = start;
            int walk = 0;
            while (true) {
                if (walk >= attempt / 16) {
                    IBlockState targetState = world.getBlockState(target);
                    if (targetState.getBlock() == grassPlant.getBlock() && random.nextInt(10) == 0) {
                        ((IGrowable) grassPlant.getBlock()).grow(world, random, target, targetState);
                    }
                    if (!targetState.isAir()) {
                        break;
                    }
                    IBlockState growth;
                    if (random.nextInt(8) == 0) {
                        List<CompositeFlowerFeature<?>> flowers = world.getBiome(target).getFlowers();
                        if (flowers.isEmpty()) {
                            break;
                        }
                        growth = flowers.get(0).getRandomFlower(random, target);
                    } else {
                        growth = grassPlant;
                    }
                    if (growth.isValidPosition(world, target)) {
                        world.setBlockState(target, growth, 3);
                    }
                    break;
                }
                target = target.add(random.nextInt(3) - 1,
                        (random.nextInt(3) - 1) * random.nextInt(3) / 2,
                        random.nextInt(3) - 1);
                IBlockState support = world.getBlockState(target.down());
                boolean suitable = support.getBlock() == Blocks.GRASS_BLOCK ||
                        support.getBlock() == this &&
                                support.get(BlockSlab.TYPE) == SlabType.TOP;
                if (!suitable || world.getBlockState(target).isBlockNormalCube()) {
                    break;
                }
                ++walk;
            }
        }
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    private static void dirtifyGrassSupport(World world, BlockPos pos) {
        if (!world.isRemote && world.getBlockState(pos.down()).getBlock() == Blocks.GRASS_BLOCK) {
            world.setBlockState(pos.down(), Blocks.DIRT.getDefaultState(), 2);
        }
    }
}
