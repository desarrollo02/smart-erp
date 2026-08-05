package py.com.logixone.kernel.application.security.admin;

import java.util.List;
import java.util.Objects;
import py.com.logixone.plugin.api.ContributionId;

public record CompanySecurityAdministrationView(
        SecurityCompanyView company,
        boolean operational,
        List<ContributionId> availablePermissions,
        List<MembershipAdministrationView> memberships,
        List<CompanyRoleAdministrationView> roles) {

    public CompanySecurityAdministrationView {
        Objects.requireNonNull(company, "company");
        availablePermissions = List.copyOf(Objects.requireNonNull(availablePermissions, "availablePermissions"));
        memberships = List.copyOf(Objects.requireNonNull(memberships, "memberships"));
        roles = List.copyOf(Objects.requireNonNull(roles, "roles"));
    }
}
