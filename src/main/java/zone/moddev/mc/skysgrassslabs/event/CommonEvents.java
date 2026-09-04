package zone.moddev.mc.skysgrassslabs.event;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import zone.moddev.mc.skysgrassslabs.entity.ai.TurfEatingAI;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.world.GrassSlabSmoothingFeature;

public final class CommonEvents {
    private static final Set<SheepEntity> TURF_TASK_SHEEP =
            Collections.newSetFromMap(new WeakHashMap<SheepEntity, Boolean>());

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(CommonEvents::addTurfEatingTask);
        MinecraftForge.EVENT_BUS.addListener(CommonEvents::flattenSlab);
        MinecraftForge.EVENT_BUS.addListener(GrassSlabSmoothingFeature::onBiomeLoading);
    }

    public static void addTurfEatingTask(EntityJoinWorldEvent event) {
        if (event.getWorld().isClientSide || !(event.getEntity() instanceof SheepEntity)) return;
        SheepEntity sheep = (SheepEntity) event.getEntity();
        if (TURF_TASK_SHEEP.add(sheep)) {
            sheep.goalSelector.addGoal(5, new TurfEatingAI(sheep));
        }
    }

    public static void flattenSlab(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || event.getFace() == null || event.getFace() == Direction.DOWN ||
                !isShovel(stack)) return;

        World world = event.getWorld();
        BlockPos pos = event.getPos();
        BlockState state = world.getBlockState(pos);
        if ((state.getBlock() != ModBlocks.DIRT_SLAB &&
                state.getBlock() != ModBlocks.GRASS_SLAB) ||
                state.getValue(SlabBlock.WATERLOGGED)) return;

        PlayerEntity player = event.getPlayer();
        if (!world.isEmptyBlock(pos.above()) || !player.mayUseItemAt(pos, event.getFace(), stack)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(ActionResultType.SUCCESS);
        if (world.isClientSide) return;

        BlockState path = state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE
                ? Blocks.GRASS_PATH.defaultBlockState()
                : ModBlocks.PATH_SLAB.defaultBlockState()
                         .setValue(SlabBlock.TYPE, state.getValue(SlabBlock.TYPE))
                         .setValue(SlabBlock.WATERLOGGED, Boolean.FALSE);
        if (world.setBlock(pos, path, 11)) {
            world.playSound(null, pos, SoundEvents.SHOVEL_FLATTEN,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
            if (!player.abilities.instabuild) {
                stack.hurtAndBreak(1, player,
                        entity -> entity.broadcastBreakEvent(event.getHand()));
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
