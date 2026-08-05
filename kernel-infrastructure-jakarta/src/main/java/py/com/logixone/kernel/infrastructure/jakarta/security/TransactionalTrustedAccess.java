package py.com.logixone.kernel.infrastructure.jakarta.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.CompanySessionReference;
import py.com.logixone.kernel.application.company.CompanyPluginQueryService;
import py.com.logixone.kernel.application.company.contribution.CompanyContributionService;
import py.com.logixone.kernel.application.company.port.CompanyRepository;
import py.com.logixone.kernel.application.company.port.PluginActivationRepository;
import py.com.logixone.kernel.application.security.SecurityQueryService;
import py.com.logixone.kernel.application.security.access.OperationAuthorization;
import py.com.logixone.kernel.application.security.access.TrustedAccessService;
import py.com.logixone.kernel.application.security.access.TrustedCompanyAccess;
import py.com.logixone.kernel.application.security.access.TrustedNavigationAccess;
import py.com.logixone.kernel.application.security.access.TrustedScreenAccess;
import py.com.logixone.kernel.application.security.port.AccessAuditPort;
import py.com.logixone.kernel.application.security.port.AppUserRepository;
import py.com.logixone.kernel.application.security.port.CompanyAuthorizationRepository;
import py.com.logixone.kernel.application.security.port.CompanyMembershipRepository;
import py.com.logixone.kernel.application.security.port.TrustedAccessPort;
import py.com.logixone.kernel.domain.company.CompanyPluginResolver;
import py.com.logixone.kernel.domain.security.CompanyAccessPolicy;
import py.com.logixone.kernel.domain.security.EffectivePermissionPolicy;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.kernel.infrastructure.jakarta.plugin.CdiPluginCatalog;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.ScreenId;

/** Current-state JTA adapter used only after container OIDC authentication. */
@ApplicationScoped
@Transactional(rollbackOn = RuntimeException.class)
public class TransactionalTrustedAccess implements TrustedAccessPort {

    @Inject
    AppUserRepository userRepository;

    @Inject
    CompanyMembershipRepository membershipRepository;

    @Inject
    CompanyAuthorizationRepository authorizationRepository;

    @Inject
    CompanyRepository companyRepository;

    @Inject
    PluginActivationRepository activationRepository;

    @Inject
    CdiPluginCatalog pluginCatalog;

    @Inject
    AccessAuditPort accessAuditPort;

    @Override
    public TrustedCompanyAccess resolve(
            ExternalIdentity externalIdentity,
            Optional<CompanySessionReference> sessionReference,
            String correlationId) {
        return service().resolve(externalIdentity, sessionReference, correlationId);
    }

    @Override
    public TrustedCompanyAccess select(
            ExternalIdentity externalIdentity,
            CompanyId requestedCompanyId,
            String correlationId) {
        return service().select(externalIdentity, requestedCompanyId, correlationId);
    }

    @Override
    public OperationAuthorization authorize(
            ExternalIdentity externalIdentity,
            CompanySessionReference sessionReference,
            PluginId requiredPluginId,
            ContributionId requiredPermissionId,
            String correlationId) {
        return service().authorize(
                externalIdentity,
                sessionReference,
                requiredPluginId,
                requiredPermissionId,
                correlationId);
    }

    @Override
    public TrustedNavigationAccess navigation(
            ExternalIdentity externalIdentity,
            CompanySessionReference sessionReference,
            String correlationId) {
        return service().navigation(externalIdentity, sessionReference, correlationId);
    }

    @Override
    public TrustedScreenAccess screen(
            ExternalIdentity externalIdentity,
            CompanySessionReference sessionReference,
            ScreenId requestedScreenId,
            PluginId requiredPluginId,
            ContributionId requiredPermissionId,
            String correlationId) {
        return service().screen(
                externalIdentity,
                sessionReference,
                requestedScreenId,
                requiredPluginId,
                requiredPermissionId,
                correlationId);
    }

    private TrustedAccessService service() {
        SecurityQueryService securityQueries = new SecurityQueryService(
                userRepository,
                membershipRepository,
                authorizationRepository,
                new CompanyAccessPolicy(),
                new EffectivePermissionPolicy());
        CompanyPluginQueryService companyQueries = new CompanyPluginQueryService(
                companyRepository,
                activationRepository,
                pluginCatalog.registry(),
                new CompanyPluginResolver());
        return new TrustedAccessService(
                securityQueries,
                new CompanyContributionService(companyQueries),
                accessAuditPort,
                Clock.systemUTC());
    }
}
