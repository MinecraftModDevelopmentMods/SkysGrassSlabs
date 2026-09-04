package zone.moddev.mc.skysgrassslabs.block;

import java.util.List;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.SnowyDirtBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.DecoratedFeatureConfig;
import net.minecraft.world.gen.feature.FlowersFeature;
import net.minecraftforge.common.IPlantable;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class GrassSlabBlock extends LegacySlabBlock implements IGrowable {
    public GrassSlabBlock() {
        super(Material.ORGANIC, SoundType.PLANT, 0.6F, true);
        setDefaultState(getDefaultState().with(SnowyDirtBlock.SNOWY, Boolean.FALSE));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(SnowyDirtBlock.SNOWY);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.with(SnowyDirtBlock.SNOWY,
                SnowySlabAppearance.hasNearbySnow(context.getWorld(), context.getPos()));
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState,
            boolean isMoving) {
        super.onBlockAdded(state, world, pos, oldState, isMoving);
        dirtifyGrassSupport(world, pos);
    }

    @Override
    public BlockState updatePostPlacement(BlockState state, Direction facing,
            BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos) {
        BlockState updated = super.updatePostPlacement(state, facing, facingState, world, pos,
                facingPos).with(SnowyDirtBlock.SNOWY,
                        SnowySlabAppearance.hasNearbySnow(world, pos));
        if (world instanceof World) {
            dirtifyGrassSupport((World) world, pos);
        }
        return updated;
    }

    @Override
    public void tick(BlockState state, World world, BlockPos pos, Random random) {
        if (world.isRemote) {
            return;
        }
        dirtifyGrassSupport(world, pos);
        if (state.get(SlabBlock.WATERLOGGED) || !GrassSpread.canRemainGrass(world, pos)) {
            world.setBlockState(pos, ModBlocks.dirtStateLike(state), 3);
            return;
        }
        BlockState repaired = state.with(SnowyDirtBlock.SNOWY,
                SnowySlabAppearance.hasNearbySnow(world, pos));
        if (repaired != state) {
            world.setBlockState(pos, repaired, 2);
        }
        GrassSpread.spreadFrom(world, pos, random, pos.down());
    }

    @Override
    public boolean canSustainPlant(BlockState state, IBlockReader world, BlockPos pos,
            Direction direction, IPlantable plantable) {
        return direction == Direction.UP && state.get(SlabBlock.TYPE) == SlabType.TOP &&
                !state.get(SlabBlock.WATERLOGGED) && Blocks.GRASS_BLOCK.canSustainPlant(
                        Blocks.GRASS_BLOCK.getDefaultState(), world, pos, direction, plantable);
    }

    @Override
    public boolean canGrow(IBlockReader world, BlockPos pos, BlockState state,
            boolean isClient) {
        return state.get(SlabBlock.TYPE) == SlabType.TOP &&
                !state.get(SlabBlock.WATERLOGGED) && world.getBlockState(pos.up()).isAir();
    }

    @Override
    public boolean canUseBonemeal(World world, Random random, BlockPos pos, BlockState state) {
        return state.get(SlabBlock.TYPE) == SlabType.TOP && !state.get(SlabBlock.WATERLOGGED);
    }

    @Override
    public void grow(World world, Random random, BlockPos pos, BlockState state) {
        if (!canUseBonemeal(world, random, pos, state)) {
            return;
        }
        BlockPos start = pos.up();
        BlockState grassPlant = Blocks.GRASS.getDefaultState();
        for (int attempt = 0; attempt < 128; ++attempt) {
            BlockPos target = start;
            int walk = 0;
            while (true) {
                if (walk >= attempt / 16) {
                    BlockState targetState = world.getBlockState(target);
                    if (targetState.getBlock() == grassPlant.getBlock() &&
                            random.nextInt(10) == 0) {
                        ((IGrowable) grassPlant.getBlock()).grow(world, random, target, targetState);
                    }
                    if (!targetState.isAir()) {
                        break;
                    }
                    BlockState growth;
                    if (random.nextInt(8) == 0) {
                        List<ConfiguredFeature<?>> flowers = world.getBiome(target).getFlowers();
                        if (flowers.isEmpty()) {
                            break;
                        }
                        ConfiguredFeature<?> flower = flowers.get(0);
                        growth = ((FlowersFeature) ((DecoratedFeatureConfig) flower.config)
                                .feature.feature).getRandomFlower(random, target);
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
                BlockState support = world.getBlockState(target.down());
                boolean suitable = support.getBlock() == Blocks.GRASS_BLOCK ||
                        support.getBlock() == this &&
                                support.get(SlabBlock.TYPE) == SlabType.TOP;
                if (!suitable || world.getBlockState(target).func_224756_o(world, target)) {
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
