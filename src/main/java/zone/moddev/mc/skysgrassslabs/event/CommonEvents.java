package zone.moddev.mc.skysgrassslabs.event;

import java.util.Set;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import zone.moddev.mc.skysgrassslabs.entity.ai.TurfEatingAI;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class CommonEvents {
    @SubscribeEvent
    public void addTurfEatingTask(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntitySheep)) {
            return;
        }
        EntitySheep sheep = (EntitySheep) event.getEntity();
        for (EntityAITasks.EntityAITaskEntry entry : sheep.tasks.taskEntries) {
            if (entry.action instanceof TurfEatingAI) {
                return;
            }
        }
        sheep.tasks.addTask(5, new TurfEatingAI(sheep));
    }

    @SubscribeEvent
    public void flattenSlab(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || event.getFace() == EnumFacing.DOWN || !isShovel(stack)) {
            return;
        }
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != ModBlocks.DIRT_SLAB && state.getBlock() != ModBlocks.GRASS_SLAB) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        if (!world.isAirBlock(pos.up()) || !player.canPlayerEdit(pos, event.getFace(), stack)) {
            return;
        }

        event.setCanceled(true);
        if (world.isRemote) {
            return;
        }

        IBlockState path = ModBlocks.PATH_SLAB.getDefaultState()
                .withProperty(BlockSlab.HALF, state.getValue(BlockSlab.HALF));
        if (world.setBlockState(pos, path, 11)) {
            world.playSound(null, pos, SoundEvents.ITEM_SHOVEL_FLATTEN,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
            if (!player.capabilities.isCreativeMode) {
                stack.damageItem(1, player);
            }
        }
    }

    private static boolean isShovel(ItemStack stack) {
        Set<String> toolClasses = stack.getItem().getToolClasses(stack);
        return toolClasses != null && toolClasses.contains("shovel");
    }
}
