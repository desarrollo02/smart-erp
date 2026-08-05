package py.com.logixone.kernel.api.audit;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;

/**
 * Framework-free envelope for auditable plugin operations. Values are technical
 * identifiers only; commercial or personal data must never be placed here.
 */
public record TechnicalAuditEvent(
        String operation,
        TechnicalAuditOutcome outcome,
        AppUserId actorUserId,
        CompanyId companyId,
        String pluginId,
        String permissionId,
        String resourceType,
        Optional<String> resourceId,
        String resultCode,
        Optional<Long> previousVersion,
        Optional<Long> resultingVersion,
        String correlationId,
        Instant occurredAt) {

    private static final Pattern CORRELATION =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");

    public TechnicalAuditEvent {
        operation = requireText(operation, "operation", 96);
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(actorUserId, "actorUserId");
        Objects.requireNonNull(companyId, "companyId");
        pluginId = requireText(pluginId, "pluginId", 128);
        permissionId = requireText(permissionId, "permissionId", 160);
        resourceType = requireText(resourceType, "resourceType", 96);
        resourceId = Objects.requireNonNull(resourceId, "resourceId")
                .map(value -> requireText(value, "resourceId", 160));
        resultCode = requireText(resultCode, "resultCode", 128);
        previousVersion = nonNegative(previousVersion, "previousVersion");
        resultingVersion = nonNegative(resultingVersion, "resultingVersion");
        correlationId = requireText(correlationId, "correlationId", 128);
        if (!CORRELATION.matcher(correlationId).matches()) {
            throw new IllegalArgumentException("Invalid correlationId");
        }
        Objects.requireNonNull(occurredAt, "occurredAt");
    }

    private static Optional<Long> nonNegative(Optional<Long> value, String name) {
        Objects.requireNonNull(value, name);
        if (value.filter(number -> number < 0).isPresent()) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }

    private static String requireText(String value, String name, int maxLength) {
        Objects.requireNonNull(value, name);
        if (value.isBlank() || value.length() > maxLength
                || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid " + name);
        }
        return value;
    }
}
