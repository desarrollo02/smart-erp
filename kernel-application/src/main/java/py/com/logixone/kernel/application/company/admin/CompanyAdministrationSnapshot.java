package py.com.logixone.kernel.application.company.admin;

import java.util.List;
import java.util.Objects;

/** Deterministic global snapshot for the company administration screens. */
public record CompanyAdministrationSnapshot(
        List<CompanySummaryView> companies,
        List<PluginCatalogView> physicalPlugins) {

    public CompanyAdministrationSnapshot {
        companies = List.copyOf(Objects.requireNonNull(companies, "companies"));
        physicalPlugins = List.copyOf(Objects.requireNonNull(physicalPlugins, "physicalPlugins"));
    }
}
