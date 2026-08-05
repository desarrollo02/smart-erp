package py.com.logixone.plugins.commercialcatalog.application.port;

import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.application.definition.CatalogDefinitions;
import py.com.logixone.plugins.commercialcatalog.domain.VariantFamilyId;

/**
 * Resolves and locks the current company-owned family while assigning it to an item.
 */
@FunctionalInterface
public interface VariantFamilyAssignmentRepository {

    Optional<CatalogDefinitions.VariantFamily> findCurrentForAssignment(
            CompanyId companyId, VariantFamilyId familyId);
}
