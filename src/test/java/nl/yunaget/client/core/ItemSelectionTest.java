package nl.yunaget.client.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ItemSelectionTest{
    @Test void legacyQuantitiesBecomeSelectionsAndNeverRequirements(){
        assertEquals(Set.of("diamond","iron"),ItemSelection.migrate(Set.of(),Map.of("diamond",64,"iron",128)));
        assertEquals(ItemSelection.migrate(Set.of(),Map.of("diamond",1)),ItemSelection.migrate(Set.of(),Map.of("diamond",99999)));
    }
    @Test void retainsNewSelectionsAndCopiesLegacyData(){
        var legacy=new HashMap<>(Map.of("diamond",64));var selection=ItemSelection.migrate(Set.of("oak_log"),legacy);
        selection.remove("diamond");assertEquals(64,legacy.get("diamond"));assertTrue(selection.contains("oak_log"));
        assertEquals(Set.of(),ItemSelection.migrate(Set.of(),null));
    }
    @Test void severalItemsCanBeCheckedAndUncheckedIndependently(){
        Set<String> selection=new LinkedHashSet<>();
        assertTrue(ItemSelection.toggle(selection,"diamond",128));assertTrue(ItemSelection.toggle(selection,"oak_log",128));
        assertEquals(2,selection.size());assertTrue(ItemSelection.toggle(selection,"diamond",128));assertEquals(Set.of("oak_log"),selection);
    }
    @Test void reachingCapacityStillAllowsDeselecting(){
        Set<String> selection=new LinkedHashSet<>(Set.of("a","b"));
        assertFalse(ItemSelection.toggle(selection,"c",2));assertEquals(2,selection.size());
        assertTrue(ItemSelection.toggle(selection,"a",2));assertTrue(ItemSelection.toggle(selection,"c",2));
    }
    @Test void groupSelectionIsAtomicAndIdempotent(){
        Set<String> selection=new LinkedHashSet<>(Set.of("a"));
        assertFalse(ItemGroups.add(selection,List.of("b","c"),2));assertEquals(Set.of("a"),selection);
        assertTrue(ItemGroups.add(selection,List.of("a","b"),2));assertEquals(Set.of("a","b"),selection);
        assertTrue(ItemGroups.add(selection,List.of("a","b"),2));
    }
    @Test void oreGroupSelectsMineableDropsRatherThanIngotRequirements(){
        var ores=ItemGroups.defaults().get("Ertsen");assertTrue(ores.contains("minecraft:raw_iron"));
        assertFalse(ores.contains("minecraft:iron_ingot"));assertEquals(ores.size(),new HashSet<>(ores).size());
    }
}
