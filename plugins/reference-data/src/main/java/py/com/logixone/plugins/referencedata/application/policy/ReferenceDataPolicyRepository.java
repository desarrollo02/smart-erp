package py.com.logixone.plugins.referencedata.application.policy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.plugins.referencedata.api.ReferenceDataCatalog;

/** Plugin-private persistence port; implementations never query another schema. */
public interface ReferenceDataPolicyRepository {

    boolean existsInCurrentRelease(ReferenceDataCatalog catalog, String code);

    Optional<ReferenceDataPolicy> find(
            CompanyId companyId, ReferenceDataCatalog catalog, String code);

    ReferenceDataPolicy change(
            CompanyId companyId,
            ChangeReferenceDataPolicy command,
            AppUserId actorUserId,
            String correlationId,
            Instant changedAt);

    List<ReferenceDataPolicyRevision> history(
            CompanyId companyId, ReferenceDataCatalog catalog, String code);
}
