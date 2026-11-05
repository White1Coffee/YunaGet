package nl.yunaget.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import nl.yunaget.client.core.TaskState;
import nl.yunaget.client.core.NearestTarget;
import java.util.*;

public final class DropCollectorManager {
    private static UUID target;
    private static int ticks, wait;
    private static final Map<UUID,Integer> REJECTED = new HashMap<>();
    public static void reset() { target=null; wait=0; REJECTED.clear(); NavigationManager.failure=""; }
    public static boolean tick(Minecraft mc) {
        ticks++;
        var c = YunaGetConfig.get;
        if (!c.collector) {
            if (target != null) NavigationManager.cancel(); target = null; return false;
        }
        if (ticks % 10 != 0) return target != null;
        REJECTED.entrySet().removeIf(e -> e.getValue() < ticks);
        List<ItemEntity> candidates=new ArrayList<>();
        for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity item) || !item.isAlive() || item.getItem().isEmpty() || REJECTED.containsKey(item.getUUID())) continue;
            String id = BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).toString();
            if (c.selectedDropsOnly && !InventoryTracker.needed(id)) continue;
            if (!InventoryTracker.roomFor(item.getItem())) continue;
            double distance = item.distanceToSqr(mc.player);
            if (distance <= c.collectorRadius*c.collectorRadius) candidates.add(item);
        }
        ItemEntity best=NearestTarget.choose(candidates,item->true,item->item.distanceToSqr(mc.player));
        if (best == null) { if (target != null) NavigationManager.cancel(); target = null; return false; }
        if (!best.getUUID().equals(target)) {
            target = best.getUUID(); wait = 0; NavigationManager.cancel(); NavigationManager.failure="";
        }
        if (++wait > 40 || !NavigationManager.failure.isEmpty()) {
            if (REJECTED.size() >= 128) REJECTED.clear(); REJECTED.put(target, ticks + 200);
            target = null; NavigationManager.cancel(); NavigationManager.failure = ""; return false;
        }
        TaskManager.set("Collector", TaskState.SEARCHING, "Drop zoeken");
        if (best.distanceToSqr(mc.player) > 1.5) {
            TaskManager.set("Collector", TaskState.NAVIGATING, "Naar " + best.getItem().getHoverName().getString());
            NavigationManager.request(best.blockPosition());
        } else {
            NavigationManager.cancel(); TaskManager.set("Collector", TaskState.INTERACTING, "Wachten op normaal oppakken");
        }
        return true;
    }
}
