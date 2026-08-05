package py.com.logixone.web.admin;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import py.com.logixone.kernel.application.security.admin.MembershipAdministrationView;
import py.com.logixone.kernel.domain.security.MembershipStatus;

public final class AdminMembershipView {

    private final String userId;
    private final String userLabel;
    private final String statusLabel;
    private final long version;
    private final boolean active;
    private final List<AdminOptionView> assignedRoles;

    private AdminMembershipView(
            String userId,
            String userLabel,
            String statusLabel,
            long version,
            boolean active,
            List<AdminOptionView> assignedRoles) {
        this.userId = userId;
        this.userLabel = userLabel;
        this.statusLabel = statusLabel;
        this.version = version;
        this.active = active;
        this.assignedRoles = assignedRoles;
    }

    static AdminMembershipView from(
            MembershipAdministrationView membership,
            Map<String, String> roleLabels) {
        Objects.requireNonNull(membership, "membership");
        boolean active = membership.status() == MembershipStatus.ACTIVE;
        return new AdminMembershipView(
                membership.userId().toString(),
                membership.userLabel(),
                active ? "Activa" : "Inactiva",
                membership.version(),
                active,
                membership.assignedRoleIds().stream()
                        .map(roleId -> new AdminOptionView(
                                roleId.toString(), roleLabels.getOrDefault(roleId.toString(), roleId.toString())))
                        .toList());
    }

    public String getUserId() { return userId; }
    public String getUserLabel() { return userLabel; }
    public String getStatusLabel() { return statusLabel; }
    public long getVersion() { return version; }
    public boolean isActive() { return active; }
    public List<AdminOptionView> getAssignedRoles() { return assignedRoles; }
}
