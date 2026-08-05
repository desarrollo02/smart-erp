package py.com.logixone.kernel.application.company.contribution;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.application.company.CompanyPluginQueryResult;
import py.com.logixone.kernel.application.company.CompanyPluginQueryService;

/** Neutral read service that projects only contributions effective for one company. */
public final class CompanyContributionService {

    private final CompanyPluginQueryService queryService;

    public CompanyContributionService(CompanyPluginQueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService, "queryService");
    }

    public CompanyContributions compose(CompanyId companyId) {
        Objects.requireNonNull(companyId, "companyId");
        CompanyPluginQueryResult query = queryService.resolve(companyId);
        if (!query.isFound()) {
            return CompanyContributions.notFound(
                    companyId,
                    query.failure().orElseThrow());
        }
        return CompanyContributions.fromResolution(query.resolution().orElseThrow());
    }
}
