package nl.yunaget.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import nl.yunaget.client.core.*;
import nl.yunaget.client.navigation.*;
import java.util.*;

public final class DropCollectorManager {
    private static UUID target;
    private static String itemId="";
    private static int entityId,initialInventory,wait,age;
    private static long ticks,pickupRevision,startedRevision;
    private static final Map<UUID,Long> REJECTED=new LinkedHashMap<>();
    private static final Map<Integer,Long> PICKUPS=new LinkedHashMap<>();
    public static int confirmedPickups;
    public static void serverPickup(int itemEntity,int player,int amount){
        var mc=Minecraft.getInstance();if(mc.player==null||player!=mc.player.getId()||amount<=0)return;
        if(PICKUPS.size()>=128)PICKUPS.remove(PICKUPS.keySet().iterator().next());PICKUPS.put(itemEntity,++pickupRevision);
    }
    public static void reset(){
        NavigationManager.cancel(NavigationOwner.DROP_COLLECTOR);target=null;wait=age=0;REJECTED.clear();PICKUPS.clear();
    }
    private static void release(String message,boolean reject){
        if(target!=null&&reject){if(REJECTED.size()>=128)REJECTED.remove(REJECTED.keySet().iterator().next());REJECTED.put(target,ticks+400);}
        NavigationManager.cancel(NavigationOwner.DROP_COLLECTOR);target=null;wait=age=0;
        TaskManager.set("Collector",TaskState.SEARCHING,message);
    }
    public static boolean tick(Minecraft mc){
        ticks++;var c=YunaGetConfig.get;
        if(!c.collector){if(target!=null)release("Collector uit",false);return false;}
        REJECTED.entrySet().removeIf(e->e.getValue()<=ticks);
        List<ItemEntity> items=new ArrayList<>();ItemEntity current=null;
        for(var entity:mc.level.entitiesForRendering())if(entity instanceof ItemEntity drop){
            if(drop.getUUID().equals(target))current=drop;
            if(!drop.isAlive()||drop.getItem().isEmpty()||REJECTED.containsKey(drop.getUUID())||NavigationManager.rejected("drop:"+drop.getUUID()))continue;
            String id=BuiltInRegistries.ITEM.getKey(drop.getItem().getItem()).toString();
            if(c.selectedDropsOnly&&!InventoryTracker.needed(id)||!InventoryTracker.roomFor(drop.getItem()))continue;
            if(drop.distanceToSqr(mc.player)<=c.collectorRadius*c.collectorRadius)items.add(drop);
        }
        if(target!=null&&PICKUPS.getOrDefault(entityId,0L)>startedRevision&&InventoryTracker.count(itemId)>initialInventory){
            confirmedPickups++;release("Drop door server opgepakt en inventory bijgewerkt",false);return false;
        }
        if(target!=null&&(current==null||!current.isAlive())){
            // Removal alone is not proof of pickup: somebody else may have collected/despawned it.
            if(++wait<=30){NavigationManager.cancel(NavigationOwner.DROP_COLLECTOR);return true;}
            release("Drop verdwenen zonder bevestigde eigen pickup",false);return false;
        }
        if(target==null){
            if(ticks%10!=0)return false;
            current=NearestTarget.choose(items,i->true,i->i.distanceToSqr(mc.player));
            if(current==null)return false;
            target=current.getUUID();entityId=current.getId();itemId=BuiltInRegistries.ITEM.getKey(current.getItem().getItem()).toString();
            initialInventory=InventoryTracker.count(itemId);startedRevision=pickupRevision;wait=age=0;
        }
        if(current==null)return false;
        if(!InventoryTracker.roomFor(current.getItem())){release("Geen inventoryruimte voor drop",false);return false;}
        if(++age>2400){release("Drop timeout; volgende kandidaat",true);return false;}
        if(NavigationManager.owns(NavigationOwner.DROP_COLLECTOR)&&NavigationManager.state()==NavigationState.FAILED){
            release("Drop onbereikbaar: "+NavigationManager.failure,true);return false;
        }
        if(NavigationManager.state()==NavigationState.ARRIVED&&NavigationManager.owns(NavigationOwner.DROP_COLLECTOR)){
            if(++wait>120){release("Geen server-pickup op bestemming; volgende drop",true);return false;}
        }else wait=0;
        if(!NavigationManager.requestDrop(target,current.position(),current.getItem().getHoverName().getString())){
            if(NavigationManager.rejected("drop:"+target)){release("Geen bereikbaar pickup-punt",true);return false;}
            return false;
        }
        TaskManager.set("Collector",NavigationManager.state()==NavigationState.ARRIVED?TaskState.INTERACTING:TaskState.NAVIGATING,
            NavigationManager.state()==NavigationState.ARRIVED?"Wachten op bevestigde pickup":NavigationManager.status);
        return true;
    }
}
