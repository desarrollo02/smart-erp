package py.com.logixone.kernel.application.security.port;

import java.util.Objects;

/** Stable application exception that prevents Jakarta/JPA/SQL failures crossing the port. */
public final class SecurityPersistenceException extends RuntimeException {

    private final SecurityPersistenceCode code;

    public SecurityPersistenceException(SecurityPersistenceCode code) {
        super(Objects.requireNonNull(code, "code").name());
        this.code = code;
    }

    public SecurityPersistenceException(SecurityPersistenceCode code, Throwable cause) {
        super(Objects.requireNonNull(code, "code").name(), cause);
        this.code = code;
    }

    public SecurityPersistenceCode code() {
        return code;
    }
}
