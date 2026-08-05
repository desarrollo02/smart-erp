package py.com.logixone.kernel.application.company.port;

import java.util.Objects;

public final class PersistenceConflictException extends RuntimeException {

    private final PersistenceConflictCode code;

    public PersistenceConflictException(PersistenceConflictCode code) {
        this(code, null);
    }

    public PersistenceConflictException(PersistenceConflictCode code, Throwable cause) {
        super(Objects.requireNonNull(code, "code").name(), cause);
        this.code = code;
    }

    public PersistenceConflictCode code() {
        return code;
    }
}
