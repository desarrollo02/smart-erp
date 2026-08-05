package py.com.logixone.kernel.application.company.admin;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.domain.company.Company;
import py.com.logixone.kernel.domain.company.CompanyStatus;
import py.com.logixone.plugin.api.PluginId;

/** Immutable administrative projection; it is not a persistence entity. */
public record CompanySummaryView(
        CompanyId companyId,
        CompanyStatus status,
        PluginId customizationPluginId,
        long version) {

    public CompanySummaryView {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(customizationPluginId, "customizationPluginId");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }

    public static CompanySummaryView from(Company company) {
        Objects.requireNonNull(company, "company");
        return new CompanySummaryView(
                company.id(), company.status(), company.customizationPluginId(), company.version());
    }
}
