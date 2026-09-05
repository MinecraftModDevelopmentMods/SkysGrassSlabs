package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Material;
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

    protected LegacySlabBlock(Material material, SoundType sound, float hardness,
            boolean randomTicks) {
        this(material, sound, hardness, randomTicks, true);
    }

    protected LegacySlabBlock(Material material, SoundType sound, float hardness,
            boolean randomTicks, boolean flattenable) {
        super(properties(material, sound, hardness, randomTicks));
        this.flattenable = flattenable;
    }

    private static BlockBehaviour.Properties properties(Material material, SoundType sound,
            float hardness, boolean randomTicks) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of(material)
                .strength(hardness).sound(sound).noOcclusion();
        return randomTicks ? properties.randomTicks() : properties;
    }

    @Override
    public BlockState getToolModifiedState(BlockState state, Level level, BlockPos pos,
            Player player, ItemStack stack, ToolAction action) {
        if (!flattenable || action != ToolActions.SHOVEL_FLATTEN ||
                !stack.canPerformAction(action) || state.getValue(WATERLOGGED)) {
            return super.getToolModifiedState(state, level, pos, player, stack, action);
        }
        return state.getValue(TYPE) == SlabType.DOUBLE
                ? Blocks.DIRT_PATH.defaultBlockState()
                : ModBlocks.PATH_SLAB.defaultBlockState()
                        .setValue(TYPE, state.getValue(TYPE))
                        .setValue(WATERLOGGED, Boolean.FALSE);
    }
}
