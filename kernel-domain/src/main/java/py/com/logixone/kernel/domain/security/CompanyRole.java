package py.com.logixone.kernel.domain.security;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;

/** Versioned role owned by exactly one company. */
public record CompanyRole(
        RoleId id,
        CompanyId companyId,
        RoleCode code,
        String displayName,
        RoleStatus status,
        long version) {

    public CompanyRole {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(displayName, "displayName");
        if (displayName.isBlank()
                || displayName.length() > 160
                || !displayName.equals(displayName.strip())
                || displayName.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("displayName must be a valid presentation value");
        }
        Objects.requireNonNull(status, "status");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }

    public boolean isActive() {
        return status == RoleStatus.ACTIVE;
    }
}
