package py.com.logixone.plugins.purchasing.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.purchasing.api.PurchaseLineKind;

/** Catalog-backed or free line description frozen at document creation. */
public record PurchasedItemSnapshot(
        Optional<CatalogItemId> catalogItemId,
        Optional<String> catalogCode,
        String description,
        String presentedUnitCode,
        String baseUnitCode,
        BigDecimal conversionFactor,
        PurchaseLineKind kind,
        long sourceVersion) {

    public PurchasedItemSnapshot {
        catalogItemId = Objects.requireNonNull(catalogItemId, "catalogItemId");
        catalogCode = Objects.requireNonNull(catalogCode, "catalogCode")
                .map(value -> PurchasingValues.code(value, "catalogCode", 64));
        description = PurchasingValues.text(description, "description", 240);
        presentedUnitCode = PurchasingValues.code(
                presentedUnitCode, "presentedUnitCode", 16);
        baseUnitCode = PurchasingValues.code(baseUnitCode, "baseUnitCode", 16);
        conversionFactor = Objects.requireNonNull(conversionFactor, "conversionFactor")
                .stripTrailingZeros();
        if (conversionFactor.signum() <= 0 || Math.max(conversionFactor.scale(), 0) > 12) {
            throw new IllegalArgumentException(
                    "conversionFactor must be positive with at most 12 decimals");
        }
        Objects.requireNonNull(kind, "kind");
        if (catalogItemId.isPresent() != catalogCode.isPresent()) {
            throw new IllegalArgumentException("Catalog identity and code must be present together");
        }
        if (kind == PurchaseLineKind.STOCK && catalogItemId.isEmpty()) {
            throw new IllegalArgumentException("STOCK lines require a catalog item");
        }
        if (sourceVersion < 0 || (catalogItemId.isEmpty() && sourceVersion != 0)) {
            throw new IllegalArgumentException("Invalid catalog source version");
        }
        if (catalogItemId.isEmpty()
                && (!presentedUnitCode.equals(baseUnitCode)
                    || conversionFactor.compareTo(BigDecimal.ONE) != 0)) {
            throw new IllegalArgumentException(
                    "Free lines must use their presented unit as base with factor one");
        }
    }

    /** Compatibility constructor for a line already expressed in its base unit. */
    public PurchasedItemSnapshot(
            Optional<CatalogItemId> catalogItemId,
            Optional<String> catalogCode,
            String description,
            String unitCode,
            PurchaseLineKind kind,
            long sourceVersion) {
        this(catalogItemId, catalogCode, description, unitCode, unitCode,
                BigDecimal.ONE, kind, sourceVersion);
    }

    public String unitCode() {
        return presentedUnitCode;
    }

    public BigDecimal toBaseQuantity(BigDecimal presentedQuantity) {
        BigDecimal result = PurchasingValues.quantity(
                presentedQuantity, "presentedQuantity").multiply(conversionFactor)
                .stripTrailingZeros();
        if (Math.max(result.scale(), 0) > 6) {
            throw new IllegalArgumentException(
                    "Converted base quantity exceeds the supported 6 decimal places");
        }
        return result;
    }
}
