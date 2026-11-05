package nl.yunaget.client.navigation;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AStarTest {
    private static AStar.Edge<String,String> edge(String target,double cost){return new AStar.Edge<>(target,cost,List.of(target));}
    @Test void usesAccumulatedCostsRatherThanGreedyDistance(){
        var graph=Map.of("S",List.of(edge("near",20),edge("far",1)),"near",List.of(edge("G",1)),"far",List.of(edge("via",1)),"via",List.of(edge("G",1)));
        var search=new AStar<>("S",n->n.equals("G"),n->graph.getOrDefault(n,List.of()),n->0,100);
        assertEquals(AStar.Status.FOUND,search.step(100));assertEquals(3,search.cost());assertEquals(List.of("far","via","G"),search.actions());
    }
    @Test void improvesPreviouslyDiscoveredNodes(){
        var graph=Map.of("S",List.of(edge("A",10),edge("B",1)),"B",List.of(edge("A",1)),"A",List.of(edge("G",1)));
        var search=new AStar<>("S",n->n.equals("G"),n->graph.getOrDefault(n,List.of()),n->0,100);
        assertEquals(AStar.Status.FOUND,search.step(100));assertEquals(3,search.cost());assertEquals(List.of("B","A","G"),search.actions());
    }
    @Test void planningIsIncrementalAndBounded(){
        var search=new AStar<Integer,Integer>(0,n->n==10000,n->List.of(new AStar.Edge<>(n+1,1,List.of(n+1))),n->0,32);
        assertEquals(AStar.Status.SEARCHING,search.step(1));assertTrue(search.expanded()<=1);
        assertEquals(AStar.Status.LIMIT,search.step(1000));assertTrue(search.expanded()<=33);
    }
    @Test void handlesArrivalAtStartAndNoPath(){
        var arrived=new AStar<String,String>("S",n->true,n->List.of(),n->0,10);
        assertEquals(AStar.Status.FOUND,arrived.step(1));assertTrue(arrived.actions().isEmpty());assertEquals(0,arrived.cost());
        var failed=new AStar<String,String>("S",n->false,n->List.of(),n->0,10);
        assertEquals(AStar.Status.EXHAUSTED,failed.step(2));
    }
    @Test void considersMultipleReachableGoalPositions(){
        var search=new AStar<>("start",Set.of("inaccessible","reachable")::contains,
            n->n.equals("start")?List.of(edge("reachable",2)):List.of(),n->0,20);
        assertEquals(AStar.Status.FOUND,search.step(20));assertEquals(List.of("reachable"),search.actions());
    }
    @Test void longFreeRouteWinsOverShortExpensiveTunnel(){
        var graph=Map.of("S",List.of(edge("G",RouteCosts.mining(50,true)),edge("walk1",1)),
            "walk1",List.of(edge("walk2",1)),"walk2",List.of(edge("G",1)));
        var search=new AStar<>("S",n->n.equals("G"),n->graph.getOrDefault(n,List.of()),n->0,30);
        assertEquals(AStar.Status.FOUND,search.step(30));assertEquals(3,search.cost());
    }
    @Test void rejectsInvalidCostsAndHeuristics(){
        assertThrows(IllegalArgumentException.class,()->edge("G",-1));
        assertThrows(IllegalArgumentException.class,()->edge("G",Double.NaN));
        assertThrows(IllegalArgumentException.class,()->new AStar<String,String>("S",n->false,n->List.of(),n->-1,10));
    }
    private record Tile(int x,int z){}
    @Test void corridorWithRightAngleFindsCollisionFreeWaypoints(){
        Set<Tile> corridor=Set.of(new Tile(0,0),new Tile(1,0),new Tile(2,0),new Tile(2,1),new Tile(2,2));
        Tile goal=new Tile(2,2);
        var search=new AStar<Tile,Tile>(new Tile(0,0),goal::equals,t->{
            List<AStar.Edge<Tile,Tile>> edges=new ArrayList<>();
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){var next=new Tile(t.x+d[0],t.z+d[1]);if(corridor.contains(next))edges.add(new AStar.Edge<>(next,1,List.of(next)));}
            return edges;
        },t->Math.abs(t.x-goal.x)+Math.abs(t.z-goal.z),50);
        assertEquals(AStar.Status.FOUND,search.step(50));assertEquals(4,search.cost());assertTrue(corridor.containsAll(search.actions()));
    }
}
