package py.com.logixone.plugins.businesspartners.application.port;

import py.com.logixone.kernel.api.company.CompanyId;

/** Transactional counter; formatting and activation policy belong to application. */
public interface BusinessPartnerCodeSequenceRepository {

    long nextValue(CompanyId companyId, String sequenceScope);
}
