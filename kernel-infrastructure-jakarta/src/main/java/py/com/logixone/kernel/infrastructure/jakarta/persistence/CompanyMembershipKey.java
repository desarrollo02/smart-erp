package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;

@Embeddable
public class CompanyMembershipKey implements Serializable {

    @Column(name = "app_user_id", nullable = false, updatable = false)
    private UUID appUserId;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    protected CompanyMembershipKey() {
    }

    CompanyMembershipKey(AppUserId userId, CompanyId companyId) {
        appUserId = Objects.requireNonNull(userId, "userId").value();
        this.companyId = Objects.requireNonNull(companyId, "companyId").value();
    }

    AppUserId userId() {
        return new AppUserId(appUserId);
    }

    CompanyId companyId() {
        return new CompanyId(companyId);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof CompanyMembershipKey that
                && appUserId.equals(that.appUserId)
                && companyId.equals(that.companyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appUserId, companyId);
    }
}
