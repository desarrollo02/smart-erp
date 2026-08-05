package py.com.logixone.plugins.businesspartners.domain;

/** Signals an optimistic version mismatch without overwriting concurrent work. */
public final class ConcurrentBusinessPartnerChangeException extends RuntimeException {

    private final long expectedVersion;
    private final long actualVersion;

    public ConcurrentBusinessPartnerChangeException(long expectedVersion, long actualVersion) {
        super("Business partner version mismatch: expected "
                + expectedVersion + " but was " + actualVersion);
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
