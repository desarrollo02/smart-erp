package py.com.logixone.kernel.application.security.admin;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.application.company.contribution.CompanyContributionService;
import py.com.logixone.kernel.application.company.contribution.CompanyContributions;
import py.com.logixone.kernel.application.company.port.CompanyRepository;
import py.com.logixone.kernel.application.security.port.AppUserRepository;
import py.com.logixone.kernel.application.security.port.CompanyAuthorizationRepository;
import py.com.logixone.kernel.application.security.port.CompanyMembershipRepository;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.MembershipRoleAssignment;
import py.com.logixone.kernel.domain.security.RolePermissionGrant;

/** Neutral, current-state administrative queries for company-owned security. */
public final class BusinessSecurityAdministrationQueryService {

    private final AppUserRepository userRepository;
    private final CompanyMembershipRepository membershipRepository;
    private final CompanyAuthorizationRepository authorizationRepository;
    private final CompanyRepository companyRepository;
    private final CompanyContributionService contributionService;

    public BusinessSecurityAdministrationQueryService(
            AppUserRepository userRepository,
            CompanyMembershipRepository membershipRepository,
            CompanyAuthorizationRepository authorizationRepository,
            CompanyRepository companyRepository,
            CompanyContributionService contributionService) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.membershipRepository = Objects.requireNonNull(membershipRepository, "membershipRepository");
        this.authorizationRepository = Objects.requireNonNull(authorizationRepository, "authorizationRepository");
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository");
        this.contributionService = Objects.requireNonNull(contributionService, "contributionService");
    }

    public BusinessSecuritySnapshot snapshot() {
        return new BusinessSecuritySnapshot(
                userRepository.findAll().stream()
                        .sorted((left, right) -> left.id().compareTo(right.id()))
                        .map(SecurityUserView::from)
                        .toList(),
                companyRepository.findAll().stream()
                        .sorted((left, right) -> left.id().compareTo(right.id()))
                        .map(SecurityCompanyView::from)
                        .toList());
    }

    public Optional<CompanySecurityAdministrationView> findCompany(CompanyId companyId) {
        Objects.requireNonNull(companyId, "companyId");
        var company = companyRepository.findById(companyId).orElse(null);
        if (company == null) {
            return Optional.empty();
        }
        Map<AppUserId, AppUser> users = userRepository.findAll().stream()
                .collect(Collectors.toUnmodifiableMap(AppUser::id, user -> user));
        List<MembershipRoleAssignment> assignments =
                authorizationRepository.findAssignmentsByCompanyId(companyId);
        List<RolePermissionGrant> grants = authorizationRepository.findPermissionGrants(companyId);

        List<MembershipAdministrationView> memberships = membershipRepository
                .findByCompanyId(companyId).stream()
                .map(membership -> {
                    AppUser user = users.get(membership.userId());
                    String label = user == null
                            ? membership.userId().toString()
                            : user.displayName().orElse(user.externalIdentity().subject());
                    return new MembershipAdministrationView(
                            membership.userId(),
                            label,
                            membership.status(),
                            membership.version(),
                            assignments.stream()
                                    .filter(assignment -> assignment.userId().equals(membership.userId()))
                                    .map(MembershipRoleAssignment::roleId)
                                    .sorted()
                                    .toList());
                })
                .toList();

        List<CompanyRoleAdministrationView> roles = authorizationRepository
                .findRolesByCompanyId(companyId).stream()
                .map(role -> new CompanyRoleAdministrationView(
                        role.id(),
                        role.code().value(),
                        role.displayName(),
                        role.status(),
                        role.version(),
                        grants.stream()
                                .filter(grant -> grant.roleId().equals(role.id()))
                                .map(RolePermissionGrant::permissionId)
                                .sorted()
                                .toList()))
                .toList();

        CompanyContributions contributions = contributionService.compose(companyId);
        return Optional.of(new CompanySecurityAdministrationView(
                SecurityCompanyView.from(company),
                contributions.operational(),
                contributions.permissions().stream().distinct().sorted().toList(),
                memberships,
                roles));
    }
}
