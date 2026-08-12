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
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderLineId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderState;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestLineId;

class PurchaseOrderTest {
    private static final PurchaseOrderLineId LINE_ID = new PurchaseOrderLineId(
            UUID.fromString("00000000-0000-0000-0000-000000000202"));

    @Test
    void appliesPartialReceiptsReturnsAndReopensFulfillmentWithoutOverReceiving() {
        PurchaseOrder order = order();
        order.issue(PurchasingDomainFixtures.APPROVER, Instant.parse("2026-08-11T12:00:00Z"), 0);

        order.applyReceipt(Map.of(LINE_ID, new BigDecimal("6")), 1);
        assertEquals(new BigDecimal("4"), order.lines().getFirst().pendingQuantity());
        assertThrows(IllegalArgumentException.class,
                () -> order.applyReceipt(Map.of(LINE_ID, new BigDecimal("5")), 2));

        order.applyReturn(Map.of(LINE_ID, new BigDecimal("2")), 2);
        assertEquals(new BigDecimal("6"), order.lines().getFirst().pendingQuantity());
        order.applyReceipt(Map.of(LINE_ID, new BigDecimal("6")), 3);
        assertEquals(PurchaseOrderState.CLOSED, order.state());

        order.applyReturn(Map.of(LINE_ID, BigDecimal.ONE), 4);
        assertEquals(PurchaseOrderState.ISSUED, order.state());
        assertEquals(BigDecimal.ONE, order.lines().getFirst().pendingQuantity());
        assertEquals(order.snapshot(), PurchaseOrder.restore(order.snapshot()).snapshot());
    }

    @Test
    void requiresJustificationForDirectQuantityAndClosesShortExplicitly() {
        PurchaseOrder.LineDraft direct = new PurchaseOrder.LineDraft(
                LINE_ID, PurchasingDomainFixtures.service(), new BigDecimal("3"),
                new BigDecimal("100"), List.of());
        assertThrows(IllegalArgumentException.class, () -> PurchaseOrder.draft(
                PurchasingDomainFixtures.COMPANY,
                new PurchaseOrderId(UUID.fromString("00000000-0000-0000-0000-000000000201")),
                "OC-1", PurchasingDomainFixtures.supplier(), PurchasingDomainFixtures.pyg(),
                List.of(direct), Optional.empty()));

        PurchaseOrder order = PurchaseOrder.draft(
                PurchasingDomainFixtures.COMPANY,
                new PurchaseOrderId(UUID.fromString("00000000-0000-0000-0000-000000000201")),
                "OC-1", PurchasingDomainFixtures.supplier(), PurchasingDomainFixtures.pyg(),
                List.of(direct), Optional.of("Compra directa autorizada"));
        order.issue(PurchasingDomainFixtures.APPROVER, Instant.now(), 0);
        order.closeShort(Map.of(LINE_ID, new BigDecimal("3")), "Proveedor sin saldo", 1);

        assertEquals(PurchaseOrderState.CLOSED, order.state());
    }

    private static PurchaseOrder order() {
        PurchaseOrder.Allocation allocation = new PurchaseOrder.Allocation(
                new PurchaseRequestId(UUID.fromString("00000000-0000-0000-0000-000000000100")),
                new PurchaseRequestLineId(UUID.fromString("00000000-0000-0000-0000-000000000101")),
                new BigDecimal("4"));
        PurchaseOrder.LineDraft line = new PurchaseOrder.LineDraft(
                LINE_ID, PurchasingDomainFixtures.stockItem(), new BigDecimal("10"),
                new BigDecimal("2500"), List.of(allocation));
        return PurchaseOrder.draft(
                PurchasingDomainFixtures.COMPANY,
                new PurchaseOrderId(UUID.fromString("00000000-0000-0000-0000-000000000201")),
                "OC-1", PurchasingDomainFixtures.supplier(), PurchasingDomainFixtures.pyg(),
                List.of(line), Optional.of("Saldo directo autorizado"));
    }
}
