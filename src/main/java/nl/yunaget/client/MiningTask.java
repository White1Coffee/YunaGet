package nl.yunaget.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import nl.yunaget.client.core.TaskState;
import java.util.*;

/** Legacy entry points remain available; all breaking uses the vanilla client game mode. */
public final class MiningTask {
    private static boolean enabled;
    private static Object world;
    private static BlockPos target;
    private static BlockPos travelTarget;
    public static BlockPos destination(){return target!=null?target:travelTarget;}
    private static int ticks, targetTicks;
    private static final Map<BlockPos, Integer> REJECTED = new HashMap<>();
    public static boolean active() { return enabled; }
    public static boolean controlsAttack() { return enabled && !TaskManager.paused && target != null; }
    public static void start(Identifier id, int amount) {
        if (id != null) YunaGetConfig.get.selectedItems.add(id.toString());
        start(); YunaGetConfig.save();
    }
    public static void start() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || YunaGetConfig.get.selectedItems.isEmpty()) { TaskManager.status = "Vink eerst een item aan"; return; }
        enabled = true; world = mc.level; TaskManager.paused = false; ticks = targetTicks = 0;
        REJECTED.clear(); TaskManager.set("Mining", TaskState.SEARCHING, "Blocks zoeken");
    }
    public static void suspend() {
        NavigationManager.cancel(nl.yunaget.client.navigation.NavigationOwner.MINING);
        travelTarget=null;
        pauseBreaking();
    }
    public static void pauseBreaking(){
        if (target != null && Minecraft.getInstance().gameMode != null) Minecraft.getInstance().gameMode.stopDestroyBlock();
        target = null; AutoToolManager.restore();
    }
    public static void stop(String reason) {
        suspend(); enabled = false; REJECTED.clear();
        if (!reason.isEmpty() && Minecraft.getInstance().player != null) Minecraft.getInstance().player.sendSystemMessage(Component.literal("[YunaGet] " + reason));
    }
    public static BlockHitResult hit(BlockPos p) {
        var mc = Minecraft.getInstance(); if (mc.player == null || mc.level == null) return null;
        Vec3 eye = mc.player.getEyePosition(), point = Vec3.atCenterOf(p);
        double range = mc.player.blockInteractionRange();
        if (eye.distanceToSqr(point) > range * range || !mc.level.hasChunkAt(p)) return null;
        var ray = mc.level.clip(new ClipContext(eye, point, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        return ray.getType() == HitResult.Type.BLOCK && ray.getBlockPos().equals(p) ? ray : null;
    }
    public static void aim(Vec3 p) {
        var player = Minecraft.getInstance().player; Vec3 d = p.subtract(player.getEyePosition());
        player.setYRot((float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90);
        player.setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x*d.x + d.z*d.z))));
    }
    public static void tick(Minecraft mc) {
        if (!enabled) return;
        if (mc.level != world) { stop(""); return; }
        var c = YunaGetConfig.get; ticks++;
        if(NavigationManager.controlsAttack())return;
        if(c.selectedItems.isEmpty()){stop("");TaskManager.set("Mining",TaskState.IDLE,"Geen items aangevinkt");return;}
        if (!InventoryTracker.hasTargetSpace()) {
            suspend(); TaskManager.set("Mining", TaskState.PAUSED, "Inventory vol; maak ruimte of sorteer"); return;
        }
        REJECTED.entrySet().removeIf(e -> e.getValue() < ticks);
        if (ticks % c.miningInterval != 0) return;
        if (target != null && (!TargetManager.matches(mc.level.getBlockState(target)) || hit(target) == null
            || target.equals(mc.player.blockPosition().below()))) {
            if(mc.level.getBlockState(target).isAir())reject(target);
            suspend();
        }
        if (target == null) {
            if (ticks % Math.max(5, c.miningInterval) != 0) return;
            List<BlockPos> choices = new ArrayList<>();
            int r = Math.min(6, c.searchRadius);
            for (BlockPos p : BlockPos.betweenClosed(mc.player.blockPosition().offset(-r,-r,-r), mc.player.blockPosition().offset(r,r,r))) {
                if (!mc.level.hasChunkAt(p) || p.distSqr(mc.player.blockPosition()) > c.searchRadius*c.searchRadius || REJECTED.containsKey(p)
                    || p.equals(mc.player.blockPosition().below())) continue;
                var state = mc.level.getBlockState(p);
                if (TargetManager.matches(state) && state.getDestroySpeed(mc.level, p) >= 0 && hit(p) != null) choices.add(p.immutable());
            }
            choices.sort(Comparator.comparingDouble(p -> mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(p))));
            for (BlockPos p : choices) {
                if (!AutoToolManager.choose(mc.level.getBlockState(p))) { reject(p); continue; }
                target = p; targetTicks = 0; break;
            }
            if (target == null) {
                navigateToKnown(mc);return;
            }
            if(NavigationManager.owner().equals("Mining"))NavigationManager.cancel();travelTarget=null;
            var ray = hit(target); aim(ray.getLocation());
            if (!mc.gameMode.startDestroyBlock(target, ray.getDirection())) { reject(target); suspend(); return; }
        } else {
            if (!AutoToolManager.choose(mc.level.getBlockState(target))) { reject(target); suspend(); return; }
            var ray = hit(target); aim(ray.getLocation()); mc.gameMode.continueDestroyBlock(target, ray.getDirection());
        }
        TaskManager.set("Mining", TaskState.INTERACTING, "Mijnen: " + target.toShortString());
        mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), true);
        if (++targetTicks * c.miningInterval >= 400) {
            reject(target); suspend(); TaskManager.set("Mining", TaskState.SEARCHING, "Block overgeslagen na timeout");
        }
    }
    private static void reject(BlockPos p) { if (REJECTED.size() >= 256) REJECTED.clear(); REJECTED.put(p.immutable(), ticks + 600); }
    private static void navigateToKnown(Minecraft mc){
        var owner=nl.yunaget.client.navigation.NavigationOwner.MINING;
        if(travelTarget!=null){
            if(!mc.level.hasChunkAt(travelTarget)||!TargetManager.matches(mc.level.getBlockState(travelTarget))){NavigationManager.cancel(owner);travelTarget=null;}
            else if(NavigationManager.owns(owner)){
                if(NavigationManager.state()==nl.yunaget.client.navigation.NavigationState.PAUSED){NavigationManager.requestBlock(travelTarget);return;}
                if(NavigationManager.active(owner)){TaskManager.set("Mining",TaskState.NAVIGATING,NavigationManager.status);return;}
                if(NavigationManager.state()==nl.yunaget.client.navigation.NavigationState.ARRIVED&&hit(travelTarget)!=null)return;
                reject(travelTarget);NavigationManager.cancel(owner);travelTarget=null;
            }else{return;}
        }
        var known=nl.yunaget.client.core.NearestTarget.choose(WorldScanner.mining,
            p->!REJECTED.containsKey(p)&&!NavigationManager.rejected("block:"+p.asLong())&&mc.level.hasChunkAt(p)
                &&TargetManager.matches(mc.level.getBlockState(p))&&p.distSqr(mc.player.blockPosition())<=YunaGetConfig.get.searchRadius*YunaGetConfig.get.searchRadius,
            p->p.distSqr(mc.player.blockPosition()));
        if(known==null){TaskManager.set("Mining",TaskState.SEARCHING,"Geen bereikbaar bekend item; afgewezen doelen krijgen cooldown");return;}
        if(AutoToolManager.evaluate(mc.level.getBlockState(known))==null){reject(known);TaskManager.status="Geen passend gereedschap voor "+describe(known);return;}
        // Detection is independent of current visibility. The controller searches all safe stand positions.
        // Actual breaking still requires the normal client raycast and normal interaction range.
        if(NavigationManager.requestBlock(known)){
            travelTarget=known;TaskManager.set("Mining",TaskState.NAVIGATING,"Route naar "+describe(known));
        }else if(NavigationManager.rejected("block:"+known.asLong()))reject(known);
    }
    private static String describe(BlockPos p){
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(Minecraft.getInstance().level.getBlockState(p).getBlock()).getPath().replace('_',' ')+" | "+p.toShortString();
    }
}
