package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;
import zone.moddev.mc.skysgrassslabs.item.ShovelSupport;

/** Orientation-aware slab flattening for the data-driven shovel contract. */
public final class SlabFlattening {
    private SlabFlattening() {
    }

    public static void handle(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!ShovelSupport.isShovel(stack) || event.getFace() == Direction.DOWN) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState current = level.getBlockState(pos);
        if ((!current.is(ModBlocks.DIRT_SLAB.get()) && !current.is(ModBlocks.GRASS_SLAB.get()))
                || !level.getBlockState(pos.above()).isAir()) {
            return;
        }

        var player = event.getEntity();
        if (!level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, event.getFace(), stack)
                || event.getHand() == InteractionHand.MAIN_HAND
                        && player.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS)
                        && !player.isSecondaryUseActive()) {
            return;
        }

        BlockState flattened = SlabTransitions.flatten(current);
        if (flattened == null) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, stack);
        }
        stack.hurtAndBreak(1, player, event.getHand().asEquipmentSlot());
        level.setBlock(pos, flattened, Block.UPDATE_ALL_IMMEDIATE);
        level.playSound(player, pos, SoundEvents.SHOVEL_FLATTEN.value(), SoundSource.BLOCKS,
                1.0F, 1.0F);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, flattened));
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
