package py.com.logixone.plugins.businesspartners.domain;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerRole;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;

/** Role state and optional business-facing code owned by one partner aggregate. */
public record CommercialRole(
        BusinessPartnerRole type,
        BusinessPartnerState state,
        Optional<BusinessPartnerCode> code) {

    public CommercialRole {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(state, "state");
        code = Objects.requireNonNull(code, "code");
    }

    CommercialRole withState(BusinessPartnerState newState) {
        return new CommercialRole(type, Objects.requireNonNull(newState, "newState"), code);
    }
}
