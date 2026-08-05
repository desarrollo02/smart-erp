package py.com.logixone.kernel.application.security.admin;

import java.util.List;
import java.util.Objects;
import py.com.logixone.kernel.api.security.SystemPermission;

public record SystemAuthorityAdministrationSnapshot(
        List<SecurityUserView> users,
        List<SystemRoleAdministrationView> roles,
        List<SystemPermission> knownPermissions) {

    public SystemAuthorityAdministrationSnapshot {
        users = List.copyOf(Objects.requireNonNull(users, "users"));
        roles = List.copyOf(Objects.requireNonNull(roles, "roles"));
        knownPermissions = List.copyOf(Objects.requireNonNull(knownPermissions, "knownPermissions"));
    }
}
