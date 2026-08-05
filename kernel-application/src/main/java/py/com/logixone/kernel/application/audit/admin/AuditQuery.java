package py.com.logixone.kernel.application.audit.admin;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import py.com.logixone.kernel.api.company.CompanyId;

/** Validated and bounded audit query independent of Jakarta/JPA. */
public record AuditQuery(
        Optional<AuditEventCategory> category,
        Optional<AuditEventOutcome> outcome,
        AuditTimeWindow timeWindow,
        Optional<CompanyId> companyId,
        Optional<String> correlationId,
        int page,
        int pageSize) {

    private static final int MAX_PAGE = 10_000;
    private static final int MAX_PAGE_SIZE = 50;
    private static final Pattern VALID_CORRELATION =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");

    public AuditQuery {
        category = Objects.requireNonNull(category, "category");
        outcome = Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(timeWindow, "timeWindow");
        companyId = Objects.requireNonNull(companyId, "companyId");
        correlationId = Objects.requireNonNull(correlationId, "correlationId");
        correlationId.ifPresent(value -> {
            if (!VALID_CORRELATION.matcher(value).matches()) {
                throw new IllegalArgumentException("correlationId has an invalid format");
            }
        });
        if (page < 0 || page > MAX_PAGE) {
            throw new IllegalArgumentException("page is outside the supported range");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize is outside the supported range");
        }
    }
}
