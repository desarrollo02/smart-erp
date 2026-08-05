package py.com.logixone.web.admin;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import py.com.logixone.kernel.application.security.admin.CompanyRoleAdministrationView;
import py.com.logixone.kernel.domain.security.RoleStatus;

public final class AdminCompanyRoleView {

    private final String roleId;
    private final String code;
    private final String displayName;
    private final String statusLabel;
    private final long version;
    private final boolean active;
    private final List<AdminPermissionView> grantedPermissions;

    private AdminCompanyRoleView(
            String roleId, String code, String displayName, String statusLabel,
            long version, boolean active, List<AdminPermissionView> grantedPermissions) {
        this.roleId = roleId;
        this.code = code;
        this.displayName = displayName;
        this.statusLabel = statusLabel;
        this.version = version;
        this.active = active;
        this.grantedPermissions = grantedPermissions;
    }

    static AdminCompanyRoleView from(
            CompanyRoleAdministrationView role,
            Set<String> availablePermissions) {
        Objects.requireNonNull(role, "role");
        boolean active = role.status() == RoleStatus.ACTIVE;
        return new AdminCompanyRoleView(
                role.roleId().toString(), role.code(), role.displayName(),
                active ? "Activo" : "Inactivo", role.version(), active,
                role.grantedPermissions().stream()
                        .map(permission -> new AdminPermissionView(
                                permission.value(), availablePermissions.contains(permission.value())))
                        .toList());
    }

    public String getRoleId() { return roleId; }
    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public String getStatusLabel() { return statusLabel; }
    public long getVersion() { return version; }
    public boolean isActive() { return active; }
    public List<AdminPermissionView> getGrantedPermissions() { return grantedPermissions; }
}
