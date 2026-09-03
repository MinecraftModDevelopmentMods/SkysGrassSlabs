package zone.moddev.mc.skysgrassslabs.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class TurfBlockItem extends ItemBlock {
    public TurfBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public EnumActionResult onItemUse(ItemUseContext context) {
        ItemStack stack = context.getItem();
        IBlockState state = context.getWorld().getBlockState(context.getPos());
        if (context.getFace() == EnumFacing.UP && state.getBlock() == ModBlocks.DIRT_SLAB) {
            EntityPlayer player = context.getPlayer();
            if (player == null || !player.canPlayerEdit(context.getPos(), context.getFace(), stack) ||
                    state.get(BlockSlab.WATERLOGGED)) {
                return EnumActionResult.FAIL;
            }
            IBlockState replacement = state.get(BlockSlab.TYPE) == SlabType.DOUBLE
                    ? Blocks.GRASS_BLOCK.getDefaultState() : ModBlocks.grassStateLike(state);
            if (context.getWorld().setBlockState(context.getPos(), replacement, 3)) {
                if (!player.abilities.isCreativeMode) {
                    stack.shrink(1);
                }
                return EnumActionResult.SUCCESS;
            }
            return EnumActionResult.FAIL;
        }
        return super.onItemUse(context);
    }
}
