package nl.yunaget.client;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.gizmos.*;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class NavigationRenderer {
    public static void register(){
        LevelRenderEvents.END_EXTRACTION.register(context->{
            var c=YunaGetConfig.get;var goal=NavigationManager.destination();
            if(!c.navigationLine||goal==null||TaskManager.paused)return;
            var collector=new SimpleGizmoCollector();
            try(var ignored=Gizmos.withCollector(collector)){
                Vector3f forward=new Vector3f(context.camera().forwardVector()).mul(0.7f);
                Vec3 start=context.camera().position().add(forward.x,forward.y,forward.z);
                Vec3 destination=Vec3.atCenterOf(goal);
                Gizmos.line(start,destination,0xFF53E7D0,2).setAlwaysOnTop();
                Gizmos.cuboid(goal,GizmoStyle.stroke(0xFF53E7D0,2)).setAlwaysOnTop();
                Vec3 previous=context.camera().position().add(0,-1.4,0);int count=0;
                for(var node:NavigationManager.route()){
                    Vec3 point=Vec3.atBottomCenterOf(node).add(0,0.1,0);
                    Gizmos.line(previous,point,0xCC53E7D0,2).setAlwaysOnTop();previous=point;
                    if(++count>=128)break;
                }
            }
            context.levelRenderer().addMainThreadGizmos(collector.drainGizmos());
        });
        HudElementRegistry.addLast(Identifier.parse("yunaget:status"),(g,delta)->{
            var mc=Minecraft.getInstance();if(!YunaGetConfig.get.statusHud||mc.player==null||mc.gui.screen()!=null)return;
            int w=Math.min(280,g.guiWidth()-16);if(w<100)return;
            g.fill(8,8,8+w,66,0xD916202D);g.fill(8,8,11,66,TaskManager.paused?0xFFFFAA55:0xFF53E7D0);
            g.text(mc.font,"YunaGet | "+(TaskManager.paused?"GEPAUZEERD":TaskManager.owner.isEmpty()?"GEREED":TaskManager.owner),18,15,0xFFFFFFFF,false);
            var goal=NavigationManager.destination();
            String location=goal==null?"Geen AutoWalk-doel":goal.toShortString()+" | "+(int)Math.sqrt(mc.player.position().distanceToSqr(Vec3.atCenterOf(goal)))+" m";
            g.text(mc.font,mc.font.plainSubstrByWidth(location,w-20),18,29,0xFF53E7D0,false);
            g.text(mc.font,mc.font.plainSubstrByWidth(TaskManager.status,w-20),18,43,0xFFCDD9E5,false);
            g.text(mc.font,"Selectie "+YunaGetConfig.get.selectedItems.size()+" | Inventory "+InventoryTracker.used+"/36",18,55,0xFF99ADBE,false);
        });
    }
}
