package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Common native slab behaviour shared by the three permanent slabs. */
public abstract class LegacySlabBlock extends SlabBlock {
    private final boolean flattenable;

    protected LegacySlabBlock(BlockBehaviour.Properties properties) {
        super(properties.noOcclusion());
        flattenable = false;
    }

    protected LegacySlabBlock(MapColor mapColor, SoundType sound, float hardness,
            boolean randomTicks) {
        this(mapColor, sound, hardness, randomTicks, true);
    }

    protected LegacySlabBlock(MapColor mapColor, SoundType sound, float hardness,
            boolean randomTicks, boolean flattenable) {
        super(properties(mapColor, sound, hardness, randomTicks));
        this.flattenable = flattenable;
    }

    private static BlockBehaviour.Properties properties(MapColor mapColor, SoundType sound,
            float hardness, boolean randomTicks) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().mapColor(mapColor)
                .strength(hardness).sound(sound).noOcclusion();
        return randomTicks ? properties.randomTicks() : properties;
    }

    @Override
    public BlockState getToolModifiedState(BlockState state,
            net.minecraft.world.item.context.UseOnContext context, ToolAction action, boolean simulate) {
        if (!flattenable || action != ToolActions.SHOVEL_FLATTEN ||
                !context.getItemInHand().canPerformAction(action) || state.getValue(WATERLOGGED)) {
            return super.getToolModifiedState(state, context, action, simulate);
        }
        return state.getValue(TYPE) == SlabType.DOUBLE
                ? Blocks.DIRT_PATH.defaultBlockState()
                : ModBlocks.PATH_SLAB.defaultBlockState()
                        .setValue(TYPE, state.getValue(TYPE))
                        .setValue(WATERLOGGED, Boolean.FALSE);
    }
}
