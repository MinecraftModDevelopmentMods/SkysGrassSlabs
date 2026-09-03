package zone.moddev.mc.skysgrassslabs.event;

import java.util.Set;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import zone.moddev.mc.skysgrassslabs.entity.ai.TurfEatingAI;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class CommonEvents {
    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(CommonEvents::addTurfEatingTask);
        MinecraftForge.EVENT_BUS.addListener(CommonEvents::flattenSlab);
    }

    public static void addTurfEatingTask(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntitySheep)) {
            return;
        }
        EntitySheep sheep = (EntitySheep) event.getEntity();
        for (EntityAITasks.EntityAITaskEntry entry : sheep.tasks.taskEntries) {
            if (entry.action instanceof TurfEatingAI) return;
        }
        sheep.tasks.addTask(5, new TurfEatingAI(sheep));
    }

    public static void flattenSlab(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || event.getFace() == null || event.getFace() == EnumFacing.DOWN ||
                !isShovel(stack)) {
            return;
        }
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = world.getBlockState(pos);
        if ((state.getBlock() != ModBlocks.DIRT_SLAB &&
                state.getBlock() != ModBlocks.GRASS_SLAB) ||
                state.get(BlockSlab.WATERLOGGED)) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        if (!world.isAirBlock(pos.up()) || !player.canPlayerEdit(pos, event.getFace(), stack)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        if (world.isRemote) return;

        IBlockState path = state.get(BlockSlab.TYPE) == SlabType.DOUBLE
                ? Blocks.GRASS_PATH.getDefaultState()
                : ModBlocks.PATH_SLAB.getDefaultState()
                        .with(BlockSlab.TYPE, state.get(BlockSlab.TYPE))
                        .with(BlockSlab.WATERLOGGED, Boolean.FALSE);
        if (world.setBlockState(pos, path, 11)) {
            world.playSound(null, pos, SoundEvents.ITEM_SHOVEL_FLATTEN,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
            if (!player.abilities.isCreativeMode) {
                stack.damageItem(1, player);
            }
        }
    }

    private static boolean isShovel(ItemStack stack) {
        Set<ToolType> types = stack.getItem().getToolTypes(stack);
        return types != null && types.contains(ToolType.SHOVEL);
    }

    private CommonEvents() {
    }
}
