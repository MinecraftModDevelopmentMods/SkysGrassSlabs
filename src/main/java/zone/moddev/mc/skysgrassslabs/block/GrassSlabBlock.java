package zone.moddev.mc.skysgrassslabs.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class GrassSlabBlock extends LegacySlabBlock implements IGrowable {
    public GrassSlabBlock() {
        super(Material.GRASS);
        setDefaultState(blockState.getBaseState()
                .withProperty(HALF, EnumBlockHalf.BOTTOM)
                .withProperty(BlockGrass.SNOWY, Boolean.FALSE));
        setSoundType(SoundType.PLANT);
        setTickRandomly(true);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty<?>[] {HALF, BlockGrass.SNOWY});
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(HALF,
                (meta & 1) == 1 ? EnumBlockHalf.BOTTOM : EnumBlockHalf.TOP);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.withProperty(BlockGrass.SNOWY,
                SnowySlabAppearance.hasNearbySnow(world, pos));
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        super.onBlockAdded(world, pos, state);
        dirtifyGrassSupport(world, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block changedBlock,
            BlockPos changedPos) {
        super.neighborChanged(state, world, pos, changedBlock, changedPos);
        dirtifyGrassSupport(world, pos);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (world.isRemote) {
            return;
        }
        dirtifyGrassSupport(world, pos);
        if (!GrassSpread.canRemainGrass(world, pos)) {
            world.setBlockState(pos, ModBlocks.DIRT_SLAB.getDefaultState()
                    .withProperty(HALF, state.getValue(HALF)), 3);
            return;
        }
        GrassSpread.spreadFrom(world, pos, random, pos.down());
    }

    @Override
    public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        List<ItemStack> drops = new ArrayList<ItemStack>();
        drops.add(new ItemStack(ModBlocks.DIRT_SLAB));
        return drops;
    }

    @Override
    protected boolean canSilkHarvest() {
        return true;
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(this);
    }

    @Nullable
    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return Item.getItemFromBlock(ModBlocks.DIRT_SLAB);
    }

    @Override
    public boolean canSustainPlant(IBlockState state, IBlockAccess world, BlockPos pos,
            EnumFacing direction, IPlantable plantable) {
        return direction == EnumFacing.UP && state.getValue(HALF) == EnumBlockHalf.TOP &&
                Blocks.GRASS.canSustainPlant(Blocks.GRASS.getDefaultState(), world, pos, direction, plantable);
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return state.getValue(HALF) == EnumBlockHalf.TOP;
    }

    @Override
    public boolean canUseBonemeal(World world, Random random, BlockPos pos, IBlockState state) {
        return state.getValue(HALF) == EnumBlockHalf.TOP;
    }

    @Override
    public void grow(World world, Random random, BlockPos pos, IBlockState state) {
        if (state.getValue(HALF) != EnumBlockHalf.TOP) {
            return;
        }
        BlockPos start = pos.up();
        for (int attempt = 0; attempt < 128; ++attempt) {
            BlockPos target = start;
            int walk = 0;
            while (true) {
                if (walk >= attempt / 16) {
                    if (world.isAirBlock(target)) {
                        if (random.nextInt(8) == 0) {
                            world.getBiome(target).plantFlower(world, random, target);
                        } else {
                            IBlockState grass = Blocks.TALLGRASS.getDefaultState()
                                    .withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS);
                            if (Blocks.TALLGRASS.canBlockStay(world, target, grass)) {
                                world.setBlockState(target, grass, 3);
                            }
                        }
                    }
                    break;
                }
                target = target.add(random.nextInt(3) - 1,
                        (random.nextInt(3) - 1) * random.nextInt(3) / 2,
                        random.nextInt(3) - 1);
                IBlockState support = world.getBlockState(target.down());
                boolean suitable = support.getBlock() == Blocks.GRASS ||
                        (support.getBlock() == this && support.getValue(HALF) == BlockSlab.EnumBlockHalf.TOP);
                if (!suitable || world.getBlockState(target).isNormalCube()) {
                    break;
                }
                ++walk;
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    private static void dirtifyGrassSupport(World world, BlockPos pos) {
        if (!world.isRemote && world.getBlockState(pos.down()).getBlock() == Blocks.GRASS) {
            world.setBlockState(pos.down(), Blocks.DIRT.getDefaultState(), 2);
        }
    }
}
