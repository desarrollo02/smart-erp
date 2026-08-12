package py.com.logixone.plugins.purchasing.api;

import java.time.LocalDate;
import java.util.Objects;

/** Minimal immutable projection of a purchase request. */
public record PurchaseRequestReference(
        PurchaseRequestId id,
        String number,
        PurchaseRequestState state,
        LocalDate requestedOn,
        int lineCount,
        long version) {

    public PurchaseRequestReference {
        Objects.requireNonNull(id, "id");
        number = ContractValues.code(number, "number", 64);
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(requestedOn, "requestedOn");
        if (lineCount <= 0 || version < 0) {
            throw new IllegalArgumentException("Invalid request line count or version");
        }
    }
}
