package py.com.logixone.plugins.purchasing.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptLineId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptState;
import py.com.logixone.plugins.purchasing.api.PurchaseLineKind;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderLineId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnLineId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnState;

class ReceiptAndReturnTest {
    private static final GoodsReceiptLineId RECEIPT_LINE_ID = new GoodsReceiptLineId(
            UUID.fromString("00000000-0000-0000-0000-000000000303"));
    private static final SupplierReturnLineId RETURN_LINE_ID = new SupplierReturnLineId(
            UUID.fromString("00000000-0000-0000-0000-000000000403"));
    private static final StockMovementId MOVEMENT_ID = new StockMovementId(
            UUID.fromString("00000000-0000-0000-0000-000000000500"));

    @Test
    void confirmsStockReceiptOnlyWithInventoryDestinationAndMovementReference() {
        GoodsReceipt receipt = receipt();

        assertThrows(IllegalArgumentException.class,
                () -> receipt.confirm(PurchasingDomainFixtures.APPROVER, Instant.now(), Map.of(), 0));
        receipt.confirm(
                PurchasingDomainFixtures.APPROVER, Instant.parse("2026-08-11T12:00:00Z"),
                Map.of(RECEIPT_LINE_ID, MOVEMENT_ID), 0);

        assertEquals(GoodsReceiptState.CONFIRMED, receipt.state());
        assertEquals(receipt.snapshot(), GoodsReceipt.restore(receipt.snapshot()).snapshot());
        assertThrows(IllegalStateException.class, () -> receipt.confirm(
                PurchasingDomainFixtures.APPROVER, Instant.now(),
                Map.of(RECEIPT_LINE_ID, MOVEMENT_ID), 1));
    }

    @Test
    void confirmsCompensatingReturnWithoutEditingReceipt() {
        SupplierReturn supplierReturn = SupplierReturn.draft(
                PurchasingDomainFixtures.COMPANY,
                new SupplierReturnId(UUID.fromString("00000000-0000-0000-0000-000000000401")),
                "DP-1", orderId(), "Artículo dañado", List.of(new SupplierReturn.Line(
                        RETURN_LINE_ID, receiptId(), RECEIPT_LINE_ID, orderLineId(),
                        PurchaseLineKind.STOCK, BigDecimal.ONE,
                        Optional.of(warehouseId()), Optional.of(locationId()))));

        supplierReturn.confirm(
                PurchasingDomainFixtures.APPROVER, Instant.parse("2026-08-11T13:00:00Z"),
                Map.of(RETURN_LINE_ID, MOVEMENT_ID), 0);

        assertEquals(SupplierReturnState.CONFIRMED, supplierReturn.state());
        assertEquals(
                supplierReturn.snapshot(), SupplierReturn.restore(supplierReturn.snapshot()).snapshot());
    }

    private static GoodsReceipt receipt() {
        return GoodsReceipt.draft(
                PurchasingDomainFixtures.COMPANY, receiptId(), "RC-1", orderId(),
                List.of(new GoodsReceipt.Line(
                        RECEIPT_LINE_ID, orderLineId(), PurchaseLineKind.STOCK,
                        new BigDecimal("2"), Optional.of(warehouseId()), Optional.of(locationId()))));
    }

    private static GoodsReceiptId receiptId() {
        return new GoodsReceiptId(UUID.fromString("00000000-0000-0000-0000-000000000301"));
    }

    private static PurchaseOrderId orderId() {
        return new PurchaseOrderId(UUID.fromString("00000000-0000-0000-0000-000000000201"));
    }

    private static PurchaseOrderLineId orderLineId() {
        return new PurchaseOrderLineId(UUID.fromString("00000000-0000-0000-0000-000000000202"));
    }

    private static WarehouseId warehouseId() {
        return new WarehouseId(UUID.fromString("00000000-0000-0000-0000-000000000501"));
    }

    private static StockLocationId locationId() {
        return new StockLocationId(UUID.fromString("00000000-0000-0000-0000-000000000502"));
    }
}
