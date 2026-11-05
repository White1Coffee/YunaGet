package nl.yunaget.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import nl.yunaget.client.navigation.*;
import org.slf4j.LoggerFactory;
import java.util.*;

/** Single owner, single route, single source of synthetic input and route-related mining. */
public final class NavigationManager {
    private enum Kind { POSITION, BLOCK, DROP, CHEST }
    private record Request(NavigationOwner owner,String key,String label,Vec3 target,Kind kind,boolean tunnel) { }
    private record Update(long revision,BlockState state) { }
    public record Motion(boolean override,float forward,float left,boolean jump) {
        static Motion none(){return new Motion(false,0,0,false);}
        static Motion hold(){return new Motion(true,0,0,false);}
    }
    private static final org.slf4j.Logger LOG=LoggerFactory.getLogger("yunaget.navigation");
    private static NavigationState state=NavigationState.IDLE;
    private static Request request;
    private static Object world;
    private static AStar<Surface,RouteStep> search;
    private static ClientTerrain terrain;
    private static final ArrayDeque<RouteStep> path=new ArrayDeque<>();
    private static final Set<Surface> blockedWaypoints=new HashSet<>();
    private static final Map<String,Long> rejectedTargets=new LinkedHashMap<>();
    private static final Map<BlockPos,Long> rejectedBlocks=new LinkedHashMap<>();
    private static final Map<BlockPos,Update> updates=new LinkedHashMap<>();
    private static List<Surface> goals=List.of();
    private static BlockPos goal;
    private static boolean tunnelPhase;
    private static boolean movementStarted;
    private static long ticks,revision,miningRevision;
    private static int repaths,removed,completed,total,stuckTicks,miningTicks,predictedAt=-1;
    private static double bestDistance=Double.POSITIVE_INFINITY,routeDistance;
    private static BlockPos mining;
    public static String failure="",status="Gereed";
    public static boolean active(){return state.active();}
    public static NavigationState state(){return state;}
    public static String owner(){return request==null?"":request.owner().display();}
    public static boolean owns(NavigationOwner owner){return request!=null&&request.owner()==owner;}
    public static boolean active(NavigationOwner owner){return active()&&owns(owner);}
    public static BlockPos destination(){return active()?goal:null;}
    public static String targetLabel(){return request==null?"geen":request.label();}
    public static Vec3 targetPosition(){return request==null?null:request.target();}
    public static RouteStep currentStep(){return path.peekFirst();}
    public static double routeDistance(){return routeDistance;}
    public static int obstaclesRemoved(){return removed;}
    public static String progress(){return completed+" / "+total;}
    public static List<BlockPos> route(){return path.stream().filter(s->!s.mining()).map(s->ClientTerrain.block(s.destination())).toList();}
    public static boolean controlsAttack(){return state==NavigationState.MINING_OBSTACLE||state==NavigationState.WAITING_FOR_BLOCK_UPDATE;}
    private static void log(String event){if(YunaGetConfig.get.debugNavigation)LOG.info("{} | {} | {} | {}",event,owner(),state,request==null?"":request.label());}
    private static void transition(NavigationState next,String text){
        if(!state.allows(next))throw new IllegalStateException("Invalid navigation transition: "+state+" -> "+next);
        boolean changed=state!=next;state=next;status=text;if(changed)log(text);
    }
    private static void releaseInput(){
        var player=Minecraft.getInstance().player;
        if(player!=null&&player.input instanceof nl.yunaget.client.mixin.ClientInputAccessor access){
            player.input.keyPresses=Input.EMPTY;access.yunaget$setMoveVector(Vec2.ZERO);
        }
    }
    private static void stopMining(){
        if(mining!=null&&Minecraft.getInstance().gameMode!=null)Minecraft.getInstance().gameMode.stopDestroyBlock();
        mining=null;miningTicks=0;predictedAt=-1;AutoToolManager.restore();
    }
    public static void cancel(){
        if(active())log("Navigation cancelled");stopMining();releaseInput();path.clear();search=null;goal=null;
        state=NavigationState.IDLE;status="Gestopt";
    }
    public static void cancel(NavigationOwner owner){if(owns(owner))cancel();}
    public static void preemptFor(NavigationOwner next){
        if(active()&&request.owner().priority<next.priority){log("Navigation preempted by "+next);cancel();}
    }
    public static void pause(){
        stopMining();releaseInput();search=null;
        transition(NavigationState.PAUSED,"Handmatig/gepauzeerd");
    }
    public static void resetWorld(){cancel();request=null;rejectedTargets.clear();rejectedBlocks.clear();updates.clear();blockedWaypoints.clear();failure="";world=Minecraft.getInstance().level;}
    public static boolean rejected(String key){return rejectedTargets.getOrDefault(key,0L)>ticks;}
    private static void fail(String reason){
        failure=reason;stopMining();releaseInput();path.clear();search=null;
        if(request!=null){if(rejectedTargets.size()>=256)rejectedTargets.remove(rejectedTargets.keySet().iterator().next());rejectedTargets.put(request.key(),ticks+600);}
        transition(NavigationState.FAILED,reason);log("No safe route: "+reason);
    }
    private static NavigationMode mode(NavigationOwner owner){
        var c=YunaGetConfig.get;
        return owner!=NavigationOwner.AUTO_SORTER&&c.tunnelMining&&c.navigationMode==NavigationMode.WALK_AND_MINE
            &&(owner!=NavigationOwner.DROP_COLLECTOR||c.tunnelForDrops)?NavigationMode.WALK_AND_MINE:NavigationMode.WALK_ONLY;
    }
    public static boolean requestBlock(BlockPos target){
        return submit(new Request(NavigationOwner.MINING,"block:"+target.asLong(),"Block "+target.toShortString(),Vec3.atCenterOf(target),Kind.BLOCK,mode(NavigationOwner.MINING)==NavigationMode.WALK_AND_MINE));
    }
    public static boolean requestDrop(UUID id,Vec3 position,String name){
        return submit(new Request(NavigationOwner.DROP_COLLECTOR,"drop:"+id,name,position,Kind.DROP,mode(NavigationOwner.DROP_COLLECTOR)==NavigationMode.WALK_AND_MINE));
    }
    public static boolean requestChest(BlockPos position){
        return submit(new Request(NavigationOwner.AUTO_SORTER,"chest:"+position.asLong(),"Chest "+position.toShortString(),Vec3.atCenterOf(position),Kind.CHEST,false));
    }
    public static void request(BlockPos destination){request(destination,TaskManager.owner);}
    public static void request(BlockPos destination,String owner){
        var id=NavigationOwner.legacy(owner);
        submit(new Request(id,"position:"+destination.asLong(),destination.toShortString(),Vec3.atBottomCenterOf(destination),Kind.POSITION,false));
    }
    private static boolean submit(Request next){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||TaskManager.paused)return false;
        if(rejected(next.key()))return false;
        if(request!=null&&active()){
            if(request.owner()!=next.owner()&&request.owner().priority>=next.owner().priority)return false;
            if(request.owner()==next.owner()&&request.key().equals(next.key())&&request.target().distanceToSqr(next.target())<.25)return true;
        }else if(request!=null&&state==NavigationState.ARRIVED&&request.key().equals(next.key())&&request.target().distanceToSqr(next.target())<.25)return true;
        cancel();request=next;world=mc.level;repaths=removed=completed=total=0;blockedWaypoints.clear();failure="";
        log("Target selected");beginPlan(false);return true;
    }
    private static void beginPlan(boolean tunnel){
        stopMining();releaseInput();path.clear();search=null;tunnelPhase=tunnel;stuckTicks=0;movementStarted=false;bestDistance=Double.POSITIVE_INFINITY;
        var mc=Minecraft.getInstance();var start=ClientTerrain.atPlayer(mc);var c=YunaGetConfig.get;
        terrain=new ClientTerrain(mc,start,tunnel,rejectedBlocks.keySet(),Math.max(0,c.maxTunnelBlocks-removed));
        goals=findGoals(terrain,tunnel);if(goals.isEmpty()){
            if(!tunnel&&request.tunnel()){beginPlan(true);return;}
            fail("Geen veilige stand-/pickup-positie");return;
        }
        goals=goals.stream().sorted(Comparator.comparingDouble(p->ClientTerrain.feet(p).distanceToSqr(mc.player.position()))).toList();
        goal=ClientTerrain.block(goals.getFirst());
        var targets=new HashSet<>(goals);
        search=new AStar<>(start,n->targets.contains(n.withoutMining()),n->terrain.neighbors(n).stream()
            .filter(e->!blockedWaypoints.contains(e.to().withoutMining())).toList(),
            n->goals.stream().mapToDouble(n::horizontalDistance).min().orElse(0),c.maxRouteNodes);
        transition(NavigationState.PLANNING,tunnel?"AutoTunnel-route berekenen":"Looproute berekenen");log("Route requested");
    }
    private static List<Surface> findGoals(ClientTerrain map,boolean tunnel){
        var mc=Minecraft.getInstance();var result=new LinkedHashSet<Surface>();
        BlockPos target=BlockPos.containing(request.target());
        if(request.kind()==Kind.POSITION){
            for(var p:map.surfaces(target.getX(),target.getZ(),request.target().y,false))if(Math.abs(p.y()-request.target().y)<1)result.add(p);
            return List.copyOf(result);
        }
        int radius=request.kind()==Kind.DROP?2:3;
        for(int x=target.getX()-radius;x<=target.getX()+radius;x++)for(int z=target.getZ()-radius;z<=target.getZ()+radius;z++) {
            for(var p:map.surfaces(x,z,target.getY()-1,tunnel)){
                if(request.kind()==Kind.DROP){
                    Vec3 at=ClientTerrain.feet(p);double dx=at.x-request.target().x,dz=at.z-request.target().z,dy=request.target().y-at.y;
                    if(dx*dx+dz*dz<=.85*.85&&dy>=-.2&&dy<=1.7)result.add(p.withoutMining());
                }else if(!(request.kind()==Kind.BLOCK&&p.x()==target.getX()&&p.z()==target.getZ()&&p.y()>target.getY())
                    &&map.canMineFrom(p,target,tunnel))result.add(p.withoutMining());
            }
        }
        return List.copyOf(result);
    }
    public static boolean walkable(BlockPos p){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return false;
        var t=new ClientTerrain(mc,ClientTerrain.atPlayer(mc),false,Set.of(),0);
        return t.surfaces(p.getX(),p.getZ(),p.getY(),false).stream().anyMatch(s->Math.abs(s.y()-p.getY())<=.55);
    }
    public static void recalculate(){
        if(request==null){status="Geen navigatietaak om te herberekenen";return;}
        if(Minecraft.getInstance().player==null)return;
        rejectedTargets.remove(request.key());TaskManager.paused=false;repaths=0;blockedWaypoints.clear();
        state=NavigationState.REPLANNING;log("Repath requested (manual)");beginPlan(false);
    }
    private static void repath(String reason,boolean blockWaypoint){
        if(!YunaGetConfig.get.repathOnStuck||repaths++>=YunaGetConfig.get.maxRepathAttempts){fail(reason+"; herberekenlimiet bereikt");return;}
        if(blockWaypoint&&path.peekFirst()!=null)blockedWaypoints.add(path.peekFirst().destination().withoutMining());
        failure=reason;transition(NavigationState.REPLANNING,reason);log("Repath requested");beginPlan(false);
    }
    public static void serverBlockUpdate(BlockPos position,BlockState block){
        if(updates.size()>=512)updates.remove(updates.keySet().iterator().next());
        updates.put(position.immutable(),new Update(++revision,block));
    }
    public static void tick(Minecraft mc){
        ticks++;rejectedTargets.entrySet().removeIf(e->e.getValue()<=ticks);rejectedBlocks.entrySet().removeIf(e->e.getValue()<=ticks);
        if(!active())return;
        if(mc.level!=world||mc.player==null){resetWorld();return;}
        if(TaskManager.paused||mc.gui.screen()!=null||!mc.isWindowActive()||mc.isPaused()){pause();return;}
        var c=YunaGetConfig.get;
        if(mc.player.getHealth()<c.minNavigationHealth||mc.player.isOnFire()||mc.player.isInLava()||mc.player.fallDistance>c.maxSafeFall+1){fail("Navigatie gestopt: gevaar/gezondheid");return;}
        if(search!=null){
            var outcome=search.step(c.routeBudget);
            if(outcome==AStar.Status.FOUND){
                path.addAll(search.actions());total=completed+path.size();routeDistance=0;
                Vec3 previous=mc.player.position();for(var planned:path)if(!planned.mining()){
                    Vec3 next=ClientTerrain.feet(planned.destination());routeDistance+=previous.distanceTo(next);previous=next;
                }
                search=null;
                if(routeDistance>c.maxRouteDistance){fail("Route langer dan maximale routeafstand");return;}
                if(path.isEmpty()){transition(NavigationState.ARRIVED,"Doel bereikt");log("Navigation completed");return;}
                goal=ClientTerrain.block(path.getLast().destination());transition(NavigationState.WALKING,"Route gevonden");log("Route found");
            }else if(outcome!=AStar.Status.SEARCHING){
                search=null;if(!tunnelPhase&&request.tunnel()){beginPlan(true);return;}
                fail(outcome==AStar.Status.LIMIT?"Zoeklimiet bereikt":"Geen veilig pad");return;
            }else return;
        }
        if(path.isEmpty()){stopMining();releaseInput();transition(NavigationState.ARRIVED,"Doel bereikt");log("Navigation completed");return;}
        RouteStep step=path.peekFirst();
        if(step.mining()){mineObstacle(mc,step);return;}
        if(!terrain.standing(step.destination())){log("Obstacle detected");repath("Pad veranderd/geblokkeerd",false);return;}
        if(state!=NavigationState.WALKING)transition(NavigationState.WALKING,"Lopen naar waypoint");
        double distance=mc.player.position().distanceToSqr(ClientTerrain.feet(step.destination()));
        if(distance+.015<bestDistance){bestDistance=distance;stuckTicks=0;}else if(++stuckTicks>60)repath("Geen voortgang bij waypoint",true);
    }
    private static void finishStep(){path.removeFirst();completed++;stuckTicks=0;bestDistance=Double.POSITIVE_INFINITY;log("Waypoint reached");}
    public static Motion input(Input physical){
        if(!active())return Motion.none();
        if(physical.forward()||physical.backward()||physical.left()||physical.right()||physical.jump()||physical.shift()){
            failure="Handmatige beweging neemt over";TaskManager.pause();return Motion.none();
        }
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level!=world||TaskManager.paused||mc.gui.screen()!=null||!mc.isWindowActive())return Motion.none();
        if(state!=NavigationState.WALKING||path.isEmpty()||path.peekFirst().mining())return Motion.hold();
        var step=path.peekFirst();
        if(Steering.arrived(mc.player.getX(),mc.player.getY(),mc.player.getZ(),step.destination(),mc.player.onGround())){
            finishStep();return Motion.hold();
        }
        Vec3 at=ClientTerrain.feet(step.destination());
        boolean jump=step.action()==RouteAction.JUMP&&mc.player.onGround()&&at.y-mc.player.getY()>.6;
        var command=Steering.towards(at.x-mc.player.getX(),at.z-mc.player.getZ(),mc.player.getYRot(),jump);
        mc.player.setYRot(command.yaw());mc.player.setXRot(Math.clamp(mc.player.getXRot(),-30,30));
        double horizontal=Math.hypot(at.x-mc.player.getX(),at.z-mc.player.getZ());
        float scale=(float)Math.min(1,horizontal/.35);
        if(!movementStarted&&(Math.abs(command.forward())+Math.abs(command.left()))*scale>.01){movementStarted=true;log("Movement started");}
        return new Motion(true,command.forward()*scale,command.left()*scale,command.jump());
    }
    private static void mineObstacle(Minecraft mc,RouteStep step){
        BlockPos p=step.obstacle();
        if(mining==null){
            if(!mc.player.onGround())return;
            if(!terrain.standing(ClientTerrain.atPlayer(mc))){fail("Geen veilige standplaats voor obstakel-mining");return;}
            if(mc.level.getBlockState(p).isAir()){finishStep();return;}
            if(removed>=YunaGetConfig.get.maxTunnelBlocks||!terrain.mineable(p)||p.equals(mc.player.blockPosition().below())
                ||mc.player.getBoundingBox().move(0,-.05,0).intersects(new AABB(p))){fail("Onveilig/verboden route-obstakel");return;}
            var hit=MiningTask.hit(p);if(hit==null){repath("Obstakel niet zichtbaar/binnen miningbereik",false);return;}
            if(!AutoToolManager.choose(mc.level.getBlockState(p))){fail("Geen passend gereedschap/durability");return;}
            mining=p;miningRevision=revision;miningTicks=0;predictedAt=-1;
            transition(NavigationState.MINING_OBSTACLE,"Route-obstakel minen "+p.toShortString());log("Mining started");
            MiningTask.aim(hit.getLocation());
            if(!mc.gameMode.startDestroyBlock(p,hit.getDirection())){denyObstacle("Vanilla blokkeert miningactie");return;}
        }
        miningTicks++;
        var update=updates.get(p);
        if(update!=null&&update.revision()>miningRevision&&update.state().isAir()&&mc.level.getBlockState(p).isAir()){
            removed++;log("Block update confirmed");stopMining();finishStep();transition(NavigationState.WALKING,"Obstakel door server bevestigd");return;
        }
        if(mc.level.getBlockState(p).isAir()){
            if(predictedAt<0)predictedAt=miningTicks;
            transition(NavigationState.WAITING_FOR_BLOCK_UPDATE,"Wachten op echte server-blockupdate");
        }else if(predictedAt>=0){
            if(miningTicks-predictedAt>20){denyObstacle("Server heeft block hersteld/geweigerd");return;}
        }else{
            var hit=MiningTask.hit(p);
            if(hit==null||!terrain.mineable(p)||!AutoToolManager.choose(mc.level.getBlockState(p))){denyObstacle("Miningveiligheid of bereik veranderd");return;}
            MiningTask.aim(hit.getLocation());mc.gameMode.continueDestroyBlock(p,hit.getDirection());
            mc.player.swing(InteractionHand.MAIN_HAND,mc.player.getMainHandItem().getAttackAnimation(),true);
        }
        if(miningTicks>600){denyObstacle("Geen bevestigde miningvoortgang; server weigert of timeout");}
    }
    private static void denyObstacle(String reason){
        if(mining!=null){if(rejectedBlocks.size()>=256)rejectedBlocks.remove(rejectedBlocks.keySet().iterator().next());rejectedBlocks.put(mining,ticks+1800);}
        // A refused interaction is never spammed or bypassed. A different safe route may still exist.
        repath(reason,false);
    }
}
