package zone.moddev.mc.skysgrassslabs.event;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import zone.moddev.mc.skysgrassslabs.entity.ai.TurfEatingAI;

public final class CommonEvents {
    private static final Set<Sheep> TURF_TASK_SHEEP =
            Collections.newSetFromMap(new WeakHashMap<Sheep, Boolean>());

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(CommonEvents::addTurfEatingTask);
    }

    public static void addTurfEatingTask(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Sheep)) return;
        Sheep sheep = (Sheep) event.getEntity();
        if (TURF_TASK_SHEEP.add(sheep)) {
            sheep.goalSelector.addGoal(5, new TurfEatingAI(sheep));
        }
    }

    private CommonEvents() {
    }
}
