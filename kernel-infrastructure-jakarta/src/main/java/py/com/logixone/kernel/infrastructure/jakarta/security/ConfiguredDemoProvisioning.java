package py.com.logixone.kernel.infrastructure.jakarta.security;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.application.company.CompanyOperationResult;
import py.com.logixone.kernel.application.company.command.ChangeCompanyStatusCommand;
import py.com.logixone.kernel.application.company.command.ChangePluginActivationCommand;
import py.com.logixone.kernel.application.company.command.RegisterCompanyCommand;
import py.com.logixone.kernel.application.company.port.CompanyRepository;
import py.com.logixone.kernel.application.security.SecurityOperationResult;
import py.com.logixone.kernel.application.security.command.AssignRoleCommand;
import py.com.logixone.kernel.application.security.command.ChangeAppUserStatusCommand;
import py.com.logixone.kernel.application.security.command.ChangeMembershipStatusCommand;
import py.com.logixone.kernel.application.security.command.ChangeRoleStatusCommand;
import py.com.logixone.kernel.application.security.command.GrantPermissionCommand;
import py.com.logixone.kernel.application.security.command.RegisterAppUserCommand;
import py.com.logixone.kernel.application.security.command.RegisterMembershipCommand;
import py.com.logixone.kernel.application.security.command.RegisterRoleCommand;
import py.com.logixone.kernel.application.security.port.AppUserRepository;
import py.com.logixone.kernel.application.security.port.CompanyAuthorizationRepository;
import py.com.logixone.kernel.application.security.port.CompanyMembershipRepository;
import py.com.logixone.kernel.domain.company.Company;
import py.com.logixone.kernel.domain.company.CompanyStatus;
import py.com.logixone.kernel.domain.company.PluginActivationState;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.CompanyMembership;
import py.com.logixone.kernel.domain.security.CompanyRole;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.kernel.domain.security.MembershipStatus;
import py.com.logixone.kernel.domain.security.RoleCode;
import py.com.logixone.kernel.domain.security.RoleStatus;
import py.com.logixone.kernel.domain.security.UserStatus;
import py.com.logixone.kernel.infrastructure.jakarta.company.TransactionalCompanyUseCases;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;

/** Closed, opt-in provisioning for the fictitious Sprint 3 visual demo. */
@ApplicationScoped
public class ConfiguredDemoProvisioning {

    private static final Logger LOGGER = System.getLogger(ConfiguredDemoProvisioning.class.getName());
    private static final String ENABLED = "LOGIXONE_DEMO_PROVISIONING_ENABLED";
    private static final PluginId FUNCTIONAL_PLUGIN = new PluginId("reference_plugin");
    private static final PluginId CUSTOMIZATION_A = new PluginId("reference_custom_a");
    private static final PluginId CUSTOMIZATION_B = new PluginId("reference_custom_b");
    private static final ContributionId DASHBOARD_PERMISSION =
            new ContributionId("reference.dashboard.view");
    private static final RoleCode DEMO_ROLE = new RoleCode("demo_operator");
    private static final String DEMO_ROLE_NAME = "Operador de demostración técnica";

    @Inject
    TransactionalCompanyUseCases companyUseCases;

    @Inject
    TransactionalSecurityUseCases securityUseCases;

    @Inject
    CompanyRepository companyRepository;

    @Inject
    AppUserRepository userRepository;

    @Inject
    CompanyMembershipRepository membershipRepository;

    @Inject
    CompanyAuthorizationRepository authorizationRepository;

    void initialize(
            @Observes @Priority(3000) @Initialized(ApplicationScoped.class)
            Object initializationEvent) {
        Objects.requireNonNull(initializationEvent, "initializationEvent");
        DemoDeclaration declaration;
        try {
            declaration = DemoDeclaration.from(System.getenv());
        } catch (RuntimeException failure) {
            LOGGER.log(Level.ERROR, "event=demo_provisioning_failed type=InvalidConfiguration");
            throw new IllegalStateException("Demo provisioning configuration is invalid", failure);
        }
        if (!declaration.enabled()) {
            LOGGER.log(Level.INFO, "event=demo_provisioning_skipped reason=Disabled");
            return;
        }

        try {
            Company companyA = ensureCompany(CUSTOMIZATION_A);
            Company companyB = ensureCompany(CUSTOMIZATION_B);

            AppUser noCompany = ensureUser(
                    declaration.issuer(), declaration.noCompanySubject(), "Demo sin empresa");
            requireExactMemberships(noCompany, Set.of());

            AppUser singleCompany = ensureUser(
                    declaration.issuer(), declaration.singleCompanySubject(), "Demo empresa A");
            ensureAuthorizedMembership(singleCompany, companyA);
            requireExactMemberships(singleCompany, Set.of(companyA.id()));

            AppUser multipleCompanies = ensureUser(
                    declaration.issuer(), declaration.multipleCompaniesSubject(), "Demo empresas A y B");
            ensureAuthorizedMembership(multipleCompanies, companyA);
            ensureAuthorizedMembership(multipleCompanies, companyB);
            requireExactMemberships(multipleCompanies, Set.of(companyA.id(), companyB.id()));

            LOGGER.log(
                    Level.INFO,
                    "event=demo_provisioning_completed company_count=2 user_count=3"
                            + " company_a_id=" + companyA.id()
                            + " company_b_id=" + companyB.id());
        } catch (RuntimeException failure) {
            LOGGER.log(
                    Level.ERROR,
                    "event=demo_provisioning_failed type=" + failure.getClass().getSimpleName());
            throw new IllegalStateException("Demo provisioning failed", failure);
        }
    }

    private Company ensureCompany(PluginId customizationPluginId) {
        Company company = companyRepository.findByCustomizationPluginId(customizationPluginId)
                .orElseGet(() -> companyValue(
                        companyUseCases.register(new RegisterCompanyCommand(customizationPluginId)),
                        "register company"));

        companyValue(
                companyUseCases.changeActivation(new ChangePluginActivationCommand(
                        company.id(), FUNCTIONAL_PLUGIN, PluginActivationState.ENABLED, 0)),
                "enable functional plugin");

        Company current = companyRepository.findById(company.id()).orElseThrow();
        if (current.status() != CompanyStatus.ACTIVE) {
            current = companyValue(
                    companyUseCases.changeStatus(new ChangeCompanyStatusCommand(
                            current.id(), CompanyStatus.ACTIVE, current.version())),
                    "activate company");
        }
        if (!current.customizationPluginId().equals(customizationPluginId)) {
            throw new IllegalStateException("Demo company customization is incompatible");
        }
        return current;
    }

    private AppUser ensureUser(String issuer, String subject, String displayName) {
        ExternalIdentity identity = new ExternalIdentity(issuer, subject);
        AppUser user = userRepository.findByExternalIdentity(identity)
                .orElseGet(() -> securityValue(
                        securityUseCases.registerUser(new RegisterAppUserCommand(
                                identity, Optional.of(displayName))),
                        "register demo user"));
        if (!user.displayName().equals(Optional.of(displayName))) {
            throw new IllegalStateException("Demo user display name is incompatible");
        }
        if (user.status() != UserStatus.ACTIVE) {
            user = securityValue(
                    securityUseCases.changeUserStatus(new ChangeAppUserStatusCommand(
                            user.id(), UserStatus.ACTIVE, user.version())),
                    "activate demo user");
        }
        return user;
    }

    private void ensureAuthorizedMembership(AppUser user, Company company) {
        CompanyMembership membership = membershipRepository.findByUserAndCompany(
                        user.id(), company.id())
                .orElseGet(() -> securityValue(
                        securityUseCases.registerMembership(new RegisterMembershipCommand(
                                user.id(), company.id())),
                        "register demo membership"));
        if (membership.status() != MembershipStatus.ACTIVE) {
            membership = securityValue(
                    securityUseCases.changeMembershipStatus(new ChangeMembershipStatusCommand(
                            membership.userId(), membership.companyId(),
                            MembershipStatus.ACTIVE, membership.version())),
                    "activate demo membership");
        }

        CompanyRole role = authorizationRepository.findRoleByCompanyAndCode(
                        company.id(), DEMO_ROLE)
                .orElseGet(() -> securityValue(
                        securityUseCases.registerRole(new RegisterRoleCommand(
                                company.id(), DEMO_ROLE, DEMO_ROLE_NAME)),
                        "register demo role"));
        if (!role.displayName().equals(DEMO_ROLE_NAME)) {
            throw new IllegalStateException("Demo role is incompatible");
        }
        if (role.status() != RoleStatus.ACTIVE) {
            role = securityValue(
                    securityUseCases.changeRoleStatus(new ChangeRoleStatusCommand(
                            role.id(), company.id(), RoleStatus.ACTIVE, role.version())),
                    "activate demo role");
        }

        securityValue(
                securityUseCases.grantPermission(new GrantPermissionCommand(
                        company.id(), role.id(), DASHBOARD_PERMISSION)),
                "grant demo permission");
        securityValue(
                securityUseCases.assignRole(new AssignRoleCommand(
                        user.id(), company.id(), role.id())),
                "assign demo role");
    }

    private void requireExactMemberships(AppUser user, Set<CompanyId> expectedCompanyIds) {
        Set<CompanyId> actualCompanyIds = membershipRepository.findByUserId(user.id()).stream()
                .map(CompanyMembership::companyId)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        if (!actualCompanyIds.equals(new TreeSet<>(expectedCompanyIds))) {
            throw new IllegalStateException("Demo user memberships are incompatible");
        }
    }

    private static <T> T companyValue(CompanyOperationResult<T> result, String operation) {
        if (result.failure().isPresent()) {
            throw new IllegalStateException(operation + " rejected: " + result.failure().orElseThrow());
        }
        return result.value().orElseThrow();
    }

    private static <T> T securityValue(SecurityOperationResult<T> result, String operation) {
        if (result.failure().isPresent()) {
            throw new IllegalStateException(operation + " rejected: " + result.failure().orElseThrow());
        }
        return result.value().orElseThrow();
    }

    private record DemoDeclaration(
            boolean enabled,
            String issuer,
            String noCompanySubject,
            String singleCompanySubject,
            String multipleCompaniesSubject) {

        private DemoDeclaration {
            if (enabled) {
                Objects.requireNonNull(issuer, "issuer");
                Objects.requireNonNull(noCompanySubject, "noCompanySubject");
                Objects.requireNonNull(singleCompanySubject, "singleCompanySubject");
                Objects.requireNonNull(multipleCompaniesSubject, "multipleCompaniesSubject");
            }
        }

        static DemoDeclaration from(Map<String, String> environment) {
            Objects.requireNonNull(environment, "environment");
            String configured = environment.getOrDefault(ENABLED, "false");
            if ("false".equals(configured)) {
                return new DemoDeclaration(false, null, null, null, null);
            }
            if (!"true".equals(configured)) {
                throw new IllegalArgumentException(ENABLED + " must be true or false");
            }
            String noCompany = required(environment, "LOGIXONE_DEMO_SUBJECT_NO_COMPANY");
            String singleCompany = required(environment, "LOGIXONE_DEMO_SUBJECT_SINGLE_COMPANY");
            String multipleCompanies = required(environment, "LOGIXONE_DEMO_SUBJECT_MULTIPLE_COMPANIES");
            if (Set.of(noCompany, singleCompany, multipleCompanies).size() != 3) {
                throw new IllegalArgumentException("Demo subjects must be different");
            }
            return new DemoDeclaration(
                    true,
                    required(environment, "LOGIXONE_OIDC_PROVIDER_URL"),
                    noCompany,
                    singleCompany,
                    multipleCompanies);
        }

        private static String required(Map<String, String> environment, String name) {
            String value = environment.get(name);
            if (value == null || value.isBlank() || !value.equals(value.strip())) {
                throw new IllegalArgumentException(name + " must contain an exact non-blank value");
            }
            return value;
        }
    }
}
