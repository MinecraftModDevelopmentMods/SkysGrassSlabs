package zone.moddev.mc.skysgrassslabs.event;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import zone.moddev.mc.skysgrassslabs.entity.ai.TurfEatingAI;
import zone.moddev.mc.skysgrassslabs.world.GrassSlabSmoothingFeature;

public final class CommonEvents {
    private static final Set<Sheep> TURF_TASK_SHEEP =
            Collections.newSetFromMap(new WeakHashMap<Sheep, Boolean>());

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(CommonEvents::addTurfEatingTask);
        MinecraftForge.EVENT_BUS.addListener(GrassSlabSmoothingFeature::onBiomeLoading);
    }

    public static void addTurfEatingTask(EntityJoinWorldEvent event) {
        if (event.getWorld().isClientSide || !(event.getEntity() instanceof Sheep)) return;
        Sheep sheep = (Sheep) event.getEntity();
        if (TURF_TASK_SHEEP.add(sheep)) {
            sheep.goalSelector.addGoal(5, new TurfEatingAI(sheep));
        }
    }

    private CommonEvents() {
    }
}
