package py.com.logixone.plugins.inventory.domain;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.WarehouseId;

/** One-level location owned by exactly one warehouse. */
public final class StockLocation {
    public static final String GENERAL_CODE = "GENERAL";

    private final CompanyId companyId;
    private final WarehouseId warehouseId;
    private final StockLocationId id;
    private final String code;
    private String name;
    private final StockLocationType type;
    private boolean active;
    private long version;

    private StockLocation(
            CompanyId companyId,
            WarehouseId warehouseId,
            StockLocationId id,
            String code,
            String name,
            StockLocationType type) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.warehouseId = Objects.requireNonNull(warehouseId, "warehouseId");
        this.id = Objects.requireNonNull(id, "id");
        this.code = InventoryValues.code(code, "code", 64);
        this.name = InventoryValues.text(name, "name", 160);
        this.type = Objects.requireNonNull(type, "type");
        if ((type == StockLocationType.GENERAL) != GENERAL_CODE.equals(this.code)) {
            throw new IllegalArgumentException("GENERAL type and code must be used together");
        }
        this.active = true;
    }

    static StockLocation restore(StockLocationSnapshot snapshot) {
        StockLocation location = new StockLocation(
                snapshot.companyId(), snapshot.warehouseId(), snapshot.id(),
                snapshot.code(), snapshot.name(), snapshot.type());
        location.active = snapshot.active();
        location.version = snapshot.version();
        return location;
    }

    static StockLocation general(
            CompanyId companyId, WarehouseId warehouseId, StockLocationId id) {
        return new StockLocation(
                companyId, warehouseId, id, GENERAL_CODE, "General", StockLocationType.GENERAL);
    }

    static StockLocation create(
            CompanyId companyId,
            WarehouseId warehouseId,
            StockLocationId id,
            String code,
            String name,
            StockLocationType type) {
        if (type == StockLocationType.GENERAL) {
            throw new IllegalArgumentException("The GENERAL location is created with its warehouse");
        }
        return new StockLocation(companyId, warehouseId, id, code, name, type);
    }

    public void rename(String name, long expectedVersion) {
        verifyVersion(expectedVersion);
        requireActive();
        this.name = InventoryValues.text(name, "name", 160);
        version++;
    }

    public void inactivate(long expectedVersion) {
        verifyVersion(expectedVersion);
        if (type == StockLocationType.GENERAL) {
            throw new IllegalStateException("The GENERAL location cannot be inactivated");
        }
        if (active) {
            active = false;
            version++;
        }
    }

    private void verifyVersion(long expectedVersion) {
        if (expectedVersion != version) {
            throw new ConcurrentInventoryChangeException(expectedVersion, version);
        }
    }

    private void requireActive() {
        if (!active) {
            throw new IllegalStateException("Inactive location cannot be changed");
        }
    }

    public CompanyId companyId() { return companyId; }
    public WarehouseId warehouseId() { return warehouseId; }
    public StockLocationId id() { return id; }
    public String code() { return code; }
    public String name() { return name; }
    public StockLocationType type() { return type; }
    public boolean active() { return active; }
    public long version() { return version; }
    public StockLocationSnapshot snapshot() {
        return new StockLocationSnapshot(companyId, warehouseId, id, code, name, type, active, version);
    }
}
