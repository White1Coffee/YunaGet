package nl.yunaget.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

/** Rendering only: never changes synced entity flags or sends server commands. */
public final class ItemGlow {
    private ItemGlow() { }

    public static boolean shouldHighlight(Entity entity) {
        if (YunaGetConfig.get.entityGlow && Keybinds.entity != null && !(entity instanceof ItemEntity)
            && entity.isAlive() && Keybinds.entity.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))) {
            var player = Minecraft.getInstance().player;
            return player != null && entity.distanceToSqr(player) <= YunaGetConfig.get.renderDistance * YunaGetConfig.get.renderDistance;
        }
        if (!Keybinds.itemGlow || !(entity instanceof ItemEntity drop) || !drop.isAlive()) return false;
        var player = Minecraft.getInstance().player;
        if (player == null || drop.distanceToSqr(player) > 64 * 64 || drop.getItem().isEmpty()) return false;
        return !Keybinds.glowSelectedOnly || YunaGetConfig.get.selectedItems.contains(BuiltInRegistries.ITEM.getKey(drop.getItem().getItem()).toString());
    }
}
