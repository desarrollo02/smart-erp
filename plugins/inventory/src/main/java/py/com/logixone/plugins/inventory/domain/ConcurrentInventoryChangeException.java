package py.com.logixone.plugins.inventory.domain;

public final class ConcurrentInventoryChangeException extends RuntimeException {
    private final long expectedVersion;
    private final long actualVersion;

    public ConcurrentInventoryChangeException(long expectedVersion, long actualVersion) {
        super("Expected inventory version " + expectedVersion + " but found " + actualVersion);
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    public long expectedVersion() {
        return expectedVersion;
    }

    public long actualVersion() {
        return actualVersion;
    }
}
