package nl.yunaget.client.core;

import java.util.*;

public final class ItemGroups {
    private ItemGroups() { }
    public static Map<String,List<String>> defaults(){
        return Map.of("Ertsen",List.of("minecraft:diamond","minecraft:raw_iron","minecraft:raw_gold","minecraft:raw_copper","minecraft:emerald","minecraft:redstone","minecraft:lapis_lazuli","minecraft:coal","minecraft:quartz","minecraft:ancient_debris"),
            "Hout",List.of("minecraft:oak_log","minecraft:spruce_log","minecraft:birch_log","minecraft:jungle_log","minecraft:acacia_log","minecraft:dark_oak_log","minecraft:mangrove_log","minecraft:cherry_log","minecraft:pale_oak_log","minecraft:crimson_stem","minecraft:warped_stem"),
            "Bouwblokken",List.of("minecraft:cobblestone","minecraft:cobbled_deepslate","minecraft:dirt","minecraft:sand","minecraft:gravel","minecraft:netherrack","minecraft:end_stone"));
    }
    public static boolean add(Set<String> selection,Collection<String> group,int limit){
        Set<String> merged=new LinkedHashSet<>(selection);merged.addAll(group);
        if(merged.size()>limit)return false;
        selection.addAll(group);return true;
    }
}
