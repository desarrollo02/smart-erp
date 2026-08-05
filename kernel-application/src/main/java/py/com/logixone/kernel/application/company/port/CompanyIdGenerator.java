package py.com.logixone.kernel.application.company.port;

import py.com.logixone.kernel.api.company.CompanyId;

@FunctionalInterface
public interface CompanyIdGenerator {

    CompanyId nextId();
}
