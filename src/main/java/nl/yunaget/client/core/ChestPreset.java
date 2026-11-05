package nl.yunaget.client.core;

import java.util.*;

public final class ChestPreset {
    public List<String> filters = new ArrayList<>();
    public int priority;
    public boolean fallback;
    public ChestPreset() { }
    public ChestPreset(List<String> filters,int priority,boolean fallback) {
        this.filters = new ArrayList<>(filters); this.priority = priority; this.fallback = fallback;
    }
    public void apply(StorageRule rule) {
        rule.filters = new ArrayList<>(filters); rule.priority = priority; rule.fallback = fallback;
    }
    public static Map<String,ChestPreset> defaults() {
        Map<String,ChestPreset> result = new LinkedHashMap<>();
        result.put("Diamonds",new ChestPreset(List.of("minecraft:diamond","minecraft:diamond_block","minecraft:diamond_ore","minecraft:deepslate_diamond_ore"),100,false));
        result.put("Ores",new ChestPreset(List.of("#c:ores","#c:raw_materials","#c:ingots","#c:gems","minecraft:ancient_debris","minecraft:netherite_scrap"),50,false));
        result.put("Wood",new ChestPreset(List.of("#minecraft:logs","#minecraft:planks","#minecraft:wooden_slabs","#minecraft:wooden_stairs"),30,false));
        result.put("Building Blocks",new ChestPreset(List.of("minecraft:cobblestone","minecraft:stone","minecraft:dirt","minecraft:deepslate","minecraft:cobbled_deepslate","#minecraft:stone_bricks","#minecraft:terracotta"),20,false));
        result.put("Mob Drops",new ChestPreset(List.of("minecraft:bone","minecraft:rotten_flesh","minecraft:string","minecraft:gunpowder","minecraft:ender_pearl","minecraft:spider_eye","minecraft:slime_ball"),30,false));
        result.put("Food",new ChestPreset(List.of("#c:foods","minecraft:bread","minecraft:apple","minecraft:golden_carrot","minecraft:cooked_beef","minecraft:cooked_porkchop","minecraft:baked_potato"),30,false));
        result.put("Tools",new ChestPreset(List.of("#minecraft:pickaxes","#minecraft:axes","#minecraft:shovels","#minecraft:hoes","#minecraft:swords","minecraft:shears","minecraft:bow","minecraft:crossbow"),40,false));
        result.put("Miscellaneous",new ChestPreset(List.of(),-100,true));
        return result;
    }
}
