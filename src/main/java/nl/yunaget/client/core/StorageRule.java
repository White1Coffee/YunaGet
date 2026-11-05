package nl.yunaget.client.core;

import java.util.*;
import java.util.function.Predicate;

/** Data-only filter; tags are resolved from the live client registry by the caller. */
public final class StorageRule {
    public String name = "Chest", scope = "", dimension = "";
    public int x, y, z, priority;
    public boolean enabled = true, fallback;
    public List<String> filters = new ArrayList<>();
    public boolean matches(String item, Predicate<String> tag, Map<String, List<String>> categories) {
        return matchesTokens(filters, item, tag, categories, new HashSet<>(), 0);
    }
    private static boolean matchesTokens(List<String> tokens, String item, Predicate<String> tag,
                                          Map<String, List<String>> categories, Set<String> visited, int depth) {
        if (depth > 8 || tokens == null) return false;
        for (String raw : tokens) {
            String token = raw.trim();
            if (token.equals(item)) return true;
            if (token.startsWith("#") && tag.test(token.substring(1))) return true;
            if (token.startsWith("@") && visited.add(token)) {
                if (matchesTokens(categories.get(token.substring(1)), item, tag, categories, visited, depth + 1)) return true;
            }
        }
        return false;
    }
    public static StorageRule choose(List<StorageRule> rules, String item, Predicate<String> tags,
                                     Map<String, List<String>> categories, boolean fallback) {
        Comparator<StorageRule> order = Comparator.comparingInt((StorageRule r) -> r.priority).reversed()
            .thenComparing(r -> r.name).thenComparingInt(r -> r.x).thenComparingInt(r -> r.z);
        Optional<StorageRule> match = rules.stream().filter(r -> r.enabled && !r.fallback && r.matches(item, tags, categories)).sorted(order).findFirst();
        return match.orElseGet(() -> fallback ? rules.stream().filter(r -> r.enabled && r.fallback).sorted(order).findFirst().orElse(null) : null);
    }
}
