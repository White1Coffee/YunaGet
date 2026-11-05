package nl.yunaget.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.*;

/** Bounded incremental scan. Cached blocks are revalidated before interaction and rendering. */
public final class WorldScanner {
    public static final Map<BlockPos, String> ores = new LinkedHashMap<>();
    public static final Set<BlockPos> mining = new LinkedHashSet<>();
    private static BlockPos origin;
    private static int cursor, radius;
    private static Object level;
    private static String signature = "";
    public static void invalidate(BlockPos p) { ores.remove(p); mining.remove(p); }
    public static void reset() { origin = null; ores.clear(); mining.clear(); cursor = 0; }
    public static void tick(Minecraft mc) {
        if (mc.level != level) { reset(); level = mc.level; }
        if (mc.player == null || mc.level == null) return;
        var c = YunaGetConfig.get;
        boolean needItems=(MiningTask.active()||c.targetXray)&&!c.selectedItems.isEmpty();
        if (!c.oreEsp && !needItems) { reset(); return; }
        int requested = Math.max(c.oreEsp ? c.scanRadius : 0, needItems ? c.searchRadius : 0);
        String current = c.selectedItems.toString() + c.oreColors.toString() + c.oreEsp;
        if (origin == null || origin.distSqr(mc.player.blockPosition()) > 16 || radius != requested || !signature.equals(current)) {
            reset(); origin = mc.player.blockPosition(); radius = requested; signature = current;
        }
        ores.entrySet().removeIf(e -> !mc.level.hasChunkAt(e.getKey()) || e.getKey().distSqr(mc.player.blockPosition()) > radius * radius
            || !BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(e.getKey()).getBlock()).toString().equals(e.getValue()));
        mining.removeIf(p -> !mc.level.hasChunkAt(p) || !TargetManager.matches(mc.level.getBlockState(p)) || p.distSqr(mc.player.blockPosition()) > radius * radius);
        int edge = radius * 2 + 1, total = edge * edge * edge;
        for (int n = 0; n < 1024; n++) {
            int at = cursor++; if (cursor >= total) cursor = 0;
            BlockPos p = origin.offset(offset(at%edge),offset(at/(edge*edge)),offset((at/edge)%edge));
            if (p.distSqr(mc.player.blockPosition()) > radius * radius || !mc.level.hasChunkAt(p)) continue;
            var state = mc.level.getBlockState(p);
            if (needItems && mining.size() < 2048 && TargetManager.matches(state)&&state.getDestroySpeed(mc.level,p)>=0) mining.add(p);
            String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            if (c.oreEsp && ores.size() < 2048 && c.oreColors.containsKey(id)) ores.put(p, id);
        }
    }
    private static int offset(int index){return index==0?0:(index%2==1?(index+1)/2:-(index/2));}
}
