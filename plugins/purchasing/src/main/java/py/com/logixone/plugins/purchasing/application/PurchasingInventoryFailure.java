package py.com.logixone.plugins.purchasing.application;

final class PurchasingInventoryFailure extends RuntimeException {
    private static final long serialVersionUID = 1L;

    PurchasingInventoryFailure(RuntimeException cause) {
        super(cause);
    }
}
