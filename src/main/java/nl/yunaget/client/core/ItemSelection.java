package nl.yunaget.client.core;

import java.util.*;

public final class ItemSelection {
    private ItemSelection() { }
    public static Set<String> migrate(Set<String> selected, Map<String,Integer> legacy) {
        Set<String> result=new LinkedHashSet<>(selected==null?Set.of():selected);
        if(legacy!=null)result.addAll(legacy.keySet());
        return result;
    }
    public static boolean toggle(Set<String> selected,String id,int limit) {
        if(selected.remove(id))return true;
        if(selected.size()>=limit)return false;
        selected.add(id);return true;
    }
}
