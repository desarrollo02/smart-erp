package py.com.logixone.plugins.sales.domain;

import java.util.Objects;
import java.util.UUID;

public record PaymentTermSnapshot(UUID id, String code, String displayName, int dueDays, long sourceVersion) {
    public PaymentTermSnapshot {
        Objects.requireNonNull(id, "id"); code = SalesValues.text(code, "term code", 32); displayName = SalesValues.text(displayName, "term name", 120);
        if (dueDays < 0 || dueDays > 3650 || sourceVersion < 0) throw new IllegalArgumentException("Invalid payment term");
    }
}
