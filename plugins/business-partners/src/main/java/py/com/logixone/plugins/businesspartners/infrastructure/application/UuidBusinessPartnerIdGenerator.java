package py.com.logixone.plugins.businesspartners.infrastructure.application;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerIdGenerator;

@ApplicationScoped
public class UuidBusinessPartnerIdGenerator implements BusinessPartnerIdGenerator {

    @Override
    public BusinessPartnerId nextId() {
        return new BusinessPartnerId(UUID.randomUUID());
    }
}
