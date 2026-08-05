package py.com.logixone.kernel.api.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;

class TechnicalAuditEventTest {

    @Test
    void acceptsOnlyBoundedTechnicalIdentifiersAndVersions() {
        TechnicalAuditEvent event = event("request:bp-1", Optional.of(3L));

        assertEquals("business_partner", event.resourceType());
        assertEquals(3L, event.resultingVersion().orElseThrow());
    }

    @Test
    void rejectsInvalidCorrelationAndNegativeVersion() {
        assertThrows(IllegalArgumentException.class, () -> event("contains space", Optional.of(3L)));
        assertThrows(IllegalArgumentException.class, () -> event("request:bp-1", Optional.of(-1L)));
    }

    private static TechnicalAuditEvent event(
            String correlationId, Optional<Long> resultingVersion) {
        return new TechnicalAuditEvent(
                "REGISTER_BUSINESS_PARTNER",
                TechnicalAuditOutcome.CHANGED,
                new AppUserId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                new CompanyId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
                "business_partners",
                "business_partners.manage",
                "business_partner",
                Optional.of("00000000-0000-0000-0000-000000000003"),
                "SUCCESS",
                Optional.empty(),
                resultingVersion,
                correlationId,
                Instant.parse("2026-07-29T12:00:00Z"));
    }
}
