package nl.yunaget.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Bounded, incremental best-first ground routing. Never teleports, places or breaks blocks. */
public final class NavigationManager {
    private static BlockPos goal, origin;
    private static String owner="";
    private static Object world;
    private static final PriorityQueue<BlockPos> OPEN = new PriorityQueue<>(Comparator.comparingInt(p ->
        Math.abs(p.getX()-goal.getX())+Math.abs(p.getY()-goal.getY())+Math.abs(p.getZ()-goal.getZ())));
    private static final Map<BlockPos, BlockPos> PARENTS = new HashMap<>();
    private static final ArrayDeque<BlockPos> PATH = new ArrayDeque<>();
    private static int elapsed, stuck;
    private static Vec3 last;
    public static String failure = "";
    public static boolean active() { return goal != null; }
    public static String owner(){return owner;}
    public static BlockPos destination(){return goal;}
    public static List<BlockPos> route(){return List.copyOf(PATH);}
    public static void cancel() { goal = null; owner=""; OPEN.clear(); PARENTS.clear(); PATH.clear(); }
    public static void request(BlockPos destination) {
        request(destination,TaskManager.owner);
    }
    public static void request(BlockPos destination,String task) {
        var mc = Minecraft.getInstance();
        if (goal != null && goal.equals(destination)&&owner.equals(task)) return;
        cancel(); failure = ""; goal = destination.immutable(); origin = mc.player.blockPosition(); world = mc.level;
        owner=task;
        elapsed = stuck = 0; last = mc.player.position(); OPEN.add(origin); PARENTS.put(origin, origin);
    }
    private static boolean dangerous(BlockPos p) {
        var level = Minecraft.getInstance().level;
        for (BlockPos q : BlockPos.betweenClosed(p.offset(-1,-1,-1), p.offset(1,1,1))) {
            if (!level.hasChunkAt(q)) return true;
            var s = level.getBlockState(q);
            if (!s.getFluidState().isEmpty() || s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE) || s.is(Blocks.CACTUS)
                || s.is(Blocks.MAGMA_BLOCK) || s.is(Blocks.CAMPFIRE) || s.is(Blocks.SOUL_CAMPFIRE) || s.is(Blocks.POWDER_SNOW)
                || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.WITHER_ROSE)) return true;
        }
        return false;
    }
    private static boolean passable(BlockPos p) {
        var l = Minecraft.getInstance().level;
        return l.hasChunkAt(p) && l.getBlockState(p).getCollisionShape(l, p).isEmpty() && l.getFluidState(p).isEmpty();
    }
    public static boolean walkable(BlockPos p) {
        var l = Minecraft.getInstance().level;
        return passable(p) && passable(p.above()) && l.hasChunkAt(p.below())
            && l.getBlockState(p.below()).isCollisionShapeFullBlock(l, p.below())
            && (!YunaGetConfig.get.safeNavigation || !dangerous(p));
    }
    private static void fail(String text) { failure = text; cancel(); TaskManager.status = text; }
    /** Called after physical keyboard input has been read; returns a synthetic ordinary forward input only. */
    public static boolean move(net.minecraft.world.entity.player.Input physical) {
        if (!active()) return false;
        var mc = Minecraft.getInstance();
        if (physical.forward() || physical.backward() || physical.left() || physical.right() || physical.jump() || physical.shift()) {
            fail("AutoWalk onderbroken door handmatige beweging"); TaskManager.pause(); return false;
        }
        if (mc.level != world || mc.gui.screen() != null || TaskManager.paused || !mc.isWindowActive()) { cancel(); return false; }
        if (mc.player.isInLava() || mc.player.isOnFire() || mc.player.fallDistance > 2 || mc.player.getHealth() <= 6) {
            fail("AutoWalk gestopt: gevaar"); TaskManager.pause(); return false;
        }
        if (++elapsed > 600) { fail("AutoWalk timeout"); return false; }
        if (PATH.isEmpty() && !OPEN.isEmpty()) {
            for (int budget = 0; budget < 100 && !OPEN.isEmpty(); budget++) {
                BlockPos p = OPEN.remove();
                if (p.equals(goal)) {
                    for (BlockPos q = p; !q.equals(origin); q = PARENTS.get(q)) PATH.addFirst(q);
                    if(PATH.isEmpty())PATH.add(p);
                    OPEN.clear(); break;
                }
                for (int[] d : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                    for (int dy : new int[]{0,1,-1}) {
                        BlockPos next = p.offset(d[0],dy,d[1]);
                        if (PARENTS.containsKey(next) || next.distSqr(origin) > 48*48 || PARENTS.size() >= 2048) continue;
                        if (dy == 1 && !passable(p.above(2))) continue;
                        if (walkable(next)) { PARENTS.put(next,p); OPEN.add(next); break; }
                    }
                }
            }
            if (PATH.isEmpty() && OPEN.isEmpty()) { fail("Geen veilig pad gevonden"); return false; }
        }
        if (PATH.isEmpty()) {
            if (OPEN.isEmpty()) cancel();
            return false;
        }
        BlockPos next = PATH.peekFirst();
        Vec3 at = Vec3.atBottomCenterOf(next);
        if (mc.player.position().distanceToSqr(at) < 0.3) { PATH.removeFirst(); if (PATH.isEmpty()) cancel(); return false; }
        if (!walkable(next)) { fail("Pad geblokkeerd of gevaarlijk"); return false; }
        if (mc.player.position().distanceToSqr(last) < 0.001) stuck++; else stuck = 0;
        last = mc.player.position();
        if (stuck > 40) { fail("AutoWalk zit vast"); return false; }
        MiningTask.aim(at.add(0, mc.player.getEyeHeight(), 0));
        jump = next.getY() > mc.player.blockPosition().getY() && mc.player.onGround();
        return true;
    }
    public static boolean jump;
}
