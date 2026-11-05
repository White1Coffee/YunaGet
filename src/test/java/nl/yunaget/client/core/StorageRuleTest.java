package nl.yunaget.client.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StorageRuleTest {
    private StorageRule rule(String name,int priority,String... filters) {
        var r=new StorageRule();r.name=name;r.priority=priority;r.filters=new ArrayList<>(List.of(filters));return r;
    }
    @Test void exactItemsDoNotMatchSimilarNames() {
        var r=rule("Diamond",0,"minecraft:diamond","minecraft:diamond_block");
        assertTrue(r.matches("minecraft:diamond",t->false,Map.of()));
        assertTrue(r.matches("minecraft:diamond_block",t->false,Map.of()));
        assertFalse(r.matches("other:diamond",t->false,Map.of()));
        assertFalse(r.matches("minecraft:diamond_sword",t->false,Map.of()));
    }
    @Test void tagsUseRegistryAnswerAndMissingTagsDoNotMatch() {
        var r=rule("Wood",0,"#minecraft:logs");
        assertTrue(r.matches("minecraft:oak_log",t->t.equals("minecraft:logs"),Map.of()));
        assertFalse(r.matches("minecraft:oak_log",t->false,Map.of()));
    }
    @Test void categoriesCanNestAndCyclesTerminate() {
        var r=rule("Nested",0,"@all");
        var cats=Map.of("all",List.of("@wood","@cycle"),"wood",List.of("minecraft:oak_log"),"cycle",List.of("@all"));
        assertTrue(r.matches("minecraft:oak_log",t->false,cats));
        assertFalse(r.matches("minecraft:diamond",t->false,cats));
    }
    @Test void enabledHighestPrioritySpecificRuleWinsOverFallback() {
        var low=rule("Low",1,"minecraft:diamond");var high=rule("High",20,"minecraft:diamond");
        var fallback=rule("Fallback",10000);fallback.fallback=true;
        assertSame(high,StorageRule.choose(List.of(fallback,low,high),"minecraft:diamond",t->false,Map.of(),true));
        high.enabled=false;
        assertSame(low,StorageRule.choose(List.of(high,low),"minecraft:diamond",t->false,Map.of(),true));
    }
    @Test void fallbackIsExplicitAndNeverMatchesWhenDisabled() {
        var fallback=rule("Misc",0);fallback.fallback=true;
        assertNull(StorageRule.choose(List.of(fallback),"minecraft:stone",t->false,Map.of(),false));
        assertSame(fallback,StorageRule.choose(List.of(fallback),"minecraft:stone",t->false,Map.of(),true));
        fallback.enabled=false;
        assertNull(StorageRule.choose(List.of(fallback),"minecraft:stone",t->false,Map.of(),true));
    }
    @Test void tieBreakDoesNotDependOnListOrder() {
        var a=rule("A",1,"minecraft:stone");var b=rule("B",1,"minecraft:stone");
        assertSame(a,StorageRule.choose(List.of(b,a),"minecraft:stone",t->false,Map.of(),false));
        assertSame(a,StorageRule.choose(List.of(a,b),"minecraft:stone",t->false,Map.of(),false));
    }
}
