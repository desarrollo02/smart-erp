package py.com.logixone.plugins.referencedata.application.policy;

import java.util.List;
import py.com.logixone.kernel.api.security.AuthorizedCompanyOperation;
import py.com.logixone.plugins.referencedata.api.ReferenceDataCatalog;

/** Inbound boundary for policy management from trusted server-side adapters. */
public interface ReferenceDataPolicyUseCases {

    ReferenceDataPolicyResult<ReferenceDataPolicy> current(
            AuthorizedCompanyOperation authorization, ReferenceDataCatalog catalog, String code);

    ReferenceDataPolicyResult<ReferenceDataPolicy> change(
            AuthorizedCompanyOperation authorization, ChangeReferenceDataPolicy command);

    ReferenceDataPolicyResult<List<ReferenceDataPolicyRevision>> history(
            AuthorizedCompanyOperation authorization, ReferenceDataCatalog catalog, String code);
}
