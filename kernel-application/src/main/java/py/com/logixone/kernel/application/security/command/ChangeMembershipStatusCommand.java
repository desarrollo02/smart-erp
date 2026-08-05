package py.com.logixone.kernel.application.security.command;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.domain.security.MembershipStatus;

public record ChangeMembershipStatusCommand(
        AppUserId userId,
        CompanyId companyId,
        MembershipStatus desiredStatus,
        long expectedVersion) {

    public ChangeMembershipStatusCommand {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(desiredStatus, "desiredStatus");
        if (expectedVersion < 0) {
            throw new IllegalArgumentException("expectedVersion must not be negative");
        }
    }
}
