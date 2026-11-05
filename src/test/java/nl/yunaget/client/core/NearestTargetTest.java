package nl.yunaget.client.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class NearestTargetTest{
    private record Target(String item,double distance,boolean reachable){}
    @Test void choosesClosestAcrossMultipleCheckedItems(){
        var diamond=new Target("diamond",100,true);var wood=new Target("oak_log",4,true);
        assertSame(wood,NearestTarget.choose(List.of(diamond,wood),t->Set.of("diamond","oak_log").contains(t.item()),Target::distance));
    }
    @Test void ignoresUnselectedAndRejectedTargets(){
        var wrong=new Target("dirt",1,true);var rejected=new Target("diamond",2,false);var good=new Target("diamond",9,true);
        assertSame(good,NearestTarget.choose(List.of(wrong,rejected,good),t->t.item().equals("diamond")&&t.reachable(),Target::distance));
    }
    @Test void emptyOrInvalidCandidatesProduceNoTarget(){
        assertNull(NearestTarget.choose(List.<Target>of(),t->true,Target::distance));
        assertNull(NearestTarget.choose(List.of(new Target("a",Double.NaN,true),new Target("a",-1,true)),t->true,Target::distance));
    }
    @Test void distanceIsReevaluatedWhenCandidateMoves(){
        var a=new Target("a",9,true);var b=new Target("b",16,true);
        assertSame(a,NearestTarget.choose(List.of(a,b),t->true,Target::distance));
        var moved=new Target("b",1,true);assertSame(moved,NearestTarget.choose(List.of(a,moved),t->true,Target::distance));
    }
}
