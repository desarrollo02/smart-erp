package py.com.logixone.plugins.purchasing.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Idempotent migration command for a draft or issued open purchase order. */
public record OpenPurchaseOrderImport(
        PurchasingSourceReference source,
        String number,
        String supplierId,
        String supplierCode,
        String supplierDisplayName,
        String currencyCode,
        int currencyMinorUnit,
        PurchaseOrderState state,
        Optional<LocalDate> issuedOn,
        List<PurchaseImportLine> lines) {

    public OpenPurchaseOrderImport {
        Objects.requireNonNull(source, "source");
        number = ContractValues.code(number, "number", 64);
        supplierId = ContractValues.uuid(supplierId, "supplierId").toString();
        supplierCode = ContractValues.code(supplierCode, "supplierCode", 64);
        supplierDisplayName = ContractValues.text(supplierDisplayName, "supplierDisplayName", 200);
        currencyCode = ContractValues.code(currencyCode, "currencyCode", 3);
        if (!currencyCode.matches("[A-Z]{3}") || currencyMinorUnit < 0 || currencyMinorUnit > 9) {
            throw new IllegalArgumentException("Invalid currency metadata");
        }
        Objects.requireNonNull(state, "state");
        if (state != PurchaseOrderState.DRAFT && state != PurchaseOrderState.ISSUED) {
            throw new IllegalArgumentException("Only draft or issued purchase orders can be imported");
        }
        issuedOn = Objects.requireNonNull(issuedOn, "issuedOn");
        if ((state == PurchaseOrderState.ISSUED) != issuedOn.isPresent()) {
            throw new IllegalArgumentException("issuedOn is required only for issued orders");
        }
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("lines must not be empty");
        }
        if (lines.stream().anyMatch(line -> line.expectedUnitPrice().isEmpty())) {
            throw new IllegalArgumentException("Purchase order lines require expectedUnitPrice");
        }
        long distinctKeys = lines.stream().map(PurchaseImportLine::sourceLineKey).distinct().count();
        if (distinctKeys != lines.size()) {
            throw new IllegalArgumentException("sourceLineKey must be unique within the order");
        }
    }
}
