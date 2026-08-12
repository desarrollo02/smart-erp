package py.com.logixone.plugins.purchasing.application.port;

import java.util.Objects;

public final class PurchasingPersistenceException extends RuntimeException {
    private final PurchasingPersistenceCode code;

    public PurchasingPersistenceException(PurchasingPersistenceCode code) {
        super(Objects.requireNonNull(code, "code").name());
        this.code = code;
    }

    public PurchasingPersistenceException(PurchasingPersistenceCode code, Throwable cause) {
        super(Objects.requireNonNull(code, "code").name(), cause);
        this.code = code;
    }

    public PurchasingPersistenceCode code() {
        return code;
    }
}
