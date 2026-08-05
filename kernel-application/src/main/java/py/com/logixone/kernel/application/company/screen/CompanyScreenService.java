package py.com.logixone.kernel.application.company.screen;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.application.company.contribution.CompanyContributionService;

/** Company-facing service that resolves effective plugins before composing screen overlays. */
public final class CompanyScreenService {

    private final CompanyContributionService contributionService;
    private final CompanyScreenComposer composer;

    public CompanyScreenService(
            CompanyContributionService contributionService,
            CompanyScreenComposer composer) {
        this.contributionService = Objects.requireNonNull(contributionService, "contributionService");
        this.composer = Objects.requireNonNull(composer, "composer");
    }

    public CompanyScreenComposition compose(CompanyId companyId) {
        Objects.requireNonNull(companyId, "companyId");
        return composer.compose(contributionService.compose(companyId));
    }
}
