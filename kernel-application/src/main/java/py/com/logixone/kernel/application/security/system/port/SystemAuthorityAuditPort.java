package py.com.logixone.kernel.application.security.system.port;

import py.com.logixone.kernel.application.security.system.audit.SystemAuthorityAuditEvent;

@FunctionalInterface
public interface SystemAuthorityAuditPort {

    void record(SystemAuthorityAuditEvent event);
}
