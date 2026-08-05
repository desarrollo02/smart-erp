package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.application.company.port.CompanyIdGenerator;

@ApplicationScoped
public class UuidCompanyIdGenerator implements CompanyIdGenerator {

    @Override
    public CompanyId nextId() {
        return new CompanyId(UUID.randomUUID());
    }
}
