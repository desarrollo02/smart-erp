package py.com.logixone.plugins.inventory.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemReference;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemScope;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemType;
import py.com.logixone.plugins.inventory.api.ExpiryPolicy;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.StockCountId;
import py.com.logixone.plugins.inventory.api.StockMovementDirection;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.inventory.api.StockMovementLine;
import py.com.logixone.plugins.inventory.api.StockMovementRequest;
import py.com.logixone.plugins.inventory.api.StockMovementType;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.inventory.api.StockReservationRequest;
import py.com.logixone.plugins.inventory.api.StockSourceReference;
import py.com.logixone.plugins.inventory.api.TrackingMode;

class InventoryPersistenceSnapshotTest {

    @Test
    void restoresMutableAggregateSnapshotsWithoutLosingVersions() {
        Warehouse warehouse = Warehouse.open(
                InventoryDomainFixtures.companyId(), InventoryDomainFixtures.warehouseId(10),
                InventoryDomainFixtures.locationId(11), "MAIN", "Central");
        warehouse.addLocation(
                InventoryDomainFixtures.locationId(12), "PICK", "Picking",
                StockLocationType.STORAGE, warehouse.version());
        assertEquals(warehouse.snapshot(), Warehouse.restore(warehouse.snapshot()).snapshot());

        InventoryItem item = inventoryItem();
        item.inactivate(item.version());
        assertEquals(item.snapshot(), InventoryItem.restore(item.snapshot()).snapshot());

        InventoryBalance balance = InventoryBalance.empty(
                InventoryDomainFixtures.companyId(), InventoryDomainFixtures.key(10, 11), "EA");
        balance.receive(new BigDecimal("5"), balance.version());
        balance.reserve(new BigDecimal("2"), balance.version());
        assertEquals(balance.snapshot(), InventoryBalance.restore(balance.snapshot()).snapshot());
    }

    @Test
    void restoresLedgerReservationAndCountSnapshots() {
        StockMovement movement = StockMovement.post(
                InventoryDomainFixtures.companyId(), new StockMovementId(java.util.UUID.randomUUID()),
                receiptRequest(), Instant.parse("2026-07-31T12:00:00Z"));
        InventoryItem item = inventoryItem();
        StockMovementSnapshot movementSnapshot = movement.snapshot(Map.of(
                item.id(), item));
        assertEquals(movement.reference(), StockMovement.restore(movementSnapshot).reference());

        StockReservation reservation = StockReservation.create(
                InventoryDomainFixtures.companyId(), new StockReservationId(java.util.UUID.randomUUID()),
                reservationRequest(), Instant.parse("2026-07-31T12:00:00Z"));
        reservation.consume(BigDecimal.ONE, reservation.version());
        assertEquals(reservation.snapshot(), StockReservation.restore(reservation.snapshot()).snapshot());

        StockCount count = StockCount.draft(
                InventoryDomainFixtures.companyId(), new StockCountId(java.util.UUID.randomUUID()),
                new StockCountScope(
                        InventoryDomainFixtures.warehouseId(10),
                        Optional.of(InventoryDomainFixtures.locationId(11))));
        count.addLine(InventoryDomainFixtures.key(10, 11), BigDecimal.ONE, count.version());
        count.start(count.version());
        count.record(InventoryDomainFixtures.key(10, 11), new BigDecimal("2"), count.version());
        assertEquals(count.snapshot(), StockCount.restore(count.snapshot()).snapshot());
    }

    private static InventoryItem inventoryItem() {
        CatalogItemReference catalog = new CatalogItemReference(
                new CatalogItemId(InventoryDomainFixtures.uuid(40)), "SKU-1", "Product",
                CatalogItemType.PRODUCT, CatalogItemState.ACTIVE,
                Set.of(CatalogItemScope.PURCHASE), "EA", 3);
        return InventoryItem.enroll(
                InventoryDomainFixtures.companyId(), InventoryDomainFixtures.itemId(), catalog,
                TrackingMode.LOT, ExpiryPolicy.NONE);
    }

    private static StockMovementRequest receiptRequest() {
        return new StockMovementRequest(
                StockMovementType.RECEIPT, "PURCHASE",
                new StockSourceReference("purchase_receipt", "PR-1"), "receipt-pr-1",
                List.of(new StockMovementLine(
                        InventoryDomainFixtures.key(10, 11), StockMovementDirection.INCREASE,
                        InventoryDomainFixtures.quantity("5"))), Optional.empty());
    }

    private static StockReservationRequest reservationRequest() {
        return new StockReservationRequest(
                InventoryDomainFixtures.key(10, 11), new BigDecimal("5"),
                new StockSourceReference("sales_order", "SO-1"),
                Instant.parse("2026-07-31T13:00:00Z"), "sales-so-1-line-1");
    }
}
