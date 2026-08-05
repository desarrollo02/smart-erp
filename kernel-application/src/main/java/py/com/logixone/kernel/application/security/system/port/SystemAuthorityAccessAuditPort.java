package py.com.logixone.kernel.application.security.system.port;

import py.com.logixone.kernel.application.security.system.audit.SystemAuthorityAccessAuditEvent;

@FunctionalInterface
public interface SystemAuthorityAccessAuditPort {

    void record(SystemAuthorityAccessAuditEvent event);
}
