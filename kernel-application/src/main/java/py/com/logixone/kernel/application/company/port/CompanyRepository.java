package py.com.logixone.kernel.application.company.port;

import java.util.List;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.domain.company.Company;
import py.com.logixone.plugin.api.PluginId;

public interface CompanyRepository {

    List<Company> findAll();

    Optional<Company> findById(CompanyId companyId);

    default Optional<Company> findByCustomizationPluginId(PluginId customizationPluginId) {
        return Optional.empty();
    }

    /** Persists a new company or an idempotent/versioned replacement and returns stored state. */
    Company save(Company company);

    boolean isCustomizationAssignedToAnotherCompany(
            PluginId customizationPluginId,
            CompanyId companyId);
}
