package py.com.logixone.kernel.application.audit.admin;

import java.util.List;
import java.util.Objects;

public record AuditPage(
        List<AuditEventView> events,
        int page,
        int pageSize,
        boolean hasNext) {

    public AuditPage {
        events = List.copyOf(Objects.requireNonNull(events, "events"));
        if (page < 0 || pageSize < 1 || events.size() > pageSize) {
            throw new IllegalArgumentException("invalid audit page metadata");
        }
    }
}
