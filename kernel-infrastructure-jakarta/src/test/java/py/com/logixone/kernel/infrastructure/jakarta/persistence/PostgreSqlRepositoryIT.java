package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.audit.TechnicalAuditEvent;
import py.com.logixone.kernel.api.audit.TechnicalAuditOutcome;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.audit.admin.AuditEventCategory;
import py.com.logixone.kernel.application.audit.admin.AuditEventOutcome;
import py.com.logixone.kernel.application.audit.admin.AuditQuery;
import py.com.logixone.kernel.application.audit.admin.AuditTimeWindow;
import py.com.logixone.kernel.application.company.CompanyAdministrationService;
import py.com.logixone.kernel.application.company.CompanyOperationStatus;
import py.com.logixone.kernel.application.company.CompanyPluginQueryService;
import py.com.logixone.kernel.application.company.PluginActivationService;
import py.com.logixone.kernel.application.company.audit.CompanyAuditActor;
import py.com.logixone.kernel.application.company.command.ChangeCompanyStatusCommand;
import py.com.logixone.kernel.application.company.command.ChangePluginActivationCommand;
import py.com.logixone.kernel.application.company.command.RegisterCompanyCommand;
import py.com.logixone.kernel.application.company.port.CompanyAuditPort;
import py.com.logixone.kernel.application.company.port.PersistenceConflictCode;
import py.com.logixone.kernel.application.company.port.PersistenceConflictException;
import py.com.logixone.kernel.application.plugin.PluginRegistry;
import py.com.logixone.kernel.application.security.port.SecurityPersistenceCode;
import py.com.logixone.kernel.application.security.port.SecurityPersistenceException;
import py.com.logixone.kernel.application.security.system.access.SystemAuthorityAccessCode;
import py.com.logixone.kernel.application.security.system.audit.SystemAuthorityAccessAuditEvent;
import py.com.logixone.kernel.application.security.system.audit.SystemAuthorityAccessAuditOutcome;
import py.com.logixone.kernel.domain.company.Company;
import py.com.logixone.kernel.domain.company.CompanyPluginResolver;
import py.com.logixone.kernel.domain.company.CompanyStatus;
import py.com.logixone.kernel.domain.company.PluginActivationDecision;
import py.com.logixone.kernel.domain.company.PluginActivationPolicy;
import py.com.logixone.kernel.domain.company.PluginActivationState;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.CompanyMembership;
import py.com.logixone.kernel.domain.security.CompanyRole;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.kernel.domain.security.MembershipRoleAssignment;
import py.com.logixone.kernel.domain.security.MembershipStatus;
import py.com.logixone.kernel.domain.security.RoleCode;
import py.com.logixone.kernel.domain.security.RoleId;
import py.com.logixone.kernel.domain.security.RolePermissionGrant;
import py.com.logixone.kernel.domain.security.RoleStatus;
import py.com.logixone.kernel.domain.security.UserStatus;
import py.com.logixone.kernel.domain.security.system.AppUserSystemRoleAssignment;
import py.com.logixone.kernel.domain.security.system.SystemRole;
import py.com.logixone.kernel.domain.security.system.SystemRoleCode;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;
import py.com.logixone.kernel.domain.security.system.SystemRolePermissionGrant;
import py.com.logixone.kernel.domain.security.system.SystemRoleStatus;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;

@Testcontainers
class PostgreSqlRepositoryIT {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse(
                    "postgres:18.4-bookworm@sha256:16fa100a3a6e92c0556632870455e7f8c6f3df5cefddd67d6b95292732bd7ff0")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("logixone_repository_test")
            .withUsername("logixone_test")
            .withPassword("test-only-password");

    private static EntityManagerFactory entityManagerFactory;

    @BeforeAll
    static void migrateAndValidateMappings() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas("core")
                .defaultSchema("core")
                .table("flyway_schema_history")
                .locations("classpath:db/migration/core")
                .createSchemas(true)
                .cleanDisabled(true)
                .validateMigrationNaming(true)
                .validateOnMigrate(true)
                .failOnMissingLocations(true)
                .outOfOrder(false)
                .load()
                .migrate();

        entityManagerFactory = Persistence.createEntityManagerFactory(
                "logixone-core-test-pu",
                Map.of(
                        "jakarta.persistence.jdbc.driver", "org.postgresql.Driver",
                        "jakarta.persistence.jdbc.url", POSTGRES.getJdbcUrl(),
                        "jakarta.persistence.jdbc.user", POSTGRES.getUsername(),
                        "jakarta.persistence.jdbc.password", POSTGRES.getPassword()));
    }

    @AfterAll
    static void closeEntityManagerFactory() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @Test
    void bootstrapsOnlyAfterFlywayAndValidatesTheRealSchema() throws Exception {
        assertTrue(entityManagerFactory.isOpen());

        try (Connection connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        SELECT count(*)
                        FROM core.flyway_schema_history
                        WHERE success AND version IS NOT NULL
                        """)) {
            assertTrue(result.next());
            assertEquals(6, result.getInt(1));
        }
    }

    @Test
    void globalAuthorityRepositoryIsIdempotentVersionedAndReferenceSafe() {
        AppUserId userId = new AppUserId(UUID.randomUUID());
        AppUser user = inTransaction(entityManager -> new JpaAppUserRepository(entityManager).save(
                new AppUser(
                        userId,
                        new ExternalIdentity(
                                "https://issuer.demo.invalid/realms/logixone",
                                UUID.randomUUID().toString()),
                        Optional.of("Administrador global"),
                        UserStatus.ACTIVE,
                        0)));
        SystemRoleId roleId = new SystemRoleId(UUID.randomUUID());
        SystemRoleCode roleCode = new SystemRoleCode(
                "system_admin_" + UUID.randomUUID().toString().replace("-", ""));
        SystemRole initialRole = new SystemRole(
                roleId, roleCode, "Administrador global", SystemRoleStatus.ACTIVE, 0);

        SystemRole storedRole = inTransaction(entityManager -> {
            JpaSystemAuthorityRepository repository = new JpaSystemAuthorityRepository(entityManager);
            repository.lockAuthorityState();
            SystemRole role = repository.saveRole(initialRole);
            assertEquals(role, repository.saveRole(role));
            assertEquals(
                    new AppUserSystemRoleAssignment(user.id(), role.id()),
                    repository.saveAssignment(new AppUserSystemRoleAssignment(user.id(), role.id())));
            assertEquals(
                    new SystemRolePermissionGrant(
                            role.id(), SystemPermission.SYSTEM_ADMINISTRATION_MANAGE),
                    repository.savePermissionGrant(new SystemRolePermissionGrant(
                            role.id(), SystemPermission.SYSTEM_ADMINISTRATION_MANAGE)));
            return role;
        });

        assertEquals(List.of(storedRole), inTransaction(entityManager ->
                new JpaSystemAuthorityRepository(entityManager).findAllRoles()).stream()
                .filter(role -> role.id().equals(roleId)).toList());
        assertEquals(1, inTransaction(entityManager ->
                new JpaSystemAuthorityRepository(entityManager).findAssignedUsers()).stream()
                .filter(found -> found.id().equals(userId)).count());
        boolean assignmentPresent = inTransaction(entityManager ->
                new JpaSystemAuthorityRepository(entityManager)
                        .findAssignment(userId, roleId).isPresent());
        boolean grantPresent = inTransaction(entityManager ->
                new JpaSystemAuthorityRepository(entityManager)
                        .findPermissionGrant(
                                roleId, SystemPermission.SYSTEM_ADMINISTRATION_MANAGE).isPresent());
        assertTrue(assignmentPresent);
        assertTrue(grantPresent);

        SystemRole updated = inTransaction(entityManager ->
                new JpaSystemAuthorityRepository(entityManager).saveRole(new SystemRole(
                        roleId,
                        roleCode,
                        "Administrador suspendido",
                        SystemRoleStatus.INACTIVE,
                        storedRole.version())));
        assertEquals(1, updated.version());

        SecurityPersistenceException stale = assertThrows(
                SecurityPersistenceException.class,
                () -> inTransaction(entityManager ->
                        new JpaSystemAuthorityRepository(entityManager).saveRole(storedRole)));
        assertEquals(SecurityPersistenceCode.SYSTEM_ROLE_VERSION_CONFLICT, stale.code());

        inTransaction(entityManager -> {
            JpaSystemAuthorityRepository repository = new JpaSystemAuthorityRepository(entityManager);
            repository.removeAssignment(new AppUserSystemRoleAssignment(userId, roleId));
            repository.removePermissionGrant(new SystemRolePermissionGrant(
                    roleId, SystemPermission.SYSTEM_ADMINISTRATION_MANAGE));
            return null;
        });
        assertTrue(inTransaction(entityManager ->
                new JpaSystemAuthorityRepository(entityManager).findAllAssignments()).stream()
                .noneMatch(assignment -> assignment.roleId().equals(roleId)));
        assertTrue(inTransaction(entityManager ->
                new JpaSystemAuthorityRepository(entityManager).findAllPermissionGrants()).stream()
                .noneMatch(grant -> grant.roleId().equals(roleId)));
    }

    @Test
    void technicalAuditStoreAndBoundedQueryPreserveOrderFiltersAndPagination() {
        AppUserId actorId = new AppUserId(UUID.randomUUID());
        Instant first = Instant.parse("2026-07-28T20:00:00Z");
        Instant second = first.plusSeconds(60);
        Instant third = second.plusSeconds(60);

        inTransaction(entityManager -> {
            JpaTechnicalAuditStore store = new JpaTechnicalAuditStore(entityManager);
            store.record(accessEvent(
                    SystemAuthorityAccessAuditOutcome.DENIED,
                    Optional.empty(),
                    Optional.of(SystemPermission.AUDIT_VIEW),
                    Optional.of(SystemAuthorityAccessCode.IDENTITY_NOT_FOUND),
                    "audit-it-1",
                    first));
            store.record(accessEvent(
                    SystemAuthorityAccessAuditOutcome.ALLOWED,
                    Optional.of(actorId),
                    Optional.of(SystemPermission.AUDIT_VIEW),
                    Optional.empty(),
                    "audit-it-2",
                    second));
            store.record(accessEvent(
                    SystemAuthorityAccessAuditOutcome.DENIED,
                    Optional.of(actorId),
                    Optional.of(SystemPermission.COMPANY_MANAGE),
                    Optional.of(SystemAuthorityAccessCode.PERMISSION_DENIED),
                    "audit-it-3",
                    third));
            return null;
        });

        var firstPage = inTransaction(entityManager -> new JpaAuditQueryAdapter(entityManager).query(
                new AuditQuery(
                        Optional.of(AuditEventCategory.SYSTEM_AUTHORITY_ACCESS),
                        Optional.empty(),
                        AuditTimeWindow.ALL,
                        Optional.empty(),
                        Optional.empty(),
                        0,
                        2)));
        assertEquals(2, firstPage.events().size());
        assertTrue(firstPage.hasNext());
        assertEquals(third, firstPage.events().get(0).occurredAt());
        assertEquals(second, firstPage.events().get(1).occurredAt());

        var exact = inTransaction(entityManager -> new JpaAuditQueryAdapter(entityManager).query(
                new AuditQuery(
                        Optional.of(AuditEventCategory.SYSTEM_AUTHORITY_ACCESS),
                        Optional.of(AuditEventOutcome.DENIED),
                        AuditTimeWindow.ALL,
                        Optional.empty(),
                        Optional.of("audit-it-1"),
                        0,
                        25)));
        assertEquals(1, exact.events().size());
        assertEquals("audit-it-1", exact.events().getFirst().correlationId().orElseThrow());
        assertTrue(exact.events().getFirst().actorUserId().isEmpty());
        assertFalse(exact.hasNext());
    }

    @Test
    void pluginTechnicalAuditRoundTripsOnlyItsTechnicalResourceEnvelope() {
        AppUserId actorId = new AppUserId(UUID.randomUUID());
        CompanyId companyId = companyId();
        String resourceId = UUID.randomUUID().toString();
        Instant occurredAt = Instant.parse("2026-07-29T12:00:00Z");

        inTransaction(entityManager -> {
            new JpaTechnicalAuditStore(entityManager).record(new TechnicalAuditEvent(
                    "ASSIGN_BUSINESS_PARTNER_ROLE",
                    TechnicalAuditOutcome.CHANGED,
                    actorId,
                    companyId,
                    "business_partners",
                    "business_partners.roles.manage",
                    "business_partner",
                    Optional.of(resourceId),
                    "SUCCESS",
                    Optional.of(2L),
                    Optional.of(3L),
                    "business-partner-audit-it",
                    occurredAt));
            return null;
        });

        var page = inTransaction(entityManager -> new JpaAuditQueryAdapter(entityManager).query(
                new AuditQuery(
                        Optional.of(AuditEventCategory.PLUGIN_OPERATION),
                        Optional.of(AuditEventOutcome.CHANGED),
                        AuditTimeWindow.ALL,
                        Optional.of(companyId),
                        Optional.of("business-partner-audit-it"),
                        0,
                        25)));

        assertEquals(1, page.events().size());
        var event = page.events().getFirst();
        assertEquals(Optional.of("business_partners"), event.pluginId());
        assertEquals(Optional.of("business_partners.roles.manage"), event.permissionId());
        assertEquals(Optional.of("business_partner"), event.resourceType());
        assertEquals(Optional.of(resourceId), event.resourceId());
        assertEquals(Optional.of(2L), event.previousVersion());
        assertEquals(Optional.of(3L), event.resultingVersion());
    }

    @Test
    void companyRoundTripIsIdempotentAndUsesOptimisticVersioning() {
        CompanyId companyId = companyId();
        PluginId customization = pluginId("custom_roundtrip");
        Company initial = new Company(companyId, CompanyStatus.ACTIVE, customization, 0);

        Company created = inTransaction(entityManager ->
                new JpaCompanyRepository(entityManager).save(initial));
        Company unchanged = inTransaction(entityManager ->
                new JpaCompanyRepository(entityManager).save(created));
        Company updated = inTransaction(entityManager ->
                new JpaCompanyRepository(entityManager).save(new Company(
                        companyId,
                        CompanyStatus.INACTIVE,
                        customization,
                        unchanged.version())));

        assertEquals(0, created.version());
        assertEquals(created, unchanged);
        assertEquals(1, updated.version());
        assertEquals(CompanyStatus.INACTIVE, updated.status());
        assertEquals(updated, inTransaction(entityManager ->
                new JpaCompanyRepository(entityManager).findById(companyId).orElseThrow()));
    }

    @Test
    void activationQueriesAreStrictlyScopedByCompany() {
        Company companyA = createCompany("custom_scope_a");
        Company companyB = createCompany("custom_scope_b");
        PluginId sharedPlugin = pluginId("sales_scope");

        PluginActivationDecision activationA = inTransaction(entityManager ->
                new JpaPluginActivationRepository(entityManager).save(new PluginActivationDecision(
                        companyA.id(), sharedPlugin, PluginActivationState.ENABLED, 0)));
        PluginActivationDecision activationB = inTransaction(entityManager ->
                new JpaPluginActivationRepository(entityManager).save(new PluginActivationDecision(
                        companyB.id(), sharedPlugin, PluginActivationState.DISABLED, 0)));

        assertEquals(
                java.util.List.of(activationA),
                inTransaction(entityManager ->
                        new JpaPluginActivationRepository(entityManager).findByCompanyId(companyA.id())));
        assertEquals(
                java.util.List.of(activationB),
                inTransaction(entityManager ->
                        new JpaPluginActivationRepository(entityManager).findByCompanyId(companyB.id())));
        boolean absentForCompanyA = inTransaction(entityManager -> new JpaPluginActivationRepository(entityManager)
                .findByCompanyAndPlugin(companyA.id(), pluginId("inventory_absent"))
                .isPresent());
        assertFalse(absentForCompanyA);
    }

    @Test
    void duplicateCustomizationProducesAStableConflictAndRollsBack() {
        Company owner = createCompany("custom_unique_owner");
        CompanyId rejectedCompanyId = companyId();

        PersistenceConflictException conflict = assertThrows(
                PersistenceConflictException.class,
                () -> inTransaction(entityManager -> new JpaCompanyRepository(entityManager).save(new Company(
                        rejectedCompanyId,
                        CompanyStatus.ACTIVE,
                        owner.customizationPluginId(),
                        0))));

        assertEquals(PersistenceConflictCode.CUSTOMIZATION_ALREADY_ASSIGNED, conflict.code());
        boolean assignedElsewhere = inTransaction(entityManager -> new JpaCompanyRepository(entityManager)
                .isCustomizationAssignedToAnotherCompany(owner.customizationPluginId(), rejectedCompanyId));
        boolean rejectedCompanyWasRolledBack = inTransaction(entityManager ->
                new JpaCompanyRepository(entityManager).findById(rejectedCompanyId).isEmpty());
        assertTrue(assignedElsewhere);
        assertTrue(rejectedCompanyWasRolledBack);
    }

    @Test
    void staleCompanyWriterGetsAStableOptimisticConflict() {
        Company initial = createCompany("custom_optimistic");
        EntityManager firstEntityManager = entityManagerFactory.createEntityManager();
        EntityManager staleEntityManager = entityManagerFactory.createEntityManager();
        firstEntityManager.getTransaction().begin();
        staleEntityManager.getTransaction().begin();

        try {
            Company firstView = new JpaCompanyRepository(firstEntityManager)
                    .findById(initial.id())
                    .orElseThrow();
            Company staleView = new JpaCompanyRepository(staleEntityManager)
                    .findById(initial.id())
                    .orElseThrow();

            Company winner = new JpaCompanyRepository(firstEntityManager).save(new Company(
                    firstView.id(), CompanyStatus.INACTIVE, firstView.customizationPluginId(), firstView.version()));
            firstEntityManager.getTransaction().commit();

            PersistenceConflictException conflict = assertThrows(
                    PersistenceConflictException.class,
                    () -> new JpaCompanyRepository(staleEntityManager).save(new Company(
                            staleView.id(),
                            staleView.status(),
                            pluginId("custom_optimistic_loser"),
                            staleView.version())));
            assertEquals(PersistenceConflictCode.COMPANY_VERSION_CONFLICT, conflict.code());
            staleEntityManager.getTransaction().rollback();

            assertEquals(winner, inTransaction(entityManager ->
                    new JpaCompanyRepository(entityManager).findById(initial.id()).orElseThrow()));
        } finally {
            rollbackIfActive(firstEntityManager);
            rollbackIfActive(staleEntityManager);
            firstEntityManager.close();
            staleEntityManager.close();
        }
    }

    @Test
    void activationRequiresACompanyAndPreservesIdempotenceAndVersionChecks() {
        Company company = createCompany("custom_activation_version");
        PluginId pluginId = pluginId("inventory_version");

        PersistenceConflictException missingCompany = assertThrows(
                PersistenceConflictException.class,
                () -> inTransaction(entityManager -> new JpaPluginActivationRepository(entityManager).save(
                        new PluginActivationDecision(
                                companyId(), pluginId, PluginActivationState.ENABLED, 0))));
        assertEquals(PersistenceConflictCode.COMPANY_NOT_FOUND, missingCompany.code());

        PluginActivationDecision created = inTransaction(entityManager ->
                new JpaPluginActivationRepository(entityManager).save(new PluginActivationDecision(
                        company.id(), pluginId, PluginActivationState.ENABLED, 0)));
        PluginActivationDecision unchanged = inTransaction(entityManager ->
                new JpaPluginActivationRepository(entityManager).save(created));
        PluginActivationDecision updated = inTransaction(entityManager ->
                new JpaPluginActivationRepository(entityManager).save(new PluginActivationDecision(
                        company.id(), pluginId, PluginActivationState.DISABLED, unchanged.version())));

        PersistenceConflictException stale = assertThrows(
                PersistenceConflictException.class,
                () -> inTransaction(entityManager -> new JpaPluginActivationRepository(entityManager).save(
                        created)));

        assertEquals(created, unchanged);
        assertEquals(1, updated.version());
        assertEquals(PersistenceConflictCode.ACTIVATION_VERSION_CONFLICT, stale.code());
    }

    @Test
    void applicationUseCasesUseRealRepositoriesAndAuditFailureRollsBack() {
        CompanyId firstId = companyId();
        CompanyId secondId = companyId();
        CompanyId rolledBackId = companyId();
        PluginId functional = pluginId("functional_use_case");
        PluginId firstCustomization = pluginId("custom_use_case_a");
        PluginId secondCustomization = pluginId("custom_use_case_b");
        PluginId rollbackCustomization = pluginId("custom_use_case_rollback");
        PluginRegistry registry = registry(
                descriptor(functional, PluginKind.FUNCTIONAL),
                descriptor(firstCustomization, PluginKind.CUSTOMIZATION),
                descriptor(secondCustomization, PluginKind.CUSTOMIZATION),
                descriptor(rollbackCustomization, PluginKind.CUSTOMIZATION));

        inTransaction(entityManager -> {
            CompanyAdministrationService firstAdministration = administration(
                    entityManager, firstId, registry, event -> { });
            Company first = firstAdministration.register(
                    new RegisterCompanyCommand(firstCustomization)).value().orElseThrow();
            PluginActivationService activations = activation(entityManager, registry, event -> { });
            assertEquals(
                    CompanyOperationStatus.CHANGED,
                    activations.change(new ChangePluginActivationCommand(
                            first.id(), functional, PluginActivationState.ENABLED, 0)).status());
            assertEquals(
                    CompanyOperationStatus.CHANGED,
                    firstAdministration.changeStatus(new ChangeCompanyStatusCommand(
                            first.id(), CompanyStatus.ACTIVE, first.version())).status());

            CompanyAdministrationService secondAdministration = administration(
                    entityManager, secondId, registry, event -> { });
            Company second = secondAdministration.register(
                    new RegisterCompanyCommand(secondCustomization)).value().orElseThrow();
            assertEquals(
                    CompanyOperationStatus.CHANGED,
                    secondAdministration.changeStatus(new ChangeCompanyStatusCommand(
                            second.id(), CompanyStatus.ACTIVE, second.version())).status());
            return null;
        });

        List<PluginId> firstEffective = inTransaction(entityManager -> query(entityManager, registry)
                .resolve(firstId)
                .resolution().orElseThrow()
                .orderedPlugins().stream()
                .map(PluginDescriptor::id)
                .toList());
        List<PluginId> secondEffective = inTransaction(entityManager -> query(entityManager, registry)
                .resolve(secondId)
                .resolution().orElseThrow()
                .orderedPlugins().stream()
                .map(PluginDescriptor::id)
                .toList());

        assertEquals(List.of(functional, firstCustomization), firstEffective);
        assertEquals(List.of(secondCustomization), secondEffective);

        assertThrows(TestAuditFailure.class, () -> inTransaction(entityManager -> {
            administration(
                    entityManager,
                    rolledBackId,
                    registry,
                    event -> { throw new TestAuditFailure(); })
                    .register(new RegisterCompanyCommand(rollbackCustomization));
            return null;
        }));
        boolean registrationRolledBack = inTransaction(entityManager ->
                new JpaCompanyRepository(entityManager).findById(rolledBackId).isEmpty());
        assertTrue(registrationRolledBack);
    }

    @Test
    void securityRepositoriesAreIdempotentVersionedAndStrictlyCompanyScoped() {
        Company companyA = createCompany(uniquePluginId("custom_security_a"));
        Company companyB = createCompany(uniquePluginId("custom_security_b"));
        AppUserId userId = new AppUserId(UUID.randomUUID());
        ExternalIdentity externalIdentity = new ExternalIdentity(
                "https://issuer.demo.invalid/realms/logixone", UUID.randomUUID().toString());
        AppUser initialUser = new AppUser(
                userId, externalIdentity, Optional.of("Demo User"), UserStatus.ACTIVE, 0);

        AppUser createdUser = inTransaction(entityManager ->
                new JpaAppUserRepository(entityManager).save(initialUser));
        AppUser unchangedUser = inTransaction(entityManager ->
                new JpaAppUserRepository(entityManager).save(createdUser));
        AppUser updatedUser = inTransaction(entityManager ->
                new JpaAppUserRepository(entityManager).save(new AppUser(
                        userId,
                        externalIdentity,
                        Optional.of("Updated Demo User"),
                        UserStatus.ACTIVE,
                        unchangedUser.version())));

        CompanyMembership membershipA = inTransaction(entityManager ->
                new JpaCompanyMembershipRepository(entityManager).save(new CompanyMembership(
                        userId, companyA.id(), MembershipStatus.ACTIVE, 0)));
        CompanyMembership unchangedMembershipA = inTransaction(entityManager ->
                new JpaCompanyMembershipRepository(entityManager).save(membershipA));
        CompanyMembership membershipB = inTransaction(entityManager ->
                new JpaCompanyMembershipRepository(entityManager).save(new CompanyMembership(
                        userId, companyB.id(), MembershipStatus.INACTIVE, 0)));

        CompanyRole roleA = new CompanyRole(
                new RoleId(UUID.randomUUID()),
                companyA.id(),
                uniqueRoleCode("operator_a"),
                "Operator A",
                RoleStatus.ACTIVE,
                0);
        CompanyRole roleB = new CompanyRole(
                new RoleId(UUID.randomUUID()),
                companyB.id(),
                uniqueRoleCode("operator_b"),
                "Operator B",
                RoleStatus.ACTIVE,
                0);
        CompanyRole storedRoleA = inTransaction(entityManager ->
                new JpaCompanyAuthorizationRepository(entityManager).saveRole(roleA));
        CompanyRole storedRoleB = inTransaction(entityManager ->
                new JpaCompanyAuthorizationRepository(entityManager).saveRole(roleB));

        MembershipRoleAssignment assignmentA = new MembershipRoleAssignment(
                userId, companyA.id(), storedRoleA.id());
        MembershipRoleAssignment assignmentB = new MembershipRoleAssignment(
                userId, companyB.id(), storedRoleB.id());
        ContributionId permissionA = new ContributionId("reference.screen.view");
        ContributionId permissionB = new ContributionId("reference.screen.edit");
        RolePermissionGrant grantA = new RolePermissionGrant(companyA.id(), storedRoleA.id(), permissionA);
        RolePermissionGrant grantB = new RolePermissionGrant(companyB.id(), storedRoleB.id(), permissionB);

        inTransaction(entityManager -> {
            JpaCompanyAuthorizationRepository repository =
                    new JpaCompanyAuthorizationRepository(entityManager);
            assertEquals(assignmentA, repository.saveAssignment(assignmentA));
            assertEquals(assignmentA, repository.saveAssignment(assignmentA));
            assertEquals(assignmentB, repository.saveAssignment(assignmentB));
            assertEquals(grantA, repository.savePermissionGrant(grantA));
            assertEquals(grantA, repository.savePermissionGrant(grantA));
            assertEquals(grantB, repository.savePermissionGrant(grantB));
            return null;
        });

        assertEquals(createdUser, unchangedUser);
        assertEquals(1, updatedUser.version());
        assertEquals(unchangedMembershipA, membershipA);
        int membershipCount = inTransaction(entityManager ->
                new JpaCompanyMembershipRepository(entityManager).findByUserId(userId).size());
        assertEquals(2, membershipCount);
        assertEquals(membershipB, inTransaction(entityManager -> new JpaCompanyMembershipRepository(entityManager)
                .findByUserAndCompany(userId, companyB.id()).orElseThrow()));
        assertEquals(updatedUser, inTransaction(entityManager -> new JpaAppUserRepository(entityManager)
                .findByExternalIdentity(externalIdentity).orElseThrow()));
        assertEquals(List.of(storedRoleA), inTransaction(entityManager ->
                new JpaCompanyAuthorizationRepository(entityManager).findRolesByCompanyId(companyA.id())));
        assertEquals(List.of(assignmentA), inTransaction(entityManager ->
                new JpaCompanyAuthorizationRepository(entityManager).findAssignments(userId, companyA.id())));
        assertEquals(List.of(grantA), inTransaction(entityManager ->
                new JpaCompanyAuthorizationRepository(entityManager).findPermissionGrants(companyA.id())));
        assertEquals(List.of(storedRoleB), inTransaction(entityManager ->
                new JpaCompanyAuthorizationRepository(entityManager).findRolesByCompanyId(companyB.id())));
        assertEquals(List.of(assignmentB), inTransaction(entityManager ->
                new JpaCompanyAuthorizationRepository(entityManager).findAssignments(userId, companyB.id())));
        assertEquals(List.of(grantB), inTransaction(entityManager ->
                new JpaCompanyAuthorizationRepository(entityManager).findPermissionGrants(companyB.id())));
    }

    @Test
    void securityRepositoriesRejectDuplicatesAndCrossCompanyRelationshipsWithStableCodes() {
        Company companyA = createCompany(uniquePluginId("custom_security_guard_a"));
        Company companyB = createCompany(uniquePluginId("custom_security_guard_b"));
        AppUserId userId = new AppUserId(UUID.randomUUID());
        ExternalIdentity identity = new ExternalIdentity(
                "https://issuer.demo.invalid/realms/logixone", UUID.randomUUID().toString());
        AppUser user = inTransaction(entityManager -> new JpaAppUserRepository(entityManager).save(new AppUser(
                userId, identity, Optional.empty(), UserStatus.ACTIVE, 0)));
        inTransaction(entityManager -> new JpaCompanyMembershipRepository(entityManager).save(
                new CompanyMembership(user.id(), companyA.id(), MembershipStatus.ACTIVE, 0)));

        RoleCode sharedCode = uniqueRoleCode("operator_guard");
        CompanyRole roleA = inTransaction(entityManager ->
                new JpaCompanyAuthorizationRepository(entityManager).saveRole(new CompanyRole(
                        new RoleId(UUID.randomUUID()), companyA.id(), sharedCode,
                        "Operator Guard", RoleStatus.ACTIVE, 0)));
        CompanyRole roleB = inTransaction(entityManager ->
                new JpaCompanyAuthorizationRepository(entityManager).saveRole(new CompanyRole(
                        new RoleId(UUID.randomUUID()), companyB.id(), uniqueRoleCode("operator_other"),
                        "Operator Other", RoleStatus.ACTIVE, 0)));

        SecurityPersistenceException duplicateIdentity = assertThrows(
                SecurityPersistenceException.class,
                () -> inTransaction(entityManager -> new JpaAppUserRepository(entityManager).save(new AppUser(
                        new AppUserId(UUID.randomUUID()), identity, Optional.empty(), UserStatus.ACTIVE, 0))));
        SecurityPersistenceException duplicateRoleCode = assertThrows(
                SecurityPersistenceException.class,
                () -> inTransaction(entityManager ->
                        new JpaCompanyAuthorizationRepository(entityManager).saveRole(new CompanyRole(
                                new RoleId(UUID.randomUUID()), companyA.id(), sharedCode,
                                "Duplicate Operator", RoleStatus.ACTIVE, 0))));
        SecurityPersistenceException crossCompanyAssignment = assertThrows(
                SecurityPersistenceException.class,
                () -> inTransaction(entityManager ->
                        new JpaCompanyAuthorizationRepository(entityManager).saveAssignment(
                                new MembershipRoleAssignment(user.id(), companyA.id(), roleB.id()))));
        SecurityPersistenceException crossCompanyGrant = assertThrows(
                SecurityPersistenceException.class,
                () -> inTransaction(entityManager ->
                        new JpaCompanyAuthorizationRepository(entityManager).savePermissionGrant(
                                new RolePermissionGrant(
                                        companyA.id(), roleB.id(), new ContributionId("reference.screen.view")))));

        assertEquals(SecurityPersistenceCode.EXTERNAL_IDENTITY_ALREADY_EXISTS, duplicateIdentity.code());
        assertEquals(SecurityPersistenceCode.ROLE_CODE_ALREADY_EXISTS, duplicateRoleCode.code());
        assertEquals(SecurityPersistenceCode.ROLE_COMPANY_MISMATCH, crossCompanyAssignment.code());
        assertEquals(SecurityPersistenceCode.ROLE_COMPANY_MISMATCH, crossCompanyGrant.code());
        boolean assignmentsRolledBack = inTransaction(entityManager ->
                new JpaCompanyAuthorizationRepository(entityManager)
                        .findAssignments(user.id(), companyA.id()).isEmpty());
        boolean grantsRolledBack = inTransaction(entityManager ->
                new JpaCompanyAuthorizationRepository(entityManager)
                        .findPermissionGrants(companyA.id()).isEmpty());
        assertTrue(assignmentsRolledBack);
        assertTrue(grantsRolledBack);
        assertEquals(roleA, inTransaction(entityManager -> new JpaCompanyAuthorizationRepository(entityManager)
                .findRoleByCompanyAndCode(companyA.id(), sharedCode).orElseThrow()));
    }

    @Test
    void staleSecurityStateIsRejectedWithoutOverwritingTheWinner() {
        Company company = createCompany(uniquePluginId("custom_security_versions"));
        AppUserId userId = new AppUserId(UUID.randomUUID());
        ExternalIdentity identity = new ExternalIdentity(
                "https://issuer.demo.invalid/realms/logixone", UUID.randomUUID().toString());
        AppUser originalUser = inTransaction(entityManager -> new JpaAppUserRepository(entityManager).save(
                new AppUser(userId, identity, Optional.empty(), UserStatus.ACTIVE, 0)));
        AppUser currentUser = inTransaction(entityManager -> new JpaAppUserRepository(entityManager).save(
                new AppUser(userId, identity, Optional.of("Winner"), UserStatus.ACTIVE, originalUser.version())));

        CompanyMembership originalMembership = inTransaction(entityManager ->
                new JpaCompanyMembershipRepository(entityManager).save(
                        new CompanyMembership(userId, company.id(), MembershipStatus.ACTIVE, 0)));
        CompanyMembership currentMembership = inTransaction(entityManager ->
                new JpaCompanyMembershipRepository(entityManager).save(new CompanyMembership(
                        userId, company.id(), MembershipStatus.INACTIVE, originalMembership.version())));

        CompanyRole originalRole = inTransaction(entityManager ->
                new JpaCompanyAuthorizationRepository(entityManager).saveRole(new CompanyRole(
                        new RoleId(UUID.randomUUID()), company.id(), uniqueRoleCode("versioned_role"),
                        "Original Role", RoleStatus.ACTIVE, 0)));
        CompanyRole currentRole = inTransaction(entityManager ->
                new JpaCompanyAuthorizationRepository(entityManager).saveRole(new CompanyRole(
                        originalRole.id(), company.id(), originalRole.code(),
                        "Winner Role", RoleStatus.ACTIVE, originalRole.version())));

        SecurityPersistenceException staleUser = assertThrows(
                SecurityPersistenceException.class,
                () -> inTransaction(entityManager -> new JpaAppUserRepository(entityManager).save(originalUser)));
        SecurityPersistenceException staleMembership = assertThrows(
                SecurityPersistenceException.class,
                () -> inTransaction(entityManager ->
                        new JpaCompanyMembershipRepository(entityManager).save(originalMembership)));
        SecurityPersistenceException staleRole = assertThrows(
                SecurityPersistenceException.class,
                () -> inTransaction(entityManager ->
                        new JpaCompanyAuthorizationRepository(entityManager).saveRole(originalRole)));

        assertEquals(SecurityPersistenceCode.USER_VERSION_CONFLICT, staleUser.code());
        assertEquals(SecurityPersistenceCode.MEMBERSHIP_VERSION_CONFLICT, staleMembership.code());
        assertEquals(SecurityPersistenceCode.ROLE_VERSION_CONFLICT, staleRole.code());
        assertEquals(currentUser, inTransaction(entityManager ->
                new JpaAppUserRepository(entityManager).findById(userId).orElseThrow()));
        assertEquals(currentMembership, inTransaction(entityManager -> new JpaCompanyMembershipRepository(entityManager)
                .findByUserAndCompany(userId, company.id()).orElseThrow()));
        assertEquals(currentRole, inTransaction(entityManager ->
                new JpaCompanyAuthorizationRepository(entityManager).findRoleById(originalRole.id()).orElseThrow()));
    }

    private static Company createCompany(String customizationId) {
        Company company = new Company(
                companyId(), CompanyStatus.ACTIVE, pluginId(customizationId), 0);
        return inTransaction(entityManager -> new JpaCompanyRepository(entityManager).save(company));
    }

    private static CompanyId companyId() {
        return new CompanyId(UUID.randomUUID());
    }

    private static PluginId pluginId(String value) {
        return new PluginId(value);
    }

    private static String uniquePluginId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private static RoleCode uniqueRoleCode(String prefix) {
        return new RoleCode(prefix + "_" + UUID.randomUUID().toString().replace("-", ""));
    }

    private static SystemAuthorityAccessAuditEvent accessEvent(
            SystemAuthorityAccessAuditOutcome outcome,
            Optional<AppUserId> actorUserId,
            Optional<SystemPermission> requiredPermission,
            Optional<SystemAuthorityAccessCode> code,
            String correlationId,
            Instant occurredAt) {
        return new SystemAuthorityAccessAuditEvent(
                outcome,
                actorUserId,
                requiredPermission,
                code,
                correlationId,
                occurredAt);
    }

    private static CompanyAdministrationService administration(
            EntityManager entityManager,
            CompanyId companyId,
            PluginRegistry registry,
            CompanyAuditPort audit) {
        return new CompanyAdministrationService(
                new JpaCompanyRepository(entityManager),
                new JpaPluginActivationRepository(entityManager),
                () -> companyId,
                registry,
                new CompanyPluginResolver(),
                audit,
                Clock.systemUTC(),
                CompanyAuditActor.TEST);
    }

    private static PluginActivationService activation(
            EntityManager entityManager,
            PluginRegistry registry,
            CompanyAuditPort audit) {
        return new PluginActivationService(
                new JpaCompanyRepository(entityManager),
                new JpaPluginActivationRepository(entityManager),
                registry,
                new PluginActivationPolicy(),
                audit,
                Clock.systemUTC(),
                CompanyAuditActor.TEST);
    }

    private static CompanyPluginQueryService query(
            EntityManager entityManager,
            PluginRegistry registry) {
        return new CompanyPluginQueryService(
                new JpaCompanyRepository(entityManager),
                new JpaPluginActivationRepository(entityManager),
                registry,
                new CompanyPluginResolver());
    }

    private static PluginRegistry registry(PluginDescriptor... descriptors) {
        return PluginRegistry.create(List.of(descriptors).stream()
                .<PluginDefinition>map(descriptor -> () -> descriptor)
                .toList());
    }

    private static PluginDescriptor descriptor(PluginId id, PluginKind kind) {
        return new PluginDescriptor(
                id,
                kind,
                SemanticVersion.parse("1.0.0"),
                new VersionRange(
                        SemanticVersion.parse("0.4.0"),
                        SemanticVersion.parse("0.5.0")),
                id.value(),
                List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private static <T> T inTransaction(Function<EntityManager, T> work) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        try {
            T result = work.apply(entityManager);
            entityManager.getTransaction().commit();
            return result;
        } catch (RuntimeException failure) {
            rollbackIfActive(entityManager);
            throw failure;
        } finally {
            entityManager.close();
        }
    }

    private static void rollbackIfActive(EntityManager entityManager) {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
    }

    private static final class TestAuditFailure extends RuntimeException {
    }
}
