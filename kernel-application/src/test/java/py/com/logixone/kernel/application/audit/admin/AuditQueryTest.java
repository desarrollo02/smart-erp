package py.com.logixone.kernel.application.audit.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditQueryTest {

    @Test
    void acceptsOnlyBoundedPagesAndValidatedExactCorrelation() {
        AuditQuery query = new AuditQuery(
                Optional.of(AuditEventCategory.SYSTEM_AUTHORITY_ACCESS),
                Optional.of(AuditEventOutcome.DENIED),
                AuditTimeWindow.LAST_7_DAYS,
                Optional.empty(),
                Optional.of("request:abc-123"),
                10_000,
                50);

        assertEquals(10_000, query.page());
        assertEquals(50, query.pageSize());
        assertEquals("request:abc-123", query.correlationId().orElseThrow());
    }

    @Test
    void rejectsInvalidPagesSizesAndCorrelationInsteadOfAcceptingQuerySyntax() {
        assertInvalid(-1, 25, Optional.empty());
        assertInvalid(10_001, 25, Optional.empty());
        assertInvalid(0, 0, Optional.empty());
        assertInvalid(0, 51, Optional.empty());
        for (String invalid : new String[] {
            "", " leading", "contains space", "quote'", "a".repeat(129)
        }) {
            assertInvalid(0, 25, Optional.of(invalid));
        }
    }

    @Test
    void closedTimeWindowsCalculateExpectedLowerBound() {
        Instant now = Instant.parse("2026-07-28T20:00:00Z");

        assertEquals(
                Optional.of(Instant.parse("2026-07-27T20:00:00Z")),
                AuditTimeWindow.LAST_24_HOURS.lowerBound(now));
        assertEquals(
                Optional.of(Instant.parse("2026-07-21T20:00:00Z")),
                AuditTimeWindow.LAST_7_DAYS.lowerBound(now));
        assertEquals(
                Optional.of(Instant.parse("2026-06-28T20:00:00Z")),
                AuditTimeWindow.LAST_30_DAYS.lowerBound(now));
        assertTrue(AuditTimeWindow.ALL.lowerBound(now).isEmpty());
    }

    @Test
    void auditPageCopiesInputAndRejectsInconsistentMetadata() {
        List<AuditEventView> mutable = new java.util.ArrayList<>();
        AuditPage page = new AuditPage(mutable, 0, 25, false);
        mutable.clear();

        assertTrue(page.events().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new AuditPage(List.of(), -1, 25, false));
        assertThrows(IllegalArgumentException.class, () -> new AuditPage(List.of(), 0, 0, false));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuditPage(List.of(event(1), event(2)), 0, 1, true));
    }

    private static void assertInvalid(
            int page,
            int pageSize,
            Optional<String> correlationId) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuditQuery(
                        Optional.empty(),
                        Optional.empty(),
                        AuditTimeWindow.ALL,
                        Optional.empty(),
                        correlationId,
                        page,
                        pageSize));
    }

    private static AuditEventView event(int suffix) {
        return new AuditEventView(
                UUID.fromString("00000000-0000-0000-0000-00000000000" + suffix),
                AuditEventCategory.SYSTEM_AUTHORITY_ACCESS,
                "AUTHORIZE",
                AuditEventOutcome.ALLOWED,
                "AUTHENTICATED",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Instant.parse("2026-07-28T20:00:00Z"));
    }
}
