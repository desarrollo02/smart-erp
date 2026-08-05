package py.com.logixone.plugins.inventory.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemReference;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemScope;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemType;
import py.com.logixone.plugins.inventory.api.ExpiryPolicy;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.StockCondition;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.TrackingMode;
import py.com.logixone.plugins.inventory.api.WarehouseId;

class WarehouseAndInventoryItemTest {
    @Test
    void warehouseCreatesGeneralAndProtectsItsIdentity() {
        Warehouse warehouse = Warehouse.open(companyId(), warehouseId(), locationId(3), " main ", "Central");

        assertEquals("MAIN", warehouse.code());
        assertEquals("GENERAL", warehouse.generalLocation().code());
        assertThrows(IllegalStateException.class, () -> warehouse.generalLocation().inactivate(0));

        warehouse.addLocation(locationId(4), "a-01", "Aisle A", StockLocationType.STORAGE, 0);
        assertEquals(2, warehouse.locations().size());
        assertThrows(IllegalArgumentException.class,
                () -> warehouse.addLocation(locationId(5), "A-01", "Duplicate", StockLocationType.STORAGE, 1));
    }

    @Test
    void warehouseOwnsLocationChangesAndProtectsTheGeneralLocation() {
        Warehouse warehouse = Warehouse.open(
                companyId(), warehouseId(), locationId(3), "MAIN", "Main");
        StockLocationId picking = locationId(4);
        warehouse.addLocation(picking, "PICK", "Picking", StockLocationType.STORAGE, 0);

        warehouse.renameLocation(picking, "Picking A", 1, 0);
        warehouse.inactivateLocation(picking, 2, 1);

        assertEquals("Picking A", warehouse.locations().get(picking).name());
        assertFalse(warehouse.locations().get(picking).active());
        assertThrows(IllegalStateException.class, () -> warehouse.inactivateLocation(
                warehouse.generalLocation().id(), 3, 0));
    }

    @Test
    void enrollmentAcceptsOnlyActiveProductsAndEnforcesTrackingPolicy() {
        InventoryItem item = InventoryItem.enroll(
                companyId(), itemId(), catalog(CatalogItemType.PRODUCT, CatalogItemState.ACTIVE),
                TrackingMode.SERIAL, ExpiryPolicy.REQUIRED);
        StockKey valid = new StockKey(
                item.id(), warehouseId(), locationId(3), Optional.empty(), Optional.of("SER-1"),
                Optional.of(java.time.LocalDate.of(2027, 1, 1)), StockCondition.AVAILABLE);

        item.validateKey(valid);
        item.validateMovementQuantity(BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class,
                () -> item.validateMovementQuantity(new BigDecimal("2")));
        assertThrows(IllegalArgumentException.class, () -> InventoryItem.enroll(
                companyId(), itemId(), catalog(CatalogItemType.SERVICE, CatalogItemState.ACTIVE),
                TrackingMode.NONE, ExpiryPolicy.NONE));
        assertThrows(IllegalArgumentException.class, () -> InventoryItem.enroll(
                companyId(), itemId(), catalog(CatalogItemType.PRODUCT, CatalogItemState.INACTIVE),
                TrackingMode.NONE, ExpiryPolicy.NONE));
    }

    @Test
    void refreshesTheCatalogSnapshotWithoutChangingInventoryIdentity() {
        InventoryItem item = InventoryItem.enroll(
                companyId(), itemId(), catalog(CatalogItemType.PRODUCT, CatalogItemState.ACTIVE),
                TrackingMode.NONE, ExpiryPolicy.NONE);
        CatalogItemReference refreshed = new CatalogItemReference(
                item.catalogItemId(), "P-2", "Updated product", CatalogItemType.PRODUCT,
                CatalogItemState.ACTIVE, Set.of(CatalogItemScope.PURCHASE), "KG", 5);

        item.refreshCatalogReference(refreshed, 0);

        assertEquals("P-2", item.catalogCode());
        assertEquals("KG", item.baseUnitCode());
        assertEquals(5, item.catalogItemVersion());
        assertEquals(1, item.version());
    }

    private static CatalogItemReference catalog(CatalogItemType type, CatalogItemState state) {
        return new CatalogItemReference(
                new CatalogItemId(uuid(8)), "P-1", "Product", type, state,
                Set.of(CatalogItemScope.PURCHASE), "EA", 4);
    }

    private static CompanyId companyId() { return new CompanyId(uuid(1)); }
    private static WarehouseId warehouseId() { return new WarehouseId(uuid(2)); }
    private static StockLocationId locationId(long suffix) { return new StockLocationId(uuid(suffix)); }
    private static InventoryItemId itemId() { return new InventoryItemId(uuid(7)); }
    private static UUID uuid(long suffix) { return new UUID(0, suffix); }
}
