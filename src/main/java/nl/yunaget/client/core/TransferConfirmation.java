package nl.yunaget.client.core;

public final class TransferConfirmation {
    private TransferConfirmation() { }
    public static boolean confirmed(int original, int remaining, int beforeDestination, int currentDestination,
                                    long sentRevision, long sourceRevision, long destinationRevision) {
        int moved = original - remaining;
        return original > 0 && remaining >= 0 && moved > 0 && currentDestination - beforeDestination >= moved
            && sourceRevision > sentRevision && destinationRevision > sentRevision;
    }
}
