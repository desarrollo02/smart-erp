package py.com.logixone.plugins.businesspartners.application.port;

import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;

@FunctionalInterface
public interface BusinessPartnerIdGenerator {

    BusinessPartnerId nextId();
}
