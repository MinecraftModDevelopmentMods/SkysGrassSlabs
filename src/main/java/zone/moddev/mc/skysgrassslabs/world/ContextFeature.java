package zone.moddev.mc.skysgrassslabs.world;

import java.util.Random;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.gen.IChunkGenSettings;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.IFeatureConfig;

abstract class ContextFeature<C extends IFeatureConfig> extends Feature<C> {
    @Override
    public final boolean func_212245_a(IWorld world,
            IChunkGenerator<? extends IChunkGenSettings> generator, Random random,
            BlockPos origin, C config) {
        return place(world, generator, random, origin, config);
    }

    abstract boolean place(IWorld world, IChunkGenerator<? extends IChunkGenSettings> generator,
            Random random, BlockPos origin, C config);
}
