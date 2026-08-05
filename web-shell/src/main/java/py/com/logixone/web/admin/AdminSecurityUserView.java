package py.com.logixone.web.admin;

import java.util.Objects;
import py.com.logixone.kernel.application.security.admin.SecurityUserView;
import py.com.logixone.kernel.domain.security.UserStatus;

public final class AdminSecurityUserView {

    private final String userId;
    private final String subject;
    private final String displayName;
    private final String statusLabel;
    private final long version;
    private final boolean active;

    private AdminSecurityUserView(
            String userId,
            String subject,
            String displayName,
            String statusLabel,
            long version,
            boolean active) {
        this.userId = userId;
        this.subject = subject;
        this.displayName = displayName;
        this.statusLabel = statusLabel;
        this.version = version;
        this.active = active;
    }

    static AdminSecurityUserView from(SecurityUserView user) {
        Objects.requireNonNull(user, "user");
        boolean active = user.status() == UserStatus.ACTIVE;
        return new AdminSecurityUserView(
                user.userId().toString(),
                user.subject(),
                user.displayName().orElse("Sin nombre de presentación"),
                active ? "Activo" : "Inactivo",
                user.version(),
                active);
    }

    public String getUserId() { return userId; }
    public String getSubject() { return subject; }
    public String getDisplayName() { return displayName; }
    public String getStatusLabel() { return statusLabel; }
    public long getVersion() { return version; }
    public boolean isActive() { return active; }
}
