package py.com.logixone.plugins.sales.domain;
public final class ConcurrentSalesChangeException extends RuntimeException {
    public ConcurrentSalesChangeException(long expected, long actual) { super("Expected version " + expected + " but was " + actual); }
}
