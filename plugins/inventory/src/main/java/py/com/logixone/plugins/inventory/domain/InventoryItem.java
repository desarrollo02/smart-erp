package py.com.logixone.plugins.inventory.domain;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemReference;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemType;
import py.com.logixone.plugins.inventory.api.ExpiryPolicy;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.TrackingMode;

/** Explicit local enrollment of one active catalog PRODUCT. */
public final class InventoryItem {
    private final CompanyId companyId;
    private final InventoryItemId id;
    private final CatalogItemId catalogItemId;
    private String catalogCode;
    private String catalogName;
    private String baseUnitCode;
    private long catalogItemVersion;
    private final TrackingMode trackingMode;
    private final ExpiryPolicy expiryPolicy;
    private boolean active;
    private long version;

    private InventoryItem(
            CompanyId companyId,
            InventoryItemId id,
            CatalogItemReference catalogItem,
            TrackingMode trackingMode,
            ExpiryPolicy expiryPolicy) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.id = Objects.requireNonNull(id, "id");
        Objects.requireNonNull(catalogItem, "catalogItem");
        if (catalogItem.type() != CatalogItemType.PRODUCT || catalogItem.state() != CatalogItemState.ACTIVE) {
            throw new IllegalArgumentException("Only an active catalog PRODUCT can be enrolled in inventory");
        }
        this.catalogItemId = catalogItem.id();
        this.catalogCode = catalogItem.code();
        this.catalogName = catalogItem.displayName();
        this.baseUnitCode = catalogItem.baseUnitCode();
        this.catalogItemVersion = catalogItem.version();
        this.trackingMode = Objects.requireNonNull(trackingMode, "trackingMode");
        this.expiryPolicy = Objects.requireNonNull(expiryPolicy, "expiryPolicy");
        active = true;
    }

    public static InventoryItem enroll(
            CompanyId companyId,
            InventoryItemId id,
            CatalogItemReference catalogItem,
            TrackingMode trackingMode,
            ExpiryPolicy expiryPolicy) {
        return new InventoryItem(companyId, id, catalogItem, trackingMode, expiryPolicy);
    }

    public static InventoryItem restore(InventoryItemSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        InventoryItem item = new InventoryItem(
                snapshot.companyId(), snapshot.id(), snapshot.catalogItemId(),
                snapshot.catalogCode(), snapshot.catalogName(), snapshot.baseUnitCode(),
                snapshot.catalogItemVersion(), snapshot.trackingMode(), snapshot.expiryPolicy());
        item.active = snapshot.active();
        item.version = snapshot.version();
        return item;
    }

    private InventoryItem(
            CompanyId companyId,
            InventoryItemId id,
            CatalogItemId catalogItemId,
            String catalogCode,
            String catalogName,
            String baseUnitCode,
            long catalogItemVersion,
            TrackingMode trackingMode,
            ExpiryPolicy expiryPolicy) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.id = Objects.requireNonNull(id, "id");
        this.catalogItemId = Objects.requireNonNull(catalogItemId, "catalogItemId");
        this.catalogCode = InventoryValues.code(catalogCode, "catalogCode", 64);
        this.catalogName = InventoryValues.text(catalogName, "catalogName", 240);
        this.baseUnitCode = InventoryValues.code(baseUnitCode, "baseUnitCode", 16);
        if (catalogItemVersion < 0) {
            throw new IllegalArgumentException("catalogItemVersion must not be negative");
        }
        this.catalogItemVersion = catalogItemVersion;
        this.trackingMode = Objects.requireNonNull(trackingMode, "trackingMode");
        this.expiryPolicy = Objects.requireNonNull(expiryPolicy, "expiryPolicy");
        this.active = true;
    }

    public void validateKey(StockKey key) {
        Objects.requireNonNull(key, "key");
        if (!id.equals(key.inventoryItemId())) {
            throw new IllegalArgumentException("Stock key belongs to another inventory item");
        }
        switch (trackingMode) {
            case NONE -> {
                if (key.lotCode().isPresent() || key.serialNumber().isPresent()) {
                    throw new IllegalArgumentException("Untracked item cannot carry lot or serial");
                }
            }
            case LOT -> {
                if (key.lotCode().isEmpty() || key.serialNumber().isPresent()) {
                    throw new IllegalArgumentException("LOT tracking requires a lot and forbids serial");
                }
            }
            case SERIAL -> {
                if (key.serialNumber().isEmpty() || key.lotCode().isPresent()) {
                    throw new IllegalArgumentException("SERIAL tracking requires a serial and forbids lot");
                }
            }
        }
        if (expiryPolicy == ExpiryPolicy.NONE && key.expiryDate().isPresent()) {
            throw new IllegalArgumentException("Expiry date is forbidden by the item policy");
        }
        if (expiryPolicy == ExpiryPolicy.REQUIRED && key.expiryDate().isEmpty()) {
            throw new IllegalArgumentException("Expiry date is required by the item policy");
        }
    }

    public void validateMovementQuantity(java.math.BigDecimal baseQuantity) {
        java.math.BigDecimal normalized = InventoryValues.quantity(baseQuantity, "baseQuantity", true);
        if (trackingMode == TrackingMode.SERIAL && normalized.compareTo(java.math.BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException("A serial-tracked movement line must have base quantity 1");
        }
    }

    public void inactivate(long expectedVersion) {
        verifyVersion(expectedVersion);
        if (active) {
            active = false;
            version++;
        }
    }

    public void refreshCatalogReference(CatalogItemReference catalogItem, long expectedVersion) {
        verifyVersion(expectedVersion);
        Objects.requireNonNull(catalogItem, "catalogItem");
        if (!catalogItemId.equals(catalogItem.id())
                || catalogItem.type() != CatalogItemType.PRODUCT
                || catalogItem.state() != CatalogItemState.ACTIVE) {
            throw new IllegalArgumentException("Catalog refresh must preserve an active PRODUCT identity");
        }
        catalogCode = catalogItem.code();
        catalogName = catalogItem.displayName();
        baseUnitCode = catalogItem.baseUnitCode();
        catalogItemVersion = catalogItem.version();
        version++;
    }

    private void verifyVersion(long expectedVersion) {
        if (expectedVersion != version) {
            throw new ConcurrentInventoryChangeException(expectedVersion, version);
        }
    }

    public CompanyId companyId() { return companyId; }
    public InventoryItemId id() { return id; }
    public CatalogItemId catalogItemId() { return catalogItemId; }
    public String catalogCode() { return catalogCode; }
    public String catalogName() { return catalogName; }
    public String baseUnitCode() { return baseUnitCode; }
    public long catalogItemVersion() { return catalogItemVersion; }
    public TrackingMode trackingMode() { return trackingMode; }
    public ExpiryPolicy expiryPolicy() { return expiryPolicy; }
    public boolean active() { return active; }
    public long version() { return version; }
    public InventoryItemSnapshot snapshot() {
        return new InventoryItemSnapshot(
                companyId, id, catalogItemId, catalogCode, catalogName, baseUnitCode,
                catalogItemVersion, trackingMode, expiryPolicy, active, version);
    }
}
