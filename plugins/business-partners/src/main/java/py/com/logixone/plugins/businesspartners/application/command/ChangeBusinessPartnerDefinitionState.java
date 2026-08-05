package py.com.logixone.plugins.businesspartners.application.command;

import java.util.Objects;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAttributeCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionKind;

/** Versioned lifecycle change for a company-owned business-partner definition. */
public record ChangeBusinessPartnerDefinitionState(
        BusinessPartnerDefinitionKind kind,
        BusinessPartnerAttributeCode code,
        BusinessPartnerState targetState,
        long expectedVersion) {

    public ChangeBusinessPartnerDefinitionState {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(targetState, "targetState");
        if (expectedVersion < 0) {
            throw new IllegalArgumentException("expectedVersion must not be negative");
        }
    }
}
