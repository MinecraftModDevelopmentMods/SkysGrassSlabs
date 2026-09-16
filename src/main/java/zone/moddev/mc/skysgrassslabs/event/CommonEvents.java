package zone.moddev.mc.skysgrassslabs.event;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import zone.moddev.mc.skysgrassslabs.block.SlabFlattening;
import zone.moddev.mc.skysgrassslabs.entity.ai.TurfEatingGoal;

/** Server gameplay event registrations. */
public final class CommonEvents {
    private static final Set<Sheep> TURF_GOAL_SHEEP =
            Collections.newSetFromMap(new WeakHashMap<>());

    public static void register() {
        NeoForge.EVENT_BUS.addListener(CommonEvents::addTurfEatingGoal);
        NeoForge.EVENT_BUS.addListener(CommonEvents::flattenSlab);
    }

    public static void addTurfEatingGoal(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Sheep sheep)) {
            return;
        }
        if (TURF_GOAL_SHEEP.add(sheep)) {
            sheep.getGoalSelector().addGoal(5, new TurfEatingGoal(sheep));
        }
    }

    private static void flattenSlab(PlayerInteractEvent.RightClickBlock event) {
        SlabFlattening.handle(event);
    }

    private CommonEvents() {
    }
}
