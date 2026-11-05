package nl.yunaget.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import nl.yunaget.client.core.*;
import java.util.*;

public final class AutoSorterManager {
    private static boolean running;
    private static TaskState phase = TaskState.IDLE;
    private static StorageRule chest;
    private static ChestMenu opened;
    private static Object world;
    private static String scope;
    private static final Set<StorageRule> VISITED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static int ticks, timeout, cooldown;
    private static boolean reopening, reopenIssued, authoritativeReopen;
    private static int reopenAt, oldMenuId, expectedSize;
    public static boolean active() { return running; }
    public static void start() {
        var mc = Minecraft.getInstance();
        if (running || mc.player == null || mc.player.containerMenu != mc.player.inventoryMenu || InventoryTransactionManager.pending()
            || !(mc.gui.screen() == null || mc.gui.screen() instanceof YunaGetScreen)) { TaskManager.status = "Andere containeractie bezig"; return; }
        mc.gui.setScreen(null); TaskManager.paused = false; MiningTask.suspend(); NavigationManager.cancel();
        running = true; world = mc.level; scope = ChestManager.scope(); phase = TaskState.SEARCHING;
        opened = null; chest = null; VISITED.clear(); ticks = timeout = 0;
        reopening = reopenIssued = authoritativeReopen = false;
        TaskManager.set("Sorter",phase,"Passende chest zoeken");
    }
    public static void abort(String reason) {
        if (!running) return;
        running = false; NavigationManager.cancel(); InventoryTransactionManager.cancel();
        TaskManager.paused = true; MiningTask.suspend();
        if (chest != null) ChestManager.warnings.put(chest,reason);
        var mc = Minecraft.getInstance();
        if (mc.player != null && opened != null && mc.player.containerMenu == opened && opened.getCarried().isEmpty()) mc.player.closeContainer();
        opened = null; phase = TaskState.ERROR; cooldown = 200;
        TaskManager.set("Sorter",phase,reason);
    }
    public static void manualIntervention() { if (running) abort("Sorteren gestopt door handmatige containerbediening"); }
    private static void next(String warning) {
        var mc = Minecraft.getInstance();
        if (chest != null) { VISITED.add(chest); if (!warning.isEmpty()) ChestManager.warnings.put(chest,warning); }
        if (opened != null && mc.player.containerMenu == opened && opened.getCarried().isEmpty()) mc.player.closeContainer();
        opened = null; chest = null; timeout = 0; phase = TaskState.SEARCHING;
        reopening = reopenIssued = authoritativeReopen = false; NavigationManager.cancel();
    }
    private static boolean movable(ItemStack stack) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (YunaGetConfig.get.favorites.contains(id)) return false;
        // Whole-stack quick moves leave at least the configured storage reserve.
        return InventoryMath.movable(InventoryTracker.count(id),stack.getCount(),YunaGetConfig.get.keepAmount,0) >= stack.getCount();
    }
    private static boolean layout(ChestMenu menu) {
        var mc = Minecraft.getInstance(); int size = menu.getContainer().getContainerSize();
        if (size != 27 && size != 54) return false;
        Set<Integer> inventory = new HashSet<>(), container = new HashSet<>();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (s.index != i) return false;
            if (s.container == mc.player.getInventory() && s.getContainerSlot() >= 0 && s.getContainerSlot() < 36) inventory.add(s.getContainerSlot());
            else if (s.container == menu.getContainer() && s.getContainerSlot() >= 0 && s.getContainerSlot() < size) container.add(s.getContainerSlot());
            else return false;
        }
        return inventory.size() == 36 && container.size() == size && menu.slots.size() == size + 36;
    }
    private static boolean capacity(ChestMenu menu, ItemStack stack) {
        int room = 0;
        for (Slot s : menu.slots) if (s.container == menu.getContainer() && s.mayPlace(stack)) {
            var there = s.getItem();
            if (there.isEmpty()) room += Math.min(stack.getMaxStackSize(),s.getMaxStackSize(stack));
            else if (ItemStack.isSameItemSameComponents(there,stack)) room += Math.max(0,Math.min(stack.getMaxStackSize(),s.getMaxStackSize(stack)) - there.getCount());
        }
        return room >= stack.getCount();
    }
    public static void tick(Minecraft mc) {
        if (cooldown > 0) cooldown--;
        var c = YunaGetConfig.get;
        if (!running) {
            if (c.sorter && c.sortWhenFull && cooldown == 0 && InventoryTracker.used * 100 >= c.inventoryThreshold * 36 && mc.gui.screen() == null) start();
            return;
        }
        if (mc.level != world || !scope.equals(ChestManager.scope())) { abort("Wereld/profiel gewijzigd"); return; }
        if (++timeout > (phase == TaskState.NAVIGATING ? 600 : 200)) {
            if (phase == TaskState.VERIFYING) { abort("Geen serverbevestiging; geen extra clicks verzonden"); return; }
            next("Timeout; chest vol, beschermd of niet toegankelijk"); return;
        }
        ticks++;
        if (phase == TaskState.SEARCHING) {
            if (mc.gui.screen() != null || mc.player.containerMenu != mc.player.inventoryMenu) { abort("Andere GUI geopend"); return; }
            for (int i = c.skipHotbar ? 9 : 0; i < 36; i++) {
                var stack = mc.player.getInventory().getItem(i);
                if (stack.isEmpty() || !movable(stack)) continue;
                var dest = ChestFilterManager.destination(stack);
                if (dest != null && !VISITED.contains(dest) && (chest == null || dest.priority > chest.priority
                    || dest.priority == chest.priority && ChestManager.pos(dest).distSqr(mc.player.blockPosition()) < ChestManager.pos(chest).distSqr(mc.player.blockPosition()))) chest = dest;
            }
            if (chest == null) {
                running = false; phase = TaskState.COMPLETE; cooldown = 200;
                TaskManager.set("Sorter",phase,"Sorteren klaar; niet-passende/onbereikbare items behouden"); return;
            }
            BlockPos p = ChestManager.pos(chest);
            if (!mc.level.hasChunkAt(p) || !(mc.level.getBlockState(p).getBlock() instanceof net.minecraft.world.level.block.ChestBlock)
                || p.distSqr(mc.player.blockPosition()) > c.chestDistance*c.chestDistance) { next("Niet geladen, verdwenen of te ver"); return; }
            expectedSize = mc.level.getBlockState(p).getValue(net.minecraft.world.level.block.ChestBlock.TYPE)
                == net.minecraft.world.level.block.state.properties.ChestType.SINGLE ? 27 : 54;
            if (MiningTask.hit(p) == null) {
                if (!c.walkToChest) { next("Niet binnen interactiebereik"); return; }
                BlockPos approach = null;
                for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) if (NavigationManager.walkable(p.relative(d))) { approach = p.relative(d); break; }
                if (approach == null) { next("Geen veilige standplaats naast chest"); return; }
                NavigationManager.request(approach); phase = TaskState.NAVIGATING; timeout = 0;
            } else phase = TaskState.INTERACTING;
        }
        if (phase == TaskState.NAVIGATING) {
            if (mc.gui.screen() != null) { abort("Andere GUI geopend"); return; }
            if (MiningTask.hit(ChestManager.pos(chest)) != null) { NavigationManager.cancel(); phase = TaskState.INTERACTING; timeout = 0; }
            else if (!NavigationManager.active()) { next("Geen pad of buiten bereik"); return; }
        }
        if (reopening) {
            if (mc.player.containerMenu instanceof ChestMenu menu && menu.containerId != oldMenuId) {
                if (!reopenIssued || !layout(menu) || menu.getContainer().getContainerSize() != expectedSize || !menu.getCarried().isEmpty()) {
                    abort("Onverwachte container tijdens controle"); return;
                }
                opened = menu; reopening = false; authoritativeReopen = true;
            } else if (mc.gui.screen() != null || mc.player.containerMenu != mc.player.inventoryMenu) {
                abort("Andere container geopend tijdens controle"); return;
            } else if (!reopenIssued && ticks - reopenAt >= 2) {
                var ray = MiningTask.hit(ChestManager.pos(chest));
                if (ray == null || !(mc.level.getBlockState(ChestManager.pos(chest)).getBlock() instanceof net.minecraft.world.level.block.ChestBlock)) {
                    abort("Chest niet bereikbaar tijdens servercontrole"); return;
                }
                MiningTask.aim(ray.getLocation()); mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,ray);
                reopenIssued = true;
            }
            TaskManager.set("Sorter",TaskState.VERIFYING,"Chest heropenen voor serverbevestiging");
            if (reopening) return;
        }
        if (phase == TaskState.INTERACTING && opened == null) {
            if (mc.player.containerMenu instanceof ChestMenu menu) {
                if (!layout(menu) || menu.getContainer().getContainerSize() != expectedSize || !menu.getCarried().isEmpty()) { abort("Onverwachte containerindeling/cursoritem"); return; }
                opened = menu; timeout = 0;
                ChestManager.warnings.remove(chest);
            } else if (mc.player.containerMenu != mc.player.inventoryMenu || mc.gui.screen() != null) { abort("Onverwachte GUI"); return; }
            else if (timeout == 0 || timeout == 1) {
                var ray = MiningTask.hit(ChestManager.pos(chest));
                if (ray == null) { next("Chest buiten bereik"); return; }
                InventoryTransactionManager.invalidate(); MiningTask.aim(ray.getLocation());
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, ray);
                timeout = 2; // Wait for the actual server-opened chest; never retry an unconfirmed open.
            }
        }
        if (opened != null) {
            if (mc.player.containerMenu != opened || !layout(opened) || !opened.getCarried().isEmpty()
                || !(mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)) { abort("Container gewijzigd of gesloten"); return; }
            if (phase == TaskState.VERIFYING) {
                if (!InventoryTransactionManager.confirmed(opened)) {
                    if (authoritativeReopen && InventoryTransactionManager.ready(opened)) {
                        abort("Server bevestigt de verplaatsing niet; items niet opnieuw aangeklikt"); return;
                    }
                    if (!authoritativeReopen && timeout >= 10) {
                        oldMenuId = opened.containerId; mc.player.closeContainer(); opened = null;
                        reopening = true; reopenIssued = false; reopenAt = ticks;
                    }
                    TaskManager.set("Sorter",phase,"Wachten op serverbevestiging"); return;
                }
                authoritativeReopen = false;
                InventoryTracker.tick(mc); phase = TaskState.INTERACTING; timeout = 0;
            }
            if (!InventoryTransactionManager.ready(opened) || ticks % 5 != 0) return;
            Slot source = null; boolean matching = false;
            for (Slot s : opened.slots) {
                if (s.container != mc.player.getInventory() || (c.skipHotbar && s.getContainerSlot() < 9) || !s.mayPickup(mc.player)) continue;
                var stack = s.getItem();
                if (!stack.isEmpty() && movable(stack) && ChestFilterManager.destination(stack) == chest) {
                    matching = true; if (capacity(opened,stack)) { source = s; break; }
                }
            }
            if (source == null) { next(matching ? "Chest vol of stack past niet" : ""); return; }
            InventoryTransactionManager.send(opened,source); phase = TaskState.VERIFYING; timeout = 0;
        }
        TaskManager.set("Sorter",phase,"Chest: " + (chest == null ? "zoeken" : chest.name));
    }
}
