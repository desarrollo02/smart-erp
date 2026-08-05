package py.com.logixone.plugins.inventory.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.WarehouseId;

/** Warehouse aggregate that always owns a non-removable GENERAL location. */
public final class Warehouse {
    private final CompanyId companyId;
    private final WarehouseId id;
    private final String code;
    private String name;
    private boolean active;
    private long version;
    private final Map<StockLocationId, StockLocation> locations = new LinkedHashMap<>();

    private Warehouse(
            CompanyId companyId,
            WarehouseId id,
            StockLocationId generalLocationId,
            String code,
            String name) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.id = Objects.requireNonNull(id, "id");
        this.code = InventoryValues.code(code, "code", 64);
        this.name = InventoryValues.text(name, "name", 160);
        StockLocation general = StockLocation.general(companyId, id, generalLocationId);
        locations.put(general.id(), general);
        active = true;
    }

    public static Warehouse open(
            CompanyId companyId,
            WarehouseId id,
            StockLocationId generalLocationId,
            String code,
            String name) {
        return new Warehouse(companyId, id, generalLocationId, code, name);
    }

    public static Warehouse restore(WarehouseSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        List<StockLocationSnapshot> general = snapshot.locations().stream()
                .filter(location -> location.type() == StockLocationType.GENERAL)
                .toList();
        if (general.size() != 1) {
            throw new IllegalArgumentException("Warehouse snapshot must contain exactly one GENERAL location");
        }
        Warehouse warehouse = new Warehouse(
                snapshot.companyId(), snapshot.id(), general.getFirst().id(), snapshot.code(), snapshot.name());
        warehouse.locations.clear();
        snapshot.locations().forEach(location -> {
            if (!snapshot.companyId().equals(location.companyId())
                    || !snapshot.id().equals(location.warehouseId())
                    || warehouse.locations.putIfAbsent(location.id(), StockLocation.restore(location)) != null) {
                throw new IllegalArgumentException("Invalid warehouse location snapshot");
            }
        });
        warehouse.active = snapshot.active();
        warehouse.version = snapshot.version();
        return warehouse;
    }

    public void addLocation(
            StockLocationId locationId,
            String code,
            String name,
            StockLocationType type,
            long expectedVersion) {
        verifyVersion(expectedVersion);
        requireActive();
        Objects.requireNonNull(locationId, "locationId");
        String normalizedCode = InventoryValues.code(code, "code", 64);
        if (locations.containsKey(locationId)
                || locations.values().stream().anyMatch(location -> location.code().equals(normalizedCode))) {
            throw new IllegalArgumentException("Location id or code already exists in this warehouse");
        }
        StockLocation location = StockLocation.create(
                companyId, id, locationId, normalizedCode, name, type);
        locations.put(locationId, location);
        version++;
    }

    public StockLocation generalLocation() {
        return locations.values().stream()
                .filter(location -> location.type() == StockLocationType.GENERAL)
                .findFirst()
                .orElseThrow();
    }

    public void rename(String name, long expectedVersion) {
        verifyVersion(expectedVersion);
        requireActive();
        this.name = InventoryValues.text(name, "name", 160);
        version++;
    }

    public void renameLocation(
            StockLocationId locationId,
            String name,
            long expectedVersion,
            long expectedLocationVersion) {
        verifyVersion(expectedVersion);
        requireActive();
        location(locationId).rename(name, expectedLocationVersion);
        version++;
    }

    public void inactivateLocation(
            StockLocationId locationId,
            long expectedVersion,
            long expectedLocationVersion) {
        verifyVersion(expectedVersion);
        requireActive();
        location(locationId).inactivate(expectedLocationVersion);
        version++;
    }

    public void inactivate(long expectedVersion) {
        verifyVersion(expectedVersion);
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
            throw new IllegalStateException("Inactive warehouse cannot be changed");
        }
    }

    private StockLocation location(StockLocationId locationId) {
        StockLocation location = locations.get(Objects.requireNonNull(locationId, "locationId"));
        if (location == null) {
            throw new IllegalArgumentException("Location does not belong to this warehouse");
        }
        return location;
    }

    public CompanyId companyId() { return companyId; }
    public WarehouseId id() { return id; }
    public String code() { return code; }
    public String name() { return name; }
    public boolean active() { return active; }
    public long version() { return version; }
    public Map<StockLocationId, StockLocation> locations() { return Map.copyOf(locations); }
    public WarehouseSnapshot snapshot() {
        return new WarehouseSnapshot(
                companyId, id, code, name, active, version,
                locations.values().stream().map(StockLocation::snapshot).toList());
    }
}
