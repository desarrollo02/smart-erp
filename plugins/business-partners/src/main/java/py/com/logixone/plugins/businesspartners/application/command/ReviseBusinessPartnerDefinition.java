package py.com.logixone.plugins.businesspartners.application.command;

import java.util.Objects;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAttributeCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionKind;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerName;

/** Versioned display-name revision that preserves the stable definition code. */
public record ReviseBusinessPartnerDefinition(
        BusinessPartnerDefinitionKind kind,
        BusinessPartnerAttributeCode code,
        BusinessPartnerName displayName,
        long expectedVersion) {

    public ReviseBusinessPartnerDefinition {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(displayName, "displayName");
        if (expectedVersion < 0) {
            throw new IllegalArgumentException("expectedVersion must not be negative");
        }
    }
}
