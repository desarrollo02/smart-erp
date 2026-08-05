package py.com.logixone.plugins.commercialcatalog.application.port;

import py.com.logixone.kernel.api.company.CompanyId;

/** Allocates gap-tolerant, strictly increasing catalog numbers inside one company and scope. */
public interface CatalogCodeSequenceRepository {
    long next(CompanyId companyId, String scope);
}
