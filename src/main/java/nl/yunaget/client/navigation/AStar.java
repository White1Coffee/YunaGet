package nl.yunaget.client.navigation;

import java.util.*;
import java.util.function.*;

/** Incremental A*: real g-costs, stale queue suppression, reopening and hard memory/expansion limits. */
public final class AStar<N,A> {
    public enum Status { SEARCHING, FOUND, EXHAUSTED, LIMIT }
    public record Edge<N,A>(N to,double cost,List<A> actions) {
        public Edge { if(!Double.isFinite(cost)||cost<=0)throw new IllegalArgumentException("Positive finite edge cost required"); actions=List.copyOf(actions); }
    }
    private record QueueNode<N>(N node,double g,double f,long order) { }
    private record Parent<N,A>(N from,Edge<N,A> edge) { }
    private final PriorityQueue<QueueNode<N>> open=new PriorityQueue<>(Comparator.<QueueNode<N>>comparingDouble(QueueNode::f).thenComparingDouble(QueueNode::g).thenComparingLong(QueueNode::order));
    private final Map<N,Double> scores=new HashMap<>();
    private final Map<N,Parent<N,A>> parents=new HashMap<>();
    private final Function<N,List<Edge<N,A>>> neighbors;
    private final Predicate<N> goal;
    private final ToDoubleFunction<N> heuristic;
    private final int limit;
    private final N start;
    private N found;
    private int expanded;
    private long serial;
    private boolean truncated;
    private Status status=Status.SEARCHING;
    public AStar(N start,Predicate<N> goal,Function<N,List<Edge<N,A>>> neighbors,ToDoubleFunction<N> heuristic,int limit) {
        if(limit<1)throw new IllegalArgumentException("limit");
        this.start=start;this.goal=goal;this.neighbors=neighbors;this.heuristic=heuristic;this.limit=limit;
        scores.put(start,0d);open.add(new QueueNode<>(start,0,estimate(start),serial++));
    }
    private double estimate(N n) { double h=heuristic.applyAsDouble(n);if(!Double.isFinite(h)||h<0)throw new IllegalArgumentException("Invalid heuristic");return h; }
    public Status step(int budget) {
        for(int work=0;work<budget&&status==Status.SEARCHING;work++) {
            if(open.isEmpty()){status=truncated?Status.LIMIT:Status.EXHAUSTED;break;}
            var current=open.remove();
            if(current.g()>scores.getOrDefault(current.node(),Double.POSITIVE_INFINITY)+1e-9)continue;
            if(goal.test(current.node())){found=current.node();status=Status.FOUND;break;}
            if(++expanded>limit){status=Status.LIMIT;break;}
            for(var edge:neighbors.apply(current.node())) {
                double cost=current.g()+edge.cost();
                if(cost+1e-9>=scores.getOrDefault(edge.to(),Double.POSITIVE_INFINITY))continue;
                if(!scores.containsKey(edge.to())&&scores.size()>=limit){truncated=true;continue;}
                if(open.size()>=limit*6){status=Status.LIMIT;break;}
                scores.put(edge.to(),cost);parents.put(edge.to(),new Parent<>(current.node(),edge));
                open.add(new QueueNode<>(edge.to(),cost,cost+estimate(edge.to()),serial++));
            }
        }
        return status;
    }
    public List<A> actions() {
        if(status!=Status.FOUND)return List.of();
        var edges=new ArrayDeque<List<A>>();
        for(N n=found;!n.equals(start);){var p=parents.get(n);edges.addFirst(p.edge().actions());n=p.from();}
        List<A> result=new ArrayList<>();edges.forEach(result::addAll);return List.copyOf(result);
    }
    public Status status(){return status;}
    public double cost(){return found==null?Double.POSITIVE_INFINITY:scores.get(found);}
    public int expanded(){return expanded;}
}
