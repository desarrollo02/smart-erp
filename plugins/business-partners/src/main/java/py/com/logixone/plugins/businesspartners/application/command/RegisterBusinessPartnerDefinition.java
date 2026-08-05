package py.com.logixone.plugins.businesspartners.application.command;

import java.util.Objects;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAttributeCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionKind;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerName;

/** Typed registration request for one company-owned business-partner definition. */
public record RegisterBusinessPartnerDefinition(
        BusinessPartnerDefinitionKind kind,
        BusinessPartnerAttributeCode code,
        BusinessPartnerName displayName) {

    public RegisterBusinessPartnerDefinition {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(displayName, "displayName");
    }
}
