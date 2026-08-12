package py.com.logixone.plugins.purchasing.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestLineId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestState;

class PurchaseRequestTest {
    @Test
    void separatesRequesterFromApproverAndFreezesAnAcceptedDecision() {
        PurchaseRequest request = request();
        Instant submittedAt = Instant.parse("2026-08-11T12:00:00Z");

        request.submit(PurchasingDomainFixtures.REQUESTER, submittedAt, 0);
        assertThrows(IllegalArgumentException.class,
                () -> request.approve(PurchasingDomainFixtures.REQUESTER, submittedAt.plusSeconds(1), 1));

        request.approve(PurchasingDomainFixtures.APPROVER, submittedAt.plusSeconds(1), 1);

        assertEquals(PurchaseRequestState.APPROVED, request.state());
        assertEquals(2, request.version());
        assertEquals(request.snapshot(), PurchaseRequest.restore(request.snapshot()).snapshot());
        assertThrows(IllegalStateException.class,
                () -> request.cancel(PurchasingDomainFixtures.APPROVER, submittedAt, "Cambio", 2));
    }

    @Test
    void rejectsStaleVersionAndInvalidStockLine() {
        PurchaseRequest request = request();

        assertThrows(ConcurrentPurchasingChangeException.class,
                () -> request.submit(PurchasingDomainFixtures.REQUESTER, Instant.now(), 7));
        assertThrows(IllegalArgumentException.class, () -> new PurchasedItemSnapshot(
                Optional.empty(), Optional.empty(), "Artículo", "UN",
                py.com.logixone.plugins.purchasing.api.PurchaseLineKind.STOCK, 0));
    }

    private static PurchaseRequest request() {
        PurchaseRequest.Line line = new PurchaseRequest.Line(
                new PurchaseRequestLineId(UUID.fromString("00000000-0000-0000-0000-000000000101")),
                PurchasingDomainFixtures.stockItem(), new BigDecimal("5"),
                Optional.of(new PurchaseRequest.ExpectedPrice(
                        new BigDecimal("1000"), PurchasingDomainFixtures.pyg())));
        return PurchaseRequest.draft(
                PurchasingDomainFixtures.COMPANY,
                new PurchaseRequestId(UUID.fromString("00000000-0000-0000-0000-000000000100")),
                "SC-1", PurchasingDomainFixtures.REQUESTER,
                LocalDate.of(2026, 8, 11), List.of(line));
    }
}
