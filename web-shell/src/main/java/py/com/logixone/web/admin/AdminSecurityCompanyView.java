package py.com.logixone.web.admin;

import java.util.Objects;
import py.com.logixone.kernel.application.security.admin.SecurityCompanyView;
import py.com.logixone.kernel.domain.company.CompanyStatus;

public final class AdminSecurityCompanyView {

    private final String companyId;
    private final String statusLabel;
    private final boolean active;

    private AdminSecurityCompanyView(String companyId, String statusLabel, boolean active) {
        this.companyId = companyId;
        this.statusLabel = statusLabel;
        this.active = active;
    }

    static AdminSecurityCompanyView from(SecurityCompanyView company) {
        Objects.requireNonNull(company, "company");
        boolean active = company.status() == CompanyStatus.ACTIVE;
        return new AdminSecurityCompanyView(
                company.companyId().toString(), active ? "Activa" : "Inactiva", active);
    }

    public String getCompanyId() { return companyId; }
    public String getStatusLabel() { return statusLabel; }
    public boolean isActive() { return active; }
}
