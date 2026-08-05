package py.com.logixone.kernel.application.security.system.port;

import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.system.access.SystemAuthorityAccess;
import py.com.logixone.kernel.domain.security.ExternalIdentity;

/** Current-state authorization used only after container authentication. */
public interface SystemAuthorityAccessPort {

    SystemAuthorityAccess authorizeAny(
            ExternalIdentity externalIdentity,
            String correlationId);

    SystemAuthorityAccess authorize(
            ExternalIdentity externalIdentity,
            SystemPermission requiredPermission,
            String correlationId);
}
