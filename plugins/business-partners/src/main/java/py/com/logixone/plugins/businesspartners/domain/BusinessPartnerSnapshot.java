package py.com.logixone.plugins.businesspartners.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerKind;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;

/** Immutable internal state used by persistence adapters to rehydrate the aggregate. */
public record BusinessPartnerSnapshot(
        CompanyId companyId,
        BusinessPartnerId id,
        BusinessPartnerCode code,
        BusinessPartnerKind kind,
        BusinessPartnerName displayName,
        Optional<BusinessPartnerName> legalName,
        Optional<BusinessPartnerName> tradeName,
        BusinessPartnerState state,
        List<CommercialRole> roles,
        List<BusinessPartnerIdentification> identifications,
        List<BusinessPartnerAddress> addresses,
        List<BusinessPartnerContactChannel> channels,
        List<BusinessPartnerContact> contacts,
        long version) {

    public BusinessPartnerSnapshot {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(displayName, "displayName");
        legalName = Objects.requireNonNull(legalName, "legalName");
        tradeName = Objects.requireNonNull(tradeName, "tradeName");
        Objects.requireNonNull(state, "state");
        roles = List.copyOf(Objects.requireNonNull(roles, "roles"));
        identifications = List.copyOf(Objects.requireNonNull(identifications, "identifications"));
        addresses = List.copyOf(Objects.requireNonNull(addresses, "addresses"));
        channels = List.copyOf(Objects.requireNonNull(channels, "channels"));
        contacts = List.copyOf(Objects.requireNonNull(contacts, "contacts"));
        if (version < 0) {
            throw new IllegalArgumentException("Business partner version must not be negative");
        }
    }
}
