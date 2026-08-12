package py.com.logixone.plugins.purchasing.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Idempotent migration command for a non-terminal purchase request. */
public record OpenPurchaseRequestImport(
        PurchasingSourceReference source,
        String number,
        LocalDate requestedOn,
        PurchaseRequestState state,
        Optional<String> expectedCurrencyCode,
        List<PurchaseImportLine> lines) {

    public OpenPurchaseRequestImport {
        Objects.requireNonNull(source, "source");
        number = ContractValues.code(number, "number", 64);
        Objects.requireNonNull(requestedOn, "requestedOn");
        Objects.requireNonNull(state, "state");
        expectedCurrencyCode = Objects.requireNonNull(
                expectedCurrencyCode, "expectedCurrencyCode")
                .map(value -> ContractValues.code(value, "expectedCurrencyCode", 3));
        if (state == PurchaseRequestState.REJECTED || state == PurchaseRequestState.CANCELLED) {
            throw new IllegalArgumentException("Only open purchase requests can be imported");
        }
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("lines must not be empty");
        }
        long distinctKeys = lines.stream().map(PurchaseImportLine::sourceLineKey).distinct().count();
        if (distinctKeys != lines.size()) {
            throw new IllegalArgumentException("sourceLineKey must be unique within the request");
        }
        boolean hasExpectedPrices = lines.stream()
                .anyMatch(line -> line.expectedUnitPrice().isPresent());
        if (hasExpectedPrices != expectedCurrencyCode.isPresent()) {
            throw new IllegalArgumentException(
                    "expectedCurrencyCode is required exactly when expected prices exist");
        }
    }

    public OpenPurchaseRequestImport(
            PurchasingSourceReference source,
            String number,
            LocalDate requestedOn,
            PurchaseRequestState state,
            List<PurchaseImportLine> lines) {
        this(source, number, requestedOn, state, Optional.empty(), lines);
    }
}
