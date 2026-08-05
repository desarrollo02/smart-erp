package py.com.logixone.kernel.domain.security.system;

import java.util.Objects;
import py.com.logixone.kernel.api.security.AppUserId;

/** Assignment of a kernel-wide role to a local application user. */
public record AppUserSystemRoleAssignment(
        AppUserId userId,
        SystemRoleId roleId) {

    public AppUserSystemRoleAssignment {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(roleId, "roleId");
    }
}
