package py.com.logixone.kernel.infrastructure.jakarta.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Collection;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.application.security.SecurityQueryService;
import py.com.logixone.kernel.application.security.port.AppUserRepository;
import py.com.logixone.kernel.application.security.port.CompanyAuthorizationRepository;
import py.com.logixone.kernel.application.security.port.CompanyMembershipRepository;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.CompanyAccessPolicy;
import py.com.logixone.kernel.domain.security.CompanySelectionResolution;
import py.com.logixone.kernel.domain.security.EffectivePermissionPolicy;
import py.com.logixone.kernel.domain.security.EffectivePermissionResolution;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.plugin.api.ContributionId;

/** Recomputes membership and permissions from current database state on every call. */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class TransactionalSecurityQueries {

    @Inject
    AppUserRepository userRepository;

    @Inject
    CompanyMembershipRepository membershipRepository;

    @Inject
    CompanyAuthorizationRepository authorizationRepository;

    public Optional<AppUser> findByExternalIdentity(ExternalIdentity externalIdentity) {
        return service().findByExternalIdentity(externalIdentity);
    }

    public CompanySelectionResolution resolveCompanies(
            ExternalIdentity externalIdentity,
            Optional<CompanyId> requestedCompanyId) {
        return service().resolveCompanies(externalIdentity, requestedCompanyId);
    }

    public EffectivePermissionResolution resolveEffectivePermissions(
            AppUserId userId,
            CompanyId companyId,
            Collection<ContributionId> availablePermissions) {
        return service().resolveEffectivePermissions(userId, companyId, availablePermissions);
    }

    private SecurityQueryService service() {
        return new SecurityQueryService(
                userRepository,
                membershipRepository,
                authorizationRepository,
                new CompanyAccessPolicy(),
                new EffectivePermissionPolicy());
    }
}
