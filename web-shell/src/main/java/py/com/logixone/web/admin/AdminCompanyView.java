package py.com.logixone.web.admin;

import java.util.Map;
import java.util.Objects;
import py.com.logixone.kernel.application.company.admin.CompanySummaryView;
import py.com.logixone.kernel.domain.company.CompanyStatus;

/** JSF-friendly immutable company row. */
public final class AdminCompanyView {

    private final String companyId;
    private final String status;
    private final String statusLabel;
    private final String customizationPluginId;
    private final String customizationName;
    private final long version;
    private final boolean active;

    private AdminCompanyView(
            String companyId,
            String status,
            String statusLabel,
            String customizationPluginId,
            String customizationName,
            long version,
            boolean active) {
        this.companyId = companyId;
        this.status = status;
        this.statusLabel = statusLabel;
        this.customizationPluginId = customizationPluginId;
        this.customizationName = customizationName;
        this.version = version;
        this.active = active;
    }

    static AdminCompanyView from(CompanySummaryView company, Map<String, String> pluginNames) {
        Objects.requireNonNull(company, "company");
        Objects.requireNonNull(pluginNames, "pluginNames");
        String customizationId = company.customizationPluginId().value();
        boolean active = company.status() == CompanyStatus.ACTIVE;
        return new AdminCompanyView(
                company.companyId().toString(),
                company.status().name(),
                active ? "Activa" : "Inactiva",
                customizationId,
                pluginNames.getOrDefault(customizationId, "Personalización no disponible"),
                company.version(),
                active);
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getCustomizationPluginId() {
        return customizationPluginId;
    }

    public String getCustomizationName() {
        return customizationName;
    }

    public long getVersion() {
        return version;
    }

    public boolean isActive() {
        return active;
    }
}
