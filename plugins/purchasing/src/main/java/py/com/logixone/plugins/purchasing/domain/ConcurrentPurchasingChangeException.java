package py.com.logixone.plugins.purchasing.domain;

/** Raised when a command uses a stale aggregate version. */
public final class ConcurrentPurchasingChangeException extends IllegalStateException {
    public ConcurrentPurchasingChangeException(long expectedVersion, long actualVersion) {
        super("Expected purchasing version " + expectedVersion + " but found " + actualVersion);
    }
}
