package nl.yunaget.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import nl.yunaget.client.core.StorageRule;
import java.util.*;

public final class ChestManager {
    public static boolean marking;
    public static StorageRule lastMarked;
    private static Object world;
    public static final Map<StorageRule,String> warnings = new IdentityHashMap<>();
    public static String scope() {
        var mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() != null) return "local:" + mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        if (mc.getCurrentServer() == null) return "";
        return "server:" + mc.getCurrentServer().ip.toLowerCase(Locale.ROOT) + "/" + YunaGetConfig.get.worldProfile;
    }
    public static String dimension() { return Minecraft.getInstance().level == null ? "" : Minecraft.getInstance().level.dimension().identifier().toString(); }
    public static BlockPos pos(StorageRule r) { return new BlockPos(r.x,r.y,r.z); }
    public static List<StorageRule> current() {
        String scope = scope(), dimension = dimension();
        return YunaGetConfig.get.chests.stream().filter(r -> r.scope.equals(scope) && r.dimension.equals(dimension)).toList();
    }
    public static BlockPos canonical(BlockPos p) {
        var l = Minecraft.getInstance().level; var s = l.getBlockState(p);
        if (s.getBlock() instanceof ChestBlock && s.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos other = ChestBlock.getConnectedBlockPos(p, s);
            if (other.compareTo(p) < 0) return other;
        }
        return p.immutable();
    }
    public static boolean markLookedAt() {
        if (Minecraft.getInstance().hitResult instanceof BlockHitResult hit) return mark(hit.getBlockPos());
        TaskManager.status = "Kijk naar een chest"; return false;
    }
    public static boolean mark(BlockPos p) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.hasChunkAt(p) || !(mc.level.getBlockState(p).getBlock() instanceof ChestBlock)) {
            TaskManager.status = "Alleen gewone single/double chests kunnen worden gemarkeerd"; return false;
        }
        if (scope().isEmpty()) { TaskManager.status = "Wereld onbekend"; return false; }
        p = canonical(p);
        for (StorageRule r : current()) if (pos(r).equals(p)) {
            AutoSorterManager.abort("Chest gedeselecteerd"); YunaGetConfig.get.chests.remove(r); warnings.remove(r);
            YunaGetConfig.save(); lastMarked = null; marking = false; TaskManager.status = "Chest gedeselecteerd: " + r.name; return true;
        }
        if (YunaGetConfig.get.chests.size() >= 256) { TaskManager.status = "Chestlimiet bereikt (256)"; return false; }
        StorageRule rule = new StorageRule(); rule.scope = scope(); rule.dimension = dimension();
        rule.x = p.getX(); rule.y = p.getY(); rule.z = p.getZ(); rule.name = "Chest " + rule.x + "," + rule.z;
        YunaGetConfig.get.chests.add(rule); YunaGetConfig.save(); lastMarked = rule; marking = false;
        TaskManager.status = "Chest gemarkeerd; geef naam en filters in Storage"; return true;
    }
    public static String availability(StorageRule r) {
        var mc = Minecraft.getInstance(); BlockPos p = pos(r);
        if (mc.level == null || !mc.level.hasChunkAt(p)) return "niet geladen";
        if (!(mc.level.getBlockState(p).getBlock() instanceof ChestBlock)) return "chest ontbreekt";
        return warnings.getOrDefault(r, r.enabled ? "beschikbaar (toegang nog niet bevestigd)" : "uitgeschakeld");
    }
    public static void tick(Minecraft mc) {
        if (world != mc.level) {
            marking = false; warnings.clear(); world = mc.level;
            AutoSorterManager.abort("Wereld/dimensie gewijzigd"); NavigationManager.cancel();
        }
    }
}
