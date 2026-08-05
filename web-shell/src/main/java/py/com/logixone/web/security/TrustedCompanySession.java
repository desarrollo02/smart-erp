package py.com.logixone.web.security;

import jakarta.enterprise.context.SessionScoped;
import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.kernel.api.security.CompanySessionReference;

/**
 * Minimal historical session state. Permissions, plugin contributions and OIDC
 * claims are deliberately never cached here.
 */
@SessionScoped
public class TrustedCompanySession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String userId;
    private String companyId;
    private long revision;

    public synchronized Optional<CompanySessionReference> reference() {
        if (userId == null || companyId == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new CompanySessionReference(
                    AppUserId.parse(userId), CompanyId.parse(companyId)));
        } catch (IllegalArgumentException invalidReference) {
            clear();
            return Optional.empty();
        }
    }

    public synchronized void bind(AuthenticatedCompanyContext context) {
        String nextUserId = context.actor().userId().toString();
        String nextCompanyId = context.companyId().toString();
        if (!nextUserId.equals(userId) || !nextCompanyId.equals(companyId)) {
            userId = nextUserId;
            companyId = nextCompanyId;
            revision++;
        }
    }

    public synchronized void clear() {
        if (userId != null || companyId != null) {
            userId = null;
            companyId = null;
            revision++;
        }
    }

    public synchronized long revision() {
        return revision;
    }
}
