package nl.yunaget.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class InventoryTracker {
    private static final Map<String, Integer> COUNTS = new HashMap<>();
    public static int used;
    public static void tick(Minecraft mc) {
        COUNTS.clear(); used = 0;
        if (mc.player == null) return;
        for (int i = 0; i < 36; i++) {
            ItemStack s = mc.player.getInventory().getItem(i);
            if (!s.isEmpty()) { used++; COUNTS.merge(BuiltInRegistries.ITEM.getKey(s.getItem()).toString(), s.getCount(), Integer::sum); }
        }
    }
    public static int count(String id) { return COUNTS.getOrDefault(id, 0); }
    public static boolean needed(String id) {
        return YunaGetConfig.get.selectedItems.contains(id);
    }
    public static boolean roomFor(ItemStack stack) {
        var mc = Minecraft.getInstance(); if (mc.player == null) return false;
        for (int i = 0; i < 36; i++) {
            var s = mc.player.getInventory().getItem(i);
            if (s.isEmpty() || (ItemStack.isSameItemSameComponents(s, stack) && s.getCount() < s.getMaxStackSize())) return true;
        }
        return false;
    }
    public static boolean hasTargetSpace() {
        for (String id : YunaGetConfig.get.selectedItems) {
            if (needed(id)) {
                var key = net.minecraft.resources.Identifier.tryParse(id);
                if (key != null && BuiltInRegistries.ITEM.containsKey(key) && roomFor(new ItemStack(BuiltInRegistries.ITEM.getValue(key)))) return true;
            }
        }
        return false;
    }
}
