package py.com.logixone.kernel.application.security.port;

import py.com.logixone.kernel.application.security.audit.SecurityAuditEvent;

@FunctionalInterface
public interface SecurityAuditPort {

    void record(SecurityAuditEvent event);
}
