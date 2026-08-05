package py.com.logixone.plugins.inventory.domain;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.inventory.api.ExpiryPolicy;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.TrackingMode;

public record InventoryItemSnapshot(
        CompanyId companyId,
        InventoryItemId id,
        CatalogItemId catalogItemId,
        String catalogCode,
        String catalogName,
        String baseUnitCode,
        long catalogItemVersion,
        TrackingMode trackingMode,
        ExpiryPolicy expiryPolicy,
        boolean active,
        long version) {
    public InventoryItemSnapshot {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(catalogItemId, "catalogItemId");
        catalogCode = InventoryValues.code(catalogCode, "catalogCode", 64);
        catalogName = InventoryValues.text(catalogName, "catalogName", 240);
        baseUnitCode = InventoryValues.code(baseUnitCode, "baseUnitCode", 16);
        if (catalogItemVersion < 0 || version < 0) {
            throw new IllegalArgumentException("versions must not be negative");
        }
        Objects.requireNonNull(trackingMode, "trackingMode");
        Objects.requireNonNull(expiryPolicy, "expiryPolicy");
    }
}
