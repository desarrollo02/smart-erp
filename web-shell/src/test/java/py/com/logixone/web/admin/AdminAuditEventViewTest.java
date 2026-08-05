package py.com.logixone.web.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.application.audit.admin.AuditEventCategory;
import py.com.logixone.kernel.application.audit.admin.AuditEventOutcome;
import py.com.logixone.kernel.application.audit.admin.AuditEventView;

class AdminAuditEventViewTest {

    @Test
    void rendersPluginOperationCategoryWithCanonicalUtf8Label() {
        AuditEventView event = new AuditEventView(
                UUID.randomUUID(),
                AuditEventCategory.PLUGIN_OPERATION,
                "TEST_OPERATION",
                AuditEventOutcome.CHANGED,
                "TEST",
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
                Instant.parse("2026-08-04T00:00:00Z"));

        AdminAuditEventView rendered = AdminAuditEventView.from(event);

        assertEquals("Operación de plugin", rendered.getCategoryLabel());
    }
}
