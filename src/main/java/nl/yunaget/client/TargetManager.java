package nl.yunaget.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public final class TargetManager {
    private static final Map<String, String> DROPS = Map.ofEntries(
        Map.entry("diamond_ore", "diamond"), Map.entry("emerald_ore", "emerald"), Map.entry("coal_ore", "coal"),
        Map.entry("iron_ore", "raw_iron"), Map.entry("copper_ore", "raw_copper"), Map.entry("gold_ore", "raw_gold"),
        Map.entry("redstone_ore", "redstone"), Map.entry("lapis_ore", "lapis_lazuli"), Map.entry("nether_quartz_ore", "quartz"),
        Map.entry("nether_gold_ore", "gold_nugget"), Map.entry("stone", "cobblestone"), Map.entry("deepslate", "cobbled_deepslate"),
        Map.entry("grass_block", "dirt"), Map.entry("ancient_debris", "ancient_debris"));
    public static boolean matches(BlockState state) {
        if (state.isAir()) return false;
        if (InventoryTracker.needed(BuiltInRegistries.ITEM.getKey(state.getBlock().asItem()).toString())) return true;
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = id.getPath().replace("deepslate_", "");
        return id.getNamespace().equals("minecraft") && InventoryTracker.needed("minecraft:" + DROPS.getOrDefault(path, ""));
    }
}
