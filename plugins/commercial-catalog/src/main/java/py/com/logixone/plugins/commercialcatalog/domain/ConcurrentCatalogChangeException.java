package py.com.logixone.plugins.commercialcatalog.domain;

/** Signals an optimistic version mismatch without overwriting concurrent work. */
public final class ConcurrentCatalogChangeException extends RuntimeException {
    private final long expectedVersion;
    private final long actualVersion;

    public ConcurrentCatalogChangeException(long expectedVersion, long actualVersion) {
        super("Catalog version mismatch: expected " + expectedVersion + " but was " + actualVersion);
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    public long expectedVersion() { return expectedVersion; }
    public long actualVersion() { return actualVersion; }
}
