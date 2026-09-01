package zone.moddev.mc.skysgrassslabs.block;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Turf item with direct dirt-slab greening before ordinary carpet placement. */
public final class TurfBlockItem extends BlockItem {
    public TurfBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState current = level.getBlockState(pos);
        if (context.getClickedFace() != Direction.UP || !current.is(ModBlocks.DIRT_SLAB.get())) {
            return super.useOn(context);
        }
        if (current.getValue(SlabBlock.WATERLOGGED)) {
            return InteractionResult.FAIL;
        }

        BlockState grass = SlabTransitions.grassFor(current);
        if (grass.hasProperty(GrassSlabBlock.SNOWY)) {
            grass = grass.setValue(GrassSlabBlock.SNOWY,
                    level.getBlockState(pos.above()).is(BlockTags.SNOW));
        }
        if (!level.setBlock(pos, grass, 11)) {
            return InteractionResult.FAIL;
        }

        Player player = context.getPlayer();
        ItemStack held = context.getItemInHand();
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, held);
        }
        var sound = grass.getSoundType(level, pos, player);
        level.playSound(player, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(player, GameEvent.BLOCK_PLACE, pos);
        if (player == null || !player.getAbilities().instabuild) {
            held.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
