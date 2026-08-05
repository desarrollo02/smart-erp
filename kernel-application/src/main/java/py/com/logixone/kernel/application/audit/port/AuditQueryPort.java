package py.com.logixone.kernel.application.audit.port;

import py.com.logixone.kernel.application.audit.admin.AuditPage;
import py.com.logixone.kernel.application.audit.admin.AuditQuery;

public interface AuditQueryPort {

    AuditPage query(AuditQuery query);
}
