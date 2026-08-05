package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;

@Embeddable
public class AppUserSystemRoleKey implements Serializable {

    @Column(name = "app_user_id", nullable = false, updatable = false)
    private UUID appUserId;

    @Column(name = "system_role_id", nullable = false, updatable = false)
    private UUID systemRoleId;

    protected AppUserSystemRoleKey() {
    }

    AppUserSystemRoleKey(AppUserId userId, SystemRoleId roleId) {
        appUserId = Objects.requireNonNull(userId, "userId").value();
        systemRoleId = Objects.requireNonNull(roleId, "roleId").value();
    }

    AppUserId userId() {
        return new AppUserId(appUserId);
    }

    SystemRoleId roleId() {
        return new SystemRoleId(systemRoleId);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof AppUserSystemRoleKey that
                && appUserId.equals(that.appUserId)
                && systemRoleId.equals(that.systemRoleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appUserId, systemRoleId);
    }
}
