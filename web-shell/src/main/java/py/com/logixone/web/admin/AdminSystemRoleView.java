package py.com.logixone.web.admin;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import py.com.logixone.kernel.application.security.admin.SystemRoleAdministrationView;
import py.com.logixone.kernel.domain.security.system.SystemRoleStatus;

public final class AdminSystemRoleView {

    private final String roleId;
    private final String code;
    private final String displayName;
    private final String statusLabel;
    private final long version;
    private final boolean active;
    private final List<AdminOptionView> assignedUsers;
    private final List<AdminPermissionView> grantedPermissions;

    private AdminSystemRoleView(
            String roleId, String code, String displayName, String statusLabel,
            long version, boolean active, List<AdminOptionView> assignedUsers,
            List<AdminPermissionView> grantedPermissions) {
        this.roleId = roleId;
        this.code = code;
        this.displayName = displayName;
        this.statusLabel = statusLabel;
        this.version = version;
        this.active = active;
        this.assignedUsers = assignedUsers;
        this.grantedPermissions = grantedPermissions;
    }

    static AdminSystemRoleView from(
            SystemRoleAdministrationView role,
            Map<String, String> userLabels) {
        Objects.requireNonNull(role, "role");
        boolean active = role.status() == SystemRoleStatus.ACTIVE;
        return new AdminSystemRoleView(
                role.roleId().toString(), role.code(), role.displayName(),
                active ? "Activo" : "Inactivo", role.version(), active,
                role.assignedUserIds().stream()
                        .map(userId -> new AdminOptionView(
                                userId.toString(), userLabels.getOrDefault(userId.toString(), userId.toString())))
                        .toList(),
                role.grantedPermissions().stream()
                        .map(permission -> new AdminPermissionView(permission.value(), true))
                        .toList());
    }

    public String getRoleId() { return roleId; }
    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public String getStatusLabel() { return statusLabel; }
    public long getVersion() { return version; }
    public boolean isActive() { return active; }
    public List<AdminOptionView> getAssignedUsers() { return assignedUsers; }
    public List<AdminPermissionView> getGrantedPermissions() { return grantedPermissions; }
}
