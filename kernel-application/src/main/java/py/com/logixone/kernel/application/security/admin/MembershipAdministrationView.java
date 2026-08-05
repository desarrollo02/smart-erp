package py.com.logixone.kernel.application.security.admin;

import java.util.List;
import java.util.Objects;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.domain.security.MembershipStatus;
import py.com.logixone.kernel.domain.security.RoleId;

public record MembershipAdministrationView(
        AppUserId userId,
        String userLabel,
        MembershipStatus status,
        long version,
        List<RoleId> assignedRoleIds) {

    public MembershipAdministrationView {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(userLabel, "userLabel");
        Objects.requireNonNull(status, "status");
        assignedRoleIds = List.copyOf(Objects.requireNonNull(assignedRoleIds, "assignedRoleIds"));
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
