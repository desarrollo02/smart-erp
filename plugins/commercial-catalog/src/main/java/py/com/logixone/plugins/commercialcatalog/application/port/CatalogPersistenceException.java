package py.com.logixone.plugins.commercialcatalog.application.port;

import java.util.Objects;

public final class CatalogPersistenceException extends RuntimeException {
    private final CatalogPersistenceCode code;

    public CatalogPersistenceException(CatalogPersistenceCode code) {
        this(code, null);
    }

    public CatalogPersistenceException(CatalogPersistenceCode code, Throwable cause) {
        super(Objects.requireNonNull(code, "code").name(), cause);
        this.code = code;
    }

    public CatalogPersistenceCode code() { return code; }
}
