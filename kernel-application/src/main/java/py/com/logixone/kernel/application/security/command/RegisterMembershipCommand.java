package py.com.logixone.kernel.application.security.command;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;

public record RegisterMembershipCommand(AppUserId userId, CompanyId companyId) {

    public RegisterMembershipCommand {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(companyId, "companyId");
    }
}
