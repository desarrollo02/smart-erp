package py.com.logixone.kernel.application.security.admin;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.domain.company.Company;
import py.com.logixone.kernel.domain.company.CompanyStatus;

public record SecurityCompanyView(CompanyId companyId, CompanyStatus status) {

    public SecurityCompanyView {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(status, "status");
    }

    public static SecurityCompanyView from(Company company) {
        Objects.requireNonNull(company, "company");
        return new SecurityCompanyView(company.id(), company.status());
    }
}
