package py.com.logixone.kernel.application.security.port;

import py.com.logixone.kernel.application.security.audit.AccessAuditEvent;

@FunctionalInterface
public interface AccessAuditPort {

    void record(AccessAuditEvent event);
}
