package py.com.logixone.kernel.application.security;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.application.security.port.AppUserRepository;
import py.com.logixone.kernel.application.security.port.CompanyAuthorizationRepository;
import py.com.logixone.kernel.application.security.port.CompanyMembershipRepository;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.CompanyAccessPolicy;
import py.com.logixone.kernel.domain.security.CompanyMembership;
import py.com.logixone.kernel.domain.security.CompanySelectionResolution;
import py.com.logixone.kernel.domain.security.EffectivePermissionPolicy;
import py.com.logixone.kernel.domain.security.EffectivePermissionResolution;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.kernel.domain.security.SecurityDiagnosticCode;
import py.com.logixone.plugin.api.ContributionId;

/** Current-state queries; no membership or permission is cached across operations. */
public final class SecurityQueryService {

    private final AppUserRepository userRepository;
    private final CompanyMembershipRepository membershipRepository;
    private final CompanyAuthorizationRepository authorizationRepository;
    private final CompanyAccessPolicy companyAccessPolicy;
    private final EffectivePermissionPolicy permissionPolicy;

    public SecurityQueryService(
            AppUserRepository userRepository,
            CompanyMembershipRepository membershipRepository,
            CompanyAuthorizationRepository authorizationRepository,
            CompanyAccessPolicy companyAccessPolicy,
            EffectivePermissionPolicy permissionPolicy) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.membershipRepository = Objects.requireNonNull(
                membershipRepository, "membershipRepository");
        this.authorizationRepository = Objects.requireNonNull(
                authorizationRepository, "authorizationRepository");
        this.companyAccessPolicy = Objects.requireNonNull(companyAccessPolicy, "companyAccessPolicy");
        this.permissionPolicy = Objects.requireNonNull(permissionPolicy, "permissionPolicy");
    }

    public Optional<AppUser> findByExternalIdentity(ExternalIdentity externalIdentity) {
        return userRepository.findByExternalIdentity(
                Objects.requireNonNull(externalIdentity, "externalIdentity"));
    }

    public CompanySelectionResolution resolveCompanies(
            ExternalIdentity externalIdentity,
            Optional<CompanyId> requestedCompanyId) {
        Objects.requireNonNull(externalIdentity, "externalIdentity");
        Objects.requireNonNull(requestedCompanyId, "requestedCompanyId");
        AppUser user = userRepository.findByExternalIdentity(externalIdentity).orElse(null);
        if (user == null) {
            return CompanySelectionResolution.denied(SecurityDiagnosticCode.USER_NOT_REGISTERED);
        }
        return companyAccessPolicy.resolve(
                user,
                membershipRepository.findByUserId(user.id()),
                requestedCompanyId);
    }

    public EffectivePermissionResolution resolveEffectivePermissions(
            AppUserId userId,
            CompanyId companyId,
            Collection<ContributionId> availablePermissions) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(availablePermissions, "availablePermissions");
        AppUser user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return EffectivePermissionResolution.denied(
                    companyId, SecurityDiagnosticCode.USER_NOT_REGISTERED);
        }
        CompanyMembership membership = membershipRepository.findByUserAndCompany(
                userId, companyId).orElse(null);
        if (membership == null) {
            return EffectivePermissionResolution.denied(
                    companyId, SecurityDiagnosticCode.MEMBERSHIP_REQUIRED);
        }
        return permissionPolicy.resolve(
                user,
                membership,
                authorizationRepository.findRolesByCompanyId(companyId),
                authorizationRepository.findAssignments(userId, companyId),
                authorizationRepository.findPermissionGrants(companyId),
                availablePermissions);
    }
}
