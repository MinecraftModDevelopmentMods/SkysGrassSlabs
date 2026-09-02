package zone.moddev.mc.skysgrassslabs.entity.ai;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zone.moddev.mc.skysgrassslabs.init.ModBlocks;

public final class TurfEatingAI extends EntityAIBase {
    private final EntitySheep sheep;
    private final World world;
    private int eatingTimer;

    public TurfEatingAI(EntitySheep sheep) {
        this.sheep = sheep;
        this.world = sheep.world;
        setMutexBits(7);
    }

    @Override
    public boolean shouldExecute() {
        if (sheep.getRNG().nextInt(sheep.isChild() ? 50 : 1000) != 0) {
            return false;
        }
        return world.getBlockState(position()).getBlock() == ModBlocks.TURF;
    }

    @Override
    public void startExecuting() {
        eatingTimer = 40;
        world.setEntityState(sheep, (byte) 10);
        sheep.getNavigator().clearPathEntity();
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
    public void updateTask() {
        eatingTimer = Math.max(0, eatingTimer - 1);
        if (eatingTimer != 4) {
            return;
        }

        BlockPos pos = position();
        if (world.getBlockState(pos).getBlock() != ModBlocks.TURF) {
            return;
        }
        if (world.getGameRules().getBoolean("mobGriefing")) {
            world.destroyBlock(pos, false);
        }
        sheep.eatGrassBonus();
    }

    private BlockPos position() {
        return new BlockPos(sheep.posX, sheep.posY, sheep.posZ);
    }
}
