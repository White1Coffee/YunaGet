package nl.yunaget.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import nl.yunaget.client.core.TransferConfirmation;

/** A server-owned snapshot. Local click prediction never counts as confirmation. */
public final class InventoryTransactionManager {
    private static int menu = -1;
    private static final Map<Integer,ItemStack> SERVER = new HashMap<>();
    private static final Map<Integer,Long> REVISIONS = new HashMap<>();
    private static long revision;
    private static int waitingSlot = -1, before, beforeDestination;
    private static long sentRevision;
    private static ItemStack moving = ItemStack.EMPTY;
    public static void content(int id, List<ItemStack> stacks) {
        menu = id; SERVER.clear(); REVISIONS.clear(); revision++;
        for (int i = 0; i < stacks.size(); i++) { SERVER.put(i, stacks.get(i).copy()); REVISIONS.put(i,revision); }
    }
    public static void slot(int id, int slot, ItemStack stack) {
        if (id != menu) return;
        SERVER.put(slot,stack.copy()); REVISIONS.put(slot, ++revision);
    }
    public static boolean ready(ChestMenu chest) { return menu == chest.containerId && SERVER.size() == chest.slots.size(); }
    public static boolean pending() { return waitingSlot >= 0; }
    private static int destinationCount(ChestMenu chest, boolean server) {
        int count = 0;
        for (Slot s : chest.slots) if (s.container == chest.getContainer()) {
            ItemStack stack = server ? SERVER.getOrDefault(s.index,ItemStack.EMPTY) : s.getItem();
            if (ItemStack.isSameItemSameComponents(stack,moving)) count += stack.getCount();
        }
        return count;
    }
    public static void send(ChestMenu chest, Slot source) {
        if (pending() || !ready(chest)) throw new IllegalStateException("Containertransactie nog niet bevestigd");
        waitingSlot = source.index; moving = source.getItem().copy(); before = moving.getCount();
        beforeDestination = destinationCount(chest,true); sentRevision = revision;
        var mc = Minecraft.getInstance();
        mc.gameMode.handleContainerInput(chest.containerId, source.index, 0, ContainerInput.QUICK_MOVE, mc.player);
    }
    public static boolean confirmed(ChestMenu chest) {
        if (!pending() || !ready(chest) || REVISIONS.getOrDefault(waitingSlot,0L) <= sentRevision) return false;
        ItemStack current = SERVER.getOrDefault(waitingSlot,ItemStack.EMPTY);
        int remaining = current.isEmpty() ? 0 : ItemStack.isSameItemSameComponents(current,moving) ? current.getCount() : before;
        long destinationRevision = chest.slots.stream().filter(s -> s.container == chest.getContainer()
            && ItemStack.isSameItemSameComponents(SERVER.getOrDefault(s.index,ItemStack.EMPTY),moving))
            .mapToLong(s -> REVISIONS.getOrDefault(s.index,0L)).max().orElse(0);
        if (!TransferConfirmation.confirmed(before,remaining,beforeDestination,destinationCount(chest,true),
            sentRevision,REVISIONS.getOrDefault(waitingSlot,0L),destinationRevision)) return false;
        waitingSlot = -1; return true;
    }
    public static void cancel() { waitingSlot = -1; moving = ItemStack.EMPTY; }
    public static void invalidate() { cancel(); menu = -1; SERVER.clear(); REVISIONS.clear(); }
}
