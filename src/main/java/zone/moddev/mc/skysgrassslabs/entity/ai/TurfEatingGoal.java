package zone.moddev.mc.skysgrassslabs.entity.ai;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

/** Vanilla-shaped sheep grazing behaviour limited to turf. */
public final class TurfEatingGoal extends Goal {
    private final Sheep sheep;
    private final Level level;
    private int eatingTimer;

    public TurfEatingGoal(Sheep sheep) {
        this.sheep = sheep;
        level = sheep.level();
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return sheep.getRandom().nextInt(sheep.isBaby() ? 50 : 1000) == 0
                && level.getBlockState(position()).is(ModBlocks.TURF.get());
    }

    @Override
    public void start() {
        eatingTimer = 40;
        level.broadcastEntityEvent(sheep, (byte) 10);
        sheep.getNavigation().stop();
    }

    @Override
    public void stop() {
        eatingTimer = 0;
    }

    @Override
    public boolean canContinueToUse() {
        return eatingTimer > 0;
    }

    public int getEatingTimer() {
        return eatingTimer;
    }

    @Override
    public void tick() {
        eatingTimer = Math.max(0, eatingTimer - 1);
        if (eatingTimer != 4) {
            return;
        }
        BlockPos pos = position();
        if (!level.getBlockState(pos).is(ModBlocks.TURF.get())) {
            return;
        }
        if (level instanceof ServerLevel serverLevel
                && EventHooks.canEntityGrief(serverLevel, sheep)) {
            level.destroyBlock(pos, false);
        }
        sheep.ate();
    }

    private BlockPos position() {
        return sheep.blockPosition();
    }
}
