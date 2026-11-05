package nl.yunaget.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class AutoToolManager {
    private static int original = -1, selected = -1;
    public static boolean choose(BlockState state) {
        var mc = Minecraft.getInstance(); var inventory = mc.player.getInventory();
        if (original >= 0 && selected >= 0 && inventory.getSelectedSlot() != selected) original = inventory.getSelectedSlot();
        if (!YunaGetConfig.get.autoTool) return !state.requiresCorrectToolForDrops() || mc.player.hasCorrectToolForDrops(state);
        int best = -1; double score = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(i);
            if (state.requiresCorrectToolForDrops() && !stack.isCorrectToolForDrops(state)) continue;
            if (stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() <= YunaGetConfig.get.minDurability) continue;
            double speed = stack.getDestroySpeed(state);
            for (var entry : stack.getEnchantments().entrySet()) {
                if (speed > 1 && entry.getKey().is(Identifier.parse("minecraft:efficiency"))) speed += entry.getIntValue() * entry.getIntValue() + 1;
            }
            if (speed > score || (speed == score && i == inventory.getSelectedSlot())) { score = speed; best = i; }
        }
        if (best < 0) return false;
        if (original < 0) original = inventory.getSelectedSlot();
        selected = best; inventory.setSelectedSlot(best); return true;
    }
    public static void restore() {
        var mc = Minecraft.getInstance();
        if (mc.player != null && original >= 0 && YunaGetConfig.get.restoreSlot && mc.player.getInventory().getSelectedSlot() == selected)
            mc.player.getInventory().setSelectedSlot(original);
        original = selected = -1;
    }
}
