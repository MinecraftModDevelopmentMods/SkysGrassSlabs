package zone.moddev.mc.skysgrassslabs.entity.ai;

import java.util.EnumSet;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class TurfEatingAI extends Goal {
    private final SheepEntity sheep;
    private final World world;
    private int eatingTimer;

    public TurfEatingAI(SheepEntity sheep) {
        this.sheep = sheep;
        world = sheep.world;
        setMutexFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean shouldExecute() {
        return sheep.getRNG().nextInt(sheep.isChild() ? 50 : 1000) == 0 &&
                world.getBlockState(position()).getBlock() == ModBlocks.TURF;
    }

    @Override
    public void startExecuting() {
        eatingTimer = 40;
        world.setEntityState(sheep, (byte) 10);
        sheep.getNavigator().clearPath();
    }

    @Override
    public void resetTask() {
        eatingTimer = 0;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return eatingTimer > 0;
    }

    public int getEatingTimer() {
        return eatingTimer;
    }

    @Override
    public void tick() {
        eatingTimer = Math.max(0, eatingTimer - 1);
        if (eatingTimer != 4) return;
        BlockPos pos = position();
        if (world.getBlockState(pos).getBlock() != ModBlocks.TURF) return;
        if (ForgeEventFactory.getMobGriefingEvent(world, sheep)) {
            world.destroyBlock(pos, false);
        }
        sheep.eatGrassBonus();
    }

    private BlockPos position() {
        return new BlockPos(sheep.posX, sheep.posY, sheep.posZ);
    }
}
