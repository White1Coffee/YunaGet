package nl.yunaget.client.navigation;
public enum NavigationOwner {
    MINING(1), DROP_COLLECTOR(2), AUTO_SORTER(3);
    public final int priority;
    NavigationOwner(int priority) { this.priority=priority; }
    public static NavigationOwner legacy(String name) {
        return switch(name) { case "Sorter", "AUTO_SORTER" -> AUTO_SORTER; case "Collector", "DROP_COLLECTOR" -> DROP_COLLECTOR; default -> MINING; };
    }
    public String display() { return switch(this) { case MINING -> "Mining"; case DROP_COLLECTOR -> "Collector"; case AUTO_SORTER -> "Sorter"; }; }
}
