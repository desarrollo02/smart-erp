package py.com.logixone.kernel.application.company.port;

import py.com.logixone.kernel.application.company.audit.CompanyAuditEvent;

@FunctionalInterface
public interface CompanyAuditPort {

    void record(CompanyAuditEvent event);
}
