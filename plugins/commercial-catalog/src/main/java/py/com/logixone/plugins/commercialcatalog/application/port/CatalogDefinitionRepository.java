package py.com.logixone.plugins.commercialcatalog.application.port;

import java.util.List;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.application.definition.CatalogDefinitions;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileId;
import py.com.logixone.plugins.commercialcatalog.domain.VariantFamilyId;

public interface CatalogDefinitionRepository {
    CatalogDefinitions.Snapshot findAll(CompanyId companyId);
    CatalogDefinitions.Unit insert(CompanyId companyId, CatalogDefinitions.Unit definition);
    CatalogDefinitions.Category insert(CompanyId companyId, CatalogDefinitions.Category definition);
    CatalogDefinitions.Brand insert(CompanyId companyId, CatalogDefinitions.Brand definition);
    CatalogDefinitions.Tag insert(CompanyId companyId, CatalogDefinitions.Tag definition);
    CatalogDefinitions.Lifecycle changeSimpleState(
            CompanyId companyId,
            CatalogDefinitions.SimpleKind kind,
            String identity,
            CatalogDefinitions.State targetState,
            long expectedVersion);
    CatalogDefinitions.SimpleRevision reviseSimpleDefinition(
            CompanyId companyId,
            CatalogDefinitions.SimpleKind kind,
            String identity,
            String displayName,
            java.util.Optional<Integer> decimalScale,
            java.util.Optional<py.com.logixone.plugins.commercialcatalog.domain.CategoryId> parentId,
            long expectedVersion);
    List<CatalogDefinitions.SimpleRevision> simpleDefinitionHistory(
            CompanyId companyId,
            CatalogDefinitions.SimpleKind kind,
            String identity);
    CatalogDefinitions.Replacement replaceSimpleDefinition(
            CompanyId companyId,
            CatalogDefinitions.SimpleKind kind,
            String identity,
            CatalogDefinitions.ReplacementCandidate replacement,
            long expectedVersion);
    CatalogDefinitions.TaxProfile insert(
            CompanyId companyId, CatalogDefinitions.TaxProfile definition);
    CatalogDefinitions.TaxProfile changeTaxProfileState(
            CompanyId companyId,
            TaxProfileId id,
            CatalogDefinitions.State targetState,
            long expectedVersion);
    CatalogDefinitions.TaxProfile reviseTaxProfile(
            CompanyId companyId,
            TaxProfileId id,
            String internalKindCode,
            String description,
            java.time.Instant validFrom,
            java.util.Optional<java.time.Instant> validUntil,
            long expectedVersion);
    List<CatalogDefinitions.TaxProfileRevision> taxProfileHistory(
            CompanyId companyId, TaxProfileId id);
    CatalogDefinitions.VariantFamily insert(
            CompanyId companyId, CatalogDefinitions.VariantFamily definition);
    CatalogDefinitions.VariantFamily changeVariantFamilyState(
            CompanyId companyId,
            VariantFamilyId id,
            CatalogDefinitions.State targetState,
            long expectedVersion);
    CatalogDefinitions.VariantFamily reviseVariantFamily(
            CompanyId companyId,
            VariantFamilyId id,
            String displayName,
            List<CatalogDefinitions.VariantAttribute> attributes,
            long expectedVersion);
    List<CatalogDefinitions.VariantFamilyRevision> variantFamilyHistory(
            CompanyId companyId, VariantFamilyId id);
}
