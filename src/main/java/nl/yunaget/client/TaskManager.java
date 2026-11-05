package nl.yunaget.client;

import nl.yunaget.client.core.TaskState;
import net.minecraft.client.Minecraft;

public final class TaskManager {
    public static String owner = "", status = "Gereed";
    public static TaskState state = TaskState.IDLE;
    public static boolean paused;
    private static Object currentWorld;
    public static void set(String task, TaskState next, String text) {
        if (!owner.equals(task)) { owner = task; state = TaskState.IDLE; }
        if (!state.canTransitionTo(next)) state = TaskState.SEARCHING;
        state = next; status = text;
    }
    public static void pause() {
        paused = true; MiningTask.suspend(); NavigationManager.cancel(); DropCollectorManager.reset();
        AutoSorterManager.abort("Gepauzeerd"); set("", TaskState.PAUSED, "Gepauzeerd; hervat via Dashboard");
    }
    public static void stop() {
        MiningTask.stop(""); NavigationManager.cancel(); AutoSorterManager.abort("Gestopt");
        YunaGetConfig.get.collector = false; paused = true;
        DropCollectorManager.reset(); ChestManager.marking=false; YunaGetConfig.save();
        set("", TaskState.IDLE, "Alle acties gestopt");
    }
    public static void tick(Minecraft mc) {
        if (currentWorld != mc.level) {
            if (currentWorld != null) { pause(); InventoryTransactionManager.invalidate(); }
            currentWorld = mc.level;
        }
        InventoryTracker.tick(mc); WorldScanner.tick(mc); ChestManager.tick(mc);
        if (mc.player == null || mc.level == null || mc.player.isDeadOrDying()) {
            MiningTask.stop(""); NavigationManager.cancel(); AutoSorterManager.abort("Wereld verlaten"); return;
        }
        if (paused || !mc.isWindowActive() || mc.isPaused()) { MiningTask.suspend(); NavigationManager.cancel(); return; }
        AutoSorterManager.tick(mc);
        if (AutoSorterManager.active()) { MiningTask.suspend(); return; }
        if (mc.gui.screen() != null) {
            MiningTask.suspend(); NavigationManager.cancel();
            if (MiningTask.active() || YunaGetConfig.get.collector) set("Automation",TaskState.PAUSED,"GUI open; acties wachten tot sluiten");
            return;
        }
        if (DropCollectorManager.tick(mc)) { MiningTask.suspend(); return; }
        MiningTask.tick(mc);
    }
}
