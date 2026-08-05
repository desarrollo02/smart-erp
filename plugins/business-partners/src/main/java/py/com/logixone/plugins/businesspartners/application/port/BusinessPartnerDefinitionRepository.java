package py.com.logixone.plugins.businesspartners.application.port;

import java.util.List;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAttributeCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionKind;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionRevision;

/** Private persistence boundary for company-owned selector definitions. */
public interface BusinessPartnerDefinitionRepository {

    List<BusinessPartnerDefinition> findAll(
            CompanyId companyId, BusinessPartnerDefinitionKind kind);

    Optional<BusinessPartnerDefinition> findByCode(
            CompanyId companyId,
            BusinessPartnerDefinitionKind kind,
            BusinessPartnerAttributeCode code);

    /** Resolves and, where supported, locks a definition used by a new operation. */
    default Optional<BusinessPartnerDefinition> findByCodeForReference(
            CompanyId companyId,
            BusinessPartnerDefinitionKind kind,
            BusinessPartnerAttributeCode code) {
        return findByCode(companyId, kind, code);
    }

    List<BusinessPartnerDefinitionRevision> history(
            CompanyId companyId,
            BusinessPartnerDefinitionKind kind,
            BusinessPartnerAttributeCode code);

    BusinessPartnerDefinition insert(BusinessPartnerDefinition definition);

    BusinessPartnerDefinition update(
            BusinessPartnerDefinition definition, long expectedPersistedVersion);
}
