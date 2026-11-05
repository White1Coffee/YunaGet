package nl.yunaget.client.navigation;

import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.*;
import nl.yunaget.client.*;
import java.util.*;

/** Collision-shape sampling of received client chunks. All methods run on the client thread. */
public final class ClientTerrain {
    private final Minecraft mc;
    private final Surface origin;
    private final boolean tunnel;
    private final Set<BlockPos> rejected;
    private final int mineBudget;
    private static final Set<String> STONE=Set.of("stone","deepslate","cobblestone","cobbled_deepslate","granite","diorite","andesite","tuff","calcite","netherrack","end_stone");
    public ClientTerrain(Minecraft mc,Surface origin,boolean tunnel,Set<BlockPos> rejected,int mineBudget) {
        this.mc=mc;this.origin=origin;this.tunnel=tunnel;this.rejected=Set.copyOf(rejected);this.mineBudget=mineBudget;
    }
    public static Surface atPlayer(Minecraft mc) {
        return new Surface((int)Math.floor(mc.player.getX()),(int)Math.round(mc.player.getY()*16),(int)Math.floor(mc.player.getZ()),0);
    }
    public static Vec3 feet(Surface p) { return new Vec3(p.x()+.5,p.y(),p.z()+.5); }
    public static BlockPos block(Surface p) { return BlockPos.containing(p.x()+.5,p.y()+.01,p.z()+.5); }
    private static AABB body(Surface p) { return new AABB(p.x()+.2,p.y()+.015,p.z()+.2,p.x()+.8,p.y()+1.79,p.z()+.8); }
    private boolean loaded(BlockPos p) { return !mc.level.isOutsideBuildHeight(p)&&mc.level.hasChunkAt(p); }
    public boolean hazardous(Surface node) {
        BlockPos p=block(node);
        int margin=YunaGetConfig.get.safeNavigation?1:0;
        for(BlockPos q:BlockPos.betweenClosed(p.offset(-margin,-1,-margin),p.offset(margin,2,margin))) {
            if(!loaded(q))return true;
            var state=mc.level.getBlockState(q);
            if(!state.getFluidState().isEmpty()||state.is(Blocks.FIRE)||state.is(Blocks.SOUL_FIRE)||state.is(Blocks.CACTUS)
                ||state.is(Blocks.MAGMA_BLOCK)||state.is(Blocks.CAMPFIRE)||state.is(Blocks.SOUL_CAMPFIRE)
                ||state.is(Blocks.POWDER_SNOW)||state.is(Blocks.SWEET_BERRY_BUSH)||state.is(Blocks.WITHER_ROSE))return true;
        }
        return false;
    }
    public boolean mineable(BlockPos p) {
        if(!loaded(p)||rejected.contains(p))return false;
        var state=mc.level.getBlockState(p);var id=BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if(state.hasBlockEntity()||mc.level.getBlockEntity(p)!=null||state.getDestroySpeed(mc.level,p)<0
            ||!id.getNamespace().equals("minecraft")||!STONE.contains(id.getPath())||AutoToolManager.evaluate(state)==null)return false;
        for(var direction:Direction.values()) {
            BlockPos q=p.relative(direction);if(!loaded(q))return false;
            if(!mc.level.getFluidState(q).isEmpty())return false;
        }
        return !(mc.level.getBlockState(p.above()).getBlock() instanceof FallingBlock);
    }
    private List<BlockPos> clearance(Surface node,boolean mining,Set<BlockPos> ignore) {
        AABB box=body(node);List<BlockPos> obstacles=new ArrayList<>();
        for(BlockPos p:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))) {
            if(!loaded(p))return null;
            if(rejected.contains(p))return null;
            if(ignore.contains(p))continue;
            var state=mc.level.getBlockState(p);
            if(!state.getFluidState().isEmpty())return null;
            boolean collision=state.getCollisionShape(mc.level,p).toAabbs().stream().anyMatch(b->b.move(p).intersects(box));
            if(!collision)continue;
            if(!mining||!mineable(p))return null;
            obstacles.add(p.immutable());
        }
        return obstacles;
    }
    public List<Surface> surfaces(int x,int z,double around,boolean mining) {
        List<Surface> result=new ArrayList<>();Set<Integer> heights=new HashSet<>();
        int high=(int)Math.floor(around)+1,low=(int)Math.floor(around)-Math.max(2,YunaGetConfig.get.maxSafeFall+1);
        for(int y=high;y>=low;y--) {
            BlockPos floor=new BlockPos(x,y,z);if(!loaded(floor))continue;
            var state=mc.level.getBlockState(floor);if(!state.getFluidState().isEmpty())continue;
            for(var box:state.getCollisionShape(mc.level,floor).toAabbs()) {
                if(box.minX>.5||box.maxX<.5||box.minZ>.5||box.maxZ<.5)continue;
                int height=(int)Math.round((y+box.maxY)*16);
                if(!heights.add(height))continue;
                Surface surface=new Surface(x,height,z,0);
                if(Math.abs(surface.y()-around)>3||hazardous(surface))continue;
                if(clearance(surface,mining,Set.of())!=null)result.add(surface);
            }
        }
        return result;
    }
    public boolean standing(Surface point) {
        if(hazardous(point)||clearance(point,false,Set.of())==null)return false;
        return surfaces(point.x(),point.z(),point.y(),false).stream().anyMatch(p->Math.abs(p.y()-point.y())<.03);
    }
    public List<AStar.Edge<Surface,RouteStep>> neighbors(Surface from) {
        var config=YunaGetConfig.get;List<AStar.Edge<Surface,RouteStep>> edges=new ArrayList<>();
        for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
            for(Surface base:surfaces(from.x()+d[0],from.z()+d[1],from.y(),tunnel)) {
                double rise=base.y()-from.y();
                if(rise>1.01||rise< -config.maxSafeFall-.01||rise>.6&&!config.allowJumping)continue;
                if(Math.hypot(base.x()-origin.x(),base.z()-origin.z())>config.maxRouteDistance)continue;
                var blocks=clearance(base,tunnel,Set.of());if(blocks==null)continue;
                List<BlockPos> obstacles=new ArrayList<>(blocks);
                if(rise>.6) {
                    // Jump arc needs extra headroom above the cell being left. Source body was already cleared by its edge.
                    Surface raised=new Surface(from.x(),base.height16(),from.z(),0);
                    Set<BlockPos> sourceBody=new HashSet<>();AABB old=body(from);
                    for(BlockPos p:BlockPos.betweenClosed(BlockPos.containing(old.minX,old.minY,old.minZ),BlockPos.containing(old.maxX,old.maxY,old.maxZ)))sourceBody.add(p.immutable());
                    var extra=clearance(raised,tunnel,sourceBody);if(extra==null)continue;
                    for(var p:extra)if(!obstacles.contains(p))obstacles.add(p);
                }
                if(from.mined()+obstacles.size()>mineBudget)continue;
                Surface to=new Surface(base.x(),base.height16(),base.z(),from.mined()+obstacles.size());
                double cost=RouteCosts.movement(rise);
                List<RouteStep> steps=new ArrayList<>();
                obstacles.sort(Comparator.comparingInt((BlockPos p)->p.getY()).reversed());
                for(var p:obstacles) {
                    RouteAction action=p.getY()<Math.floor(from.y())?RouteAction.MINE_FLOOR_OBSTACLE:
                        p.getY()>Math.floor(base.y())?RouteAction.MINE_HEADROOM:RouteAction.MINE_FORWARD;
                    steps.add(new RouteStep(action,to,p));cost+=RouteCosts.mining(AutoToolManager.evaluate(mc.level.getBlockState(p)).ticks(),config.preferExistingPaths);
                }
                steps.add(new RouteStep(rise>.6?RouteAction.JUMP:rise>.05?RouteAction.STEP_UP:rise< -.05?RouteAction.STEP_DOWN:RouteAction.WALK,to,null));
                edges.add(new AStar.Edge<>(to,cost,steps));
            }
        }
        return edges;
    }
    public boolean canMineFrom(Surface from,BlockPos target,boolean virtual) {
        Vec3 eye=feet(from).add(0,mc.player.getEyeHeight(),0),end=Vec3.atCenterOf(target);
        if(eye.distanceToSqr(end)>mc.player.blockInteractionRange()*mc.player.blockInteractionRange())return false;
        var clear=clearance(from,virtual,Set.of());if(clear==null)return false;
        Set<BlockPos> ignore=virtual?new HashSet<>(clear):Set.of();
        BlockGetter view=new BlockGetter() {
            public BlockEntity getBlockEntity(BlockPos p){return ignore.contains(p)?null:mc.level.getBlockEntity(p);}
            public BlockState getBlockState(BlockPos p){return ignore.contains(p)?Blocks.AIR.defaultBlockState():mc.level.getBlockState(p);}
            public FluidState getFluidState(BlockPos p){return getBlockState(p).getFluidState();}
            public int getHeight(){return mc.level.getHeight();}
            public int getMinY(){return mc.level.getMinY();}
        };
        var hit=view.clip(new ClipContext(eye,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,mc.player));
        return hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(target);
    }
}
