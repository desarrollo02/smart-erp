package py.com.logixone.kernel.application.security.command;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.domain.security.RoleCode;

public record RegisterRoleCommand(
        CompanyId companyId,
        RoleCode roleCode,
        String displayName) {

    public RegisterRoleCommand {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(roleCode, "roleCode");
        Objects.requireNonNull(displayName, "displayName");
    }
}
