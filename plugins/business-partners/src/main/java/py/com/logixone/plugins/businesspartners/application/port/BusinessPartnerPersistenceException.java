package py.com.logixone.plugins.businesspartners.application.port;

import java.util.Objects;

public final class BusinessPartnerPersistenceException extends RuntimeException {

    private final BusinessPartnerPersistenceCode code;

    public BusinessPartnerPersistenceException(BusinessPartnerPersistenceCode code) {
        super(Objects.requireNonNull(code, "code").name());
        this.code = code;
    }

    public BusinessPartnerPersistenceException(
            BusinessPartnerPersistenceCode code, Throwable cause) {
        super(Objects.requireNonNull(code, "code").name(), cause);
        this.code = code;
    }

    public BusinessPartnerPersistenceCode code() {
        return code;
    }
}
