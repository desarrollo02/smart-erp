package py.com.logixone.web.security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.system.access.SystemAuthorityAccess;
import py.com.logixone.kernel.application.security.system.access.SystemAuthorityContext;
import py.com.logixone.kernel.application.security.system.port.SystemAuthorityAccessPort;
import py.com.logixone.kernel.domain.security.ExternalIdentity;

/** Request-only boundary for global authorization after container OIDC authentication. */
@RequestScoped
public class TrustedAdminWebAccess {

    @Inject
    ValidatedOidcPrincipal oidcPrincipal;

    @Inject
    SystemAuthorityAccessPort accessPort;

    @Inject
    RequestCorrelation correlation;

    private SystemAuthorityContext currentContext;

    public SystemAuthorityContext requireAny() {
        if (currentContext != null) {
            return currentContext;
        }
        ExternalIdentity identity = requiredIdentity();
        SystemAuthorityAccess access = accessPort.authorizeAny(identity, correlation.value());
        currentContext = access.context().orElseThrow(TrustedWebAccessException::forbidden);
        return currentContext;
    }

    public SystemAuthorityContext require(SystemPermission permission) {
        ExternalIdentity identity = requiredIdentity();
        SystemAuthorityAccess access = accessPort.authorize(
                identity, permission, correlation.value());
        currentContext = access.context().orElseThrow(TrustedWebAccessException::forbidden);
        return currentContext;
    }

    private ExternalIdentity requiredIdentity() {
        Optional<ExternalIdentity> identity = oidcPrincipal.currentIdentity();
        if (identity.isEmpty()) {
            throw TrustedWebAccessException.unauthorized();
        }
        return identity.orElseThrow();
    }
}
