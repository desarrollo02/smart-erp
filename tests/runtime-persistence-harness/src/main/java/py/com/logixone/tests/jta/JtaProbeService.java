package py.com.logixone.tests.jta;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.company.CompanyAdministrationService;
import py.com.logixone.kernel.application.company.CompanyOperationResult;
import py.com.logixone.kernel.application.company.CompanyOperationStatus;
import py.com.logixone.kernel.application.company.CompanyPluginQueryService;
import py.com.logixone.kernel.application.company.PluginActivationService;
import py.com.logixone.kernel.application.company.contribution.CompanyContributionService;
import py.com.logixone.kernel.application.company.contribution.CompanyContributions;
import py.com.logixone.kernel.application.company.screen.CompanyScreenComposer;
import py.com.logixone.kernel.application.company.screen.CompanyScreenComposition;
import py.com.logixone.kernel.application.company.screen.CompanyScreenService;
import py.com.logixone.kernel.application.company.audit.CompanyAuditActor;
import py.com.logixone.kernel.application.company.command.ChangeCompanyStatusCommand;
import py.com.logixone.kernel.application.company.command.ChangePluginActivationCommand;
import py.com.logixone.kernel.application.company.command.RegisterCompanyCommand;
import py.com.logixone.kernel.application.company.port.CompanyAuditPort;
import py.com.logixone.kernel.application.company.port.CompanyRepository;
import py.com.logixone.kernel.application.company.port.PluginActivationRepository;
import py.com.logixone.kernel.application.plugin.PluginRegistry;
import py.com.logixone.kernel.application.security.SecurityOperationResult;
import py.com.logixone.kernel.application.security.port.AppUserRepository;
import py.com.logixone.kernel.application.security.system.SystemAuthorityBootstrapService;
import py.com.logixone.kernel.application.security.system.SystemAuthorityBootstrapState;
import py.com.logixone.kernel.application.security.system.audit.SystemAuthorityAuditOperation;
import py.com.logixone.kernel.application.security.system.command.BootstrapSystemAuthorityCommand;
import py.com.logixone.kernel.application.security.system.port.SystemAuthorityAuditPort;
import py.com.logixone.kernel.application.security.system.port.SystemAuthorityRepository;
import py.com.logixone.kernel.domain.company.Company;
import py.com.logixone.kernel.domain.company.CompanyPluginResolver;
import py.com.logixone.kernel.domain.company.CompanyStatus;
import py.com.logixone.kernel.domain.company.PluginActivationDecision;
import py.com.logixone.kernel.domain.company.PluginActivationPolicy;
import py.com.logixone.kernel.domain.company.PluginActivationState;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.kernel.domain.security.system.SystemRoleCode;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;
import py.com.logixone.kernel.infrastructure.jakarta.plugin.CdiPluginCatalog;
import py.com.logixone.kernel.infrastructure.jakarta.persistence.CorePersistenceNames;
import py.com.logixone.plugin.api.PluginId;

@Dependent
class JtaProbeService {

    private static final PluginId FUNCTIONAL_PLUGIN_ID = new PluginId("jta_functional");
    private static final PluginId CUSTOMIZATION_A = new PluginId("jta_custom_a");
    private static final PluginId CUSTOMIZATION_B = new PluginId("jta_custom_b");
    private static final PluginId ROLLBACK_CUSTOMIZATION = new PluginId("jta_custom_rollback");

    private final CompanyRepository companies;
    private final PluginActivationRepository activations;
    private final CompanyAuditPort audit;
    private final CdiPluginCatalog catalog;
    private final AppUserRepository users;
    private final SystemAuthorityRepository systemAuthority;
    private final SystemAuthorityAuditPort systemAuthorityAudit;

    @PersistenceContext(unitName = CorePersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    @Inject
    JtaProbeService(
            CompanyRepository companies,
            PluginActivationRepository activations,
            CompanyAuditPort audit,
            CdiPluginCatalog catalog,
            AppUserRepository users,
            SystemAuthorityRepository systemAuthority,
            SystemAuthorityAuditPort systemAuthorityAudit) {
        this.companies = companies;
        this.activations = activations;
        this.audit = audit;
        this.catalog = catalog;
        this.users = users;
        this.systemAuthority = systemAuthority;
        this.systemAuthorityAudit = systemAuthorityAudit;
    }

    @Transactional
    public void commit(CompanyId companyId) {
        writeCompanyAndActivation(companyId);
    }

    @Transactional
    public void rollback(CompanyId companyId) {
        writeCompanyAndActivation(companyId);
        throw new ExpectedRollbackException();
    }

    @Transactional
    public void reset() {
        entityManager.createNativeQuery("""
                        DELETE FROM core.app_user_system_role
                        WHERE system_role_id IN (
                            SELECT system_role_id
                            FROM core.system_role
                            WHERE starts_with(role_code, 'jta.system_admin.')
                        ) OR app_user_id IN (
                            SELECT app_user_id
                            FROM core.app_user
                            WHERE issuer = 'https://jta-probe.invalid/realms/logixone'
                        )
                        """)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        DELETE FROM core.system_role_permission
                        WHERE system_role_id IN (
                            SELECT system_role_id
                            FROM core.system_role
                            WHERE starts_with(role_code, 'jta.system_admin.')
                        )
                        """)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        DELETE FROM core.system_role
                        WHERE starts_with(role_code, 'jta.system_admin.')
                        """)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        DELETE FROM core.app_user
                        WHERE issuer = 'https://jta-probe.invalid/realms/logixone'
                        """)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        DELETE FROM core.company_plugin_activation
                        WHERE company_id IN (
                            SELECT company_id
                            FROM core.company
                            WHERE customization_plugin_id IN (
                                'jta_custom_a', 'jta_custom_b', 'jta_custom_rollback'
                            ) OR starts_with(customization_plugin_id, 'custom_probe_')
                        )
                        """)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        DELETE FROM core.company
                        WHERE customization_plugin_id IN (
                            'jta_custom_a', 'jta_custom_b', 'jta_custom_rollback'
                        ) OR starts_with(customization_plugin_id, 'custom_probe_')
                        """)
                .executeUpdate();
    }

    @Transactional
    public ApplicationProbeResult applicationCommitA(CompanyId companyId) {
        return applicationCommit(companyId, CUSTOMIZATION_A, true);
    }

    @Transactional
    public ApplicationProbeResult applicationCommitB(CompanyId companyId) {
        return applicationCommit(companyId, CUSTOMIZATION_B, true);
    }

    @Transactional
    public void applicationRollback(CompanyId companyId) {
        CompanyAdministrationService administration = administration(
                companyId,
                event -> {
                    throw new ExpectedRollbackException();
                });
        administration.register(new RegisterCompanyCommand(ROLLBACK_CUSTOMIZATION));
        throw new IllegalStateException("failing audit unexpectedly returned");
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<String> effectivePlugins(CompanyId companyId) {
        return new CompanyPluginQueryService(
                companies,
                activations,
                registry(),
                new CompanyPluginResolver())
                .resolve(companyId)
                .resolution()
                .orElseThrow()
                .orderedPlugins()
                .stream()
                .map(descriptor -> descriptor.id().value())
                .toList();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ContributionProbeResult contributions(CompanyId companyId) {
        CompanyPluginQueryService query = new CompanyPluginQueryService(
                companies,
                activations,
                registry(),
                new CompanyPluginResolver());
        CompanyContributions result = new CompanyContributionService(query).compose(companyId);
        return new ContributionProbeResult(
                result.plugins().stream()
                        .map(plugin -> plugin.pluginId().value())
                        .toList(),
                result.capabilities().stream().map(Object::toString).toList(),
                result.permissions().stream().map(Object::toString).toList(),
                result.menuContributions().stream()
                        .map(menu -> menu.id().value())
                        .toList());
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ScreenProbeResult screens(CompanyId companyId) {
        CompanyPluginQueryService query = new CompanyPluginQueryService(
                companies,
                activations,
                registry(),
                new CompanyPluginResolver());
        CompanyScreenComposition result = new CompanyScreenService(
                new CompanyContributionService(query),
                new CompanyScreenComposer())
                .compose(companyId);
        var screen = result.screens().getFirst();
        var summary = screen.elements().stream()
                .filter(element -> element.id().value().equals("summary"))
                .findFirst()
                .orElseThrow();
        var refresh = screen.elements().stream()
                .filter(element -> element.id().value().equals("refresh"))
                .findFirst()
                .orElseThrow();
        return new ScreenProbeResult(
                screen.id().toString(),
                summary.labelKey().value(),
                summary.visible(),
                summary.required(),
                refresh.enabled(),
                screen.slotContents().stream()
                        .map(content -> content.fragmentId().ownerPluginId().value())
                        .toList());
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProbeState state(CompanyId companyId) {
        return new ProbeState(
                companies.findById(companyId).isPresent(),
                activations.findByCompanyAndPlugin(companyId, FUNCTIONAL_PLUGIN_ID).isPresent());
    }

    @Transactional
    public SystemAuthorityProbeResult systemAuthorityCommit(UUID probeId) {
        return bootstrapSystemAuthority(probeId);
    }

    @Transactional
    public void systemAuthorityRollback(UUID probeId) {
        bootstrapSystemAuthority(probeId);
        throw new ExpectedRollbackException();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public SystemAuthorityProbeState systemAuthorityState(UUID probeId) {
        AppUserId userId = systemUserId(probeId);
        SystemRoleId roleId = systemRoleId(probeId);
        return new SystemAuthorityProbeState(
                count("SELECT COUNT(*) FROM core.app_user WHERE app_user_id = :id", userId.value()),
                count("SELECT COUNT(*) FROM core.system_role WHERE system_role_id = :id", roleId.value()),
                count("""
                        SELECT COUNT(*) FROM core.app_user_system_role
                        WHERE app_user_id = :userId AND system_role_id = :roleId
                        """, userId.value(), roleId.value()),
                count("""
                        SELECT COUNT(*) FROM core.system_role_permission
                        WHERE system_role_id = :id
                        """, roleId.value()),
                count("""
                        SELECT COUNT(*) FROM core.audit_event
                        WHERE category = 'SYSTEM_AUTHORITY_OPERATION'
                          AND operation = :operation
                          AND subject_user_id = :userId
                          AND system_role_id = :roleId
                        """, SystemAuthorityAuditOperation.BOOTSTRAP_SYSTEM_AUTHORITY.name(),
                        userId.value(), roleId.value()));
    }

    private void writeCompanyAndActivation(CompanyId companyId) {
        String customizationId = "custom_probe_" + companyId.value().toString().replace("-", "");
        companies.save(new Company(
                companyId,
                CompanyStatus.ACTIVE,
                new PluginId(customizationId),
                0));
        activations.save(new PluginActivationDecision(
                companyId,
                FUNCTIONAL_PLUGIN_ID,
                PluginActivationState.ENABLED,
                0));
    }

    private SystemAuthorityProbeResult bootstrapSystemAuthority(UUID probeId) {
        SystemAuthorityBootstrapService service = new SystemAuthorityBootstrapService(
                users,
                systemAuthority,
                () -> systemUserId(probeId),
                () -> systemRoleId(probeId),
                systemAuthorityAudit,
                Clock.systemUTC());
        SecurityOperationResult<SystemAuthorityBootstrapState> result = service.bootstrap(
                new BootstrapSystemAuthorityCommand(
                        new ExternalIdentity(
                                "https://jta-probe.invalid/realms/logixone",
                                probeId.toString()),
                        Optional.of("JTA system authority probe"),
                        new SystemRoleCode("jta.system_admin.p"
                                + probeId.toString().replace("-", "")),
                        "JTA system administrator probe",
                        Set.of(
                                SystemPermission.SYSTEM_ADMINISTRATION_MANAGE,
                                SystemPermission.AUDIT_VIEW)));
        SystemAuthorityBootstrapState state = result.value().orElseThrow();
        return new SystemAuthorityProbeResult(
                result.status().name(),
                state.user().id().value().toString(),
                state.role().id().value().toString());
    }

    private AppUserId systemUserId(UUID probeId) {
        return new AppUserId(stableUuid("jta-system-user:", probeId));
    }

    private SystemRoleId systemRoleId(UUID probeId) {
        return new SystemRoleId(stableUuid("jta-system-role:", probeId));
    }

    private UUID stableUuid(String namespace, UUID probeId) {
        return UUID.nameUUIDFromBytes(
                (namespace + probeId).getBytes(StandardCharsets.UTF_8));
    }

    private long count(String sql, Object id) {
        return ((Number) entityManager.createNativeQuery(sql)
                .setParameter("id", id)
                .getSingleResult()).longValue();
    }

    private long count(String sql, Object userId, Object roleId) {
        return ((Number) entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("roleId", roleId)
                .getSingleResult()).longValue();
    }

    private long count(String sql, Object operation, Object userId, Object roleId) {
        return ((Number) entityManager.createNativeQuery(sql)
                .setParameter("operation", operation)
                .setParameter("userId", userId)
                .setParameter("roleId", roleId)
                .getSingleResult()).longValue();
    }

    private ApplicationProbeResult applicationCommit(
            CompanyId companyId,
            PluginId customizationId,
            boolean enableFunctional) {
        removePreviousProbeCompany(customizationId);
        CompanyAdministrationService administration = administration(companyId, audit);
        CompanyOperationResult<Company> registration = administration.register(
                new RegisterCompanyCommand(customizationId));
        Company company = registration.value().orElseThrow();
        CompanyOperationStatus activationStatus = CompanyOperationStatus.UNCHANGED;
        if (enableFunctional) {
            CompanyOperationResult<PluginActivationDecision> activation = new PluginActivationService(
                    companies,
                    activations,
                    registry(),
                    new PluginActivationPolicy(),
                    audit,
                    Clock.systemUTC(),
                    CompanyAuditActor.TEST)
                    .change(new ChangePluginActivationCommand(
                            company.id(),
                            FUNCTIONAL_PLUGIN_ID,
                            PluginActivationState.ENABLED,
                            0));
            activationStatus = activation.status();
        }
        CompanyOperationResult<Company> status = administration.changeStatus(
                new ChangeCompanyStatusCommand(
                        company.id(), CompanyStatus.ACTIVE, company.version()));
        return new ApplicationProbeResult(
                registration.status(), activationStatus, status.status());
    }

    private void removePreviousProbeCompany(PluginId customizationId) {
        entityManager.createNativeQuery("""
                        DELETE FROM core.company_plugin_activation
                        WHERE company_id IN (
                            SELECT company_id
                            FROM core.company
                            WHERE customization_plugin_id = :pluginId
                        )
                        """)
                .setParameter("pluginId", customizationId.value())
                .executeUpdate();
        entityManager.createNativeQuery("""
                        DELETE FROM core.company
                        WHERE customization_plugin_id = :pluginId
                        """)
                .setParameter("pluginId", customizationId.value())
                .executeUpdate();
    }

    private CompanyAdministrationService administration(
            CompanyId companyId,
            CompanyAuditPort auditPort) {
        return new CompanyAdministrationService(
                companies,
                activations,
                () -> companyId,
                registry(),
                new CompanyPluginResolver(),
                auditPort,
                Clock.systemUTC(),
                CompanyAuditActor.TEST);
    }

    private PluginRegistry registry() {
        return catalog.registry();
    }

    record ProbeState(boolean company, boolean activation) {
    }

    record ApplicationProbeResult(
            CompanyOperationStatus registration,
            CompanyOperationStatus activation,
            CompanyOperationStatus companyStatus) {
    }

    record ContributionProbeResult(
            List<String> plugins,
            List<String> capabilities,
            List<String> permissions,
            List<String> menus) {
    }

    record ScreenProbeResult(
            String screen,
            String summaryLabel,
            boolean summaryVisible,
            boolean summaryRequired,
            boolean refreshEnabled,
            List<String> fragmentOwners) {
    }

    record SystemAuthorityProbeResult(String outcome, String userId, String roleId) {
    }

    record SystemAuthorityProbeState(
            long users,
            long roles,
            long assignments,
            long permissions,
            long auditEvents) {
    }
}
