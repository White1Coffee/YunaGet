package nl.yunaget.client.core;

import java.util.*;

public final class InventoryMath {
    private InventoryMath() { }
    public record Stack(String item, int count) { }
    public static int count(List<Stack> stacks, String item) {
        return stacks.stream().filter(s -> s.item().equals(item)).mapToInt(s -> Math.max(0, s.count())).sum();
    }
    public static int movable(int total, int stack, int keep, int selectedGoal) {
        return Math.max(0, Math.min(stack, total - Math.max(keep, selectedGoal)));
    }
    public static double progress(int count, int goal) { return goal <= 0 ? 1 : Math.clamp((double) count / goal, 0, 1); }
}
