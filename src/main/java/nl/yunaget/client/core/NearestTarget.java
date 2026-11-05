package nl.yunaget.client.core;

import java.util.*;
import java.util.function.*;

public final class NearestTarget {
    private NearestTarget() { }
    public static <T> T choose(Collection<T> candidates, Predicate<T> eligible, ToDoubleFunction<T> distanceSquared) {
        T best=null;double distance=Double.POSITIVE_INFINITY;
        for(T candidate:candidates){
            if(!eligible.test(candidate))continue;
            double value=distanceSquared.applyAsDouble(candidate);
            if(value>=0&&value<distance){best=candidate;distance=value;}
        }
        return best;
    }
}
