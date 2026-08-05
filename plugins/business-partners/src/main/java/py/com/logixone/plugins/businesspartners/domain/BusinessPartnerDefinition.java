package py.com.logixone.plugins.businesspartners.domain;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;

/** Company-owned definition referenced by stable code from business-partner details. */
public record BusinessPartnerDefinition(
        CompanyId companyId,
        BusinessPartnerDefinitionKind kind,
        BusinessPartnerAttributeCode code,
        BusinessPartnerName displayName,
        BusinessPartnerState state,
        long version) {

    public BusinessPartnerDefinition {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(state, "state");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }

    public static BusinessPartnerDefinition create(
            CompanyId companyId,
            BusinessPartnerDefinitionKind kind,
            BusinessPartnerAttributeCode code,
            BusinessPartnerName displayName) {
        return new BusinessPartnerDefinition(
                companyId, kind, code, displayName, BusinessPartnerState.ACTIVE, 0);
    }

    public BusinessPartnerDefinition changeState(
            BusinessPartnerState targetState, long expectedVersion) {
        Objects.requireNonNull(targetState, "targetState");
        if (expectedVersion != version) {
            throw new ConcurrentBusinessPartnerChangeException(expectedVersion, version);
        }
        if (state == targetState) {
            return this;
        }
        return new BusinessPartnerDefinition(
                companyId, kind, code, displayName, targetState, version + 1);
    }

    public BusinessPartnerDefinition reviseDisplayName(
            BusinessPartnerName revisedDisplayName, long expectedVersion) {
        Objects.requireNonNull(revisedDisplayName, "revisedDisplayName");
        if (expectedVersion != version) {
            throw new ConcurrentBusinessPartnerChangeException(expectedVersion, version);
        }
        if (displayName.equals(revisedDisplayName)) {
            return this;
        }
        return new BusinessPartnerDefinition(
                companyId, kind, code, revisedDisplayName, state, version + 1);
    }
}
