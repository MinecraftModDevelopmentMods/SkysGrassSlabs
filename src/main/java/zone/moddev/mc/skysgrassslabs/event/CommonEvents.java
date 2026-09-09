package zone.moddev.mc.skysgrassslabs.event;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import zone.moddev.mc.skysgrassslabs.entity.ai.TurfEatingGoal;

/** Server gameplay event registrations. */
public final class CommonEvents {
    private static final Set<Sheep> TURF_GOAL_SHEEP =
            Collections.newSetFromMap(new WeakHashMap<>());

    public static void register() {
        EntityJoinLevelEvent.BUS.addListener(CommonEvents::addTurfEatingGoal);
    }

    public static void addTurfEatingGoal(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Sheep sheep)) {
            return;
        }
        if (TURF_GOAL_SHEEP.add(sheep)) {
            sheep.goalSelector.addGoal(5, new TurfEatingGoal(sheep));
        }
    }

    private CommonEvents() {
    }
}
