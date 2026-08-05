package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.audit.admin.AuditEventCategory;
import py.com.logixone.kernel.application.audit.admin.AuditEventOutcome;
import py.com.logixone.kernel.domain.company.Company;
import py.com.logixone.kernel.domain.company.CompanyStatus;
import py.com.logixone.kernel.domain.company.PluginActivationDecision;
import py.com.logixone.kernel.domain.company.PluginActivationState;
import py.com.logixone.kernel.domain.security.system.AppUserSystemRoleAssignment;
import py.com.logixone.kernel.domain.security.system.SystemRole;
import py.com.logixone.kernel.domain.security.system.SystemRoleCode;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;
import py.com.logixone.kernel.domain.security.system.SystemRolePermissionGrant;
import py.com.logixone.kernel.domain.security.system.SystemRoleStatus;
import py.com.logixone.plugin.api.PluginId;

class JpaEntityMappingTest {

    private static final CompanyId COMPANY_ID = new CompanyId(
            UUID.fromString("00000000-0000-0000-0000-000000000001"));

    @Test
    void companyMappingStaysInsideCoreAndConvertsWithoutLeakingEntity() {
        Table table = CompanyEntity.class.getAnnotation(Table.class);
        Company company = new Company(
                COMPANY_ID,
                CompanyStatus.INACTIVE,
                new PluginId("custom_company_a"),
                0);

        CompanyEntity entity = CompanyEntity.newEntity(company);

        assertTrue(CompanyEntity.class.isAnnotationPresent(Entity.class));
        assertEquals("core", table.schema());
        assertEquals("company", table.name());
        assertEquals(company, entity.toDomain());
    }

    @Test
    void activationMappingUsesScalarCompositeIdentityAndConvertsToDomain() {
        Table table = PluginActivationEntity.class.getAnnotation(Table.class);
        PluginActivationDecision decision = new PluginActivationDecision(
                COMPANY_ID,
                new PluginId("sales"),
                PluginActivationState.ENABLED,
                0);

        PluginActivationEntity entity = PluginActivationEntity.newEntity(decision);

        assertEquals("core", table.schema());
        assertEquals("company_plugin_activation", table.name());
        assertEquals(decision, entity.toDomain());
        assertFalse(Arrays.stream(PluginActivationEntity.class.getDeclaredFields())
                .anyMatch(field -> field.getType().equals(CompanyEntity.class)));
    }

    @Test
    void productionPersistenceUnitUsesJtaAndCanOnlyValidateTheFlywaySchema() throws IOException {
        String persistenceXml = Files.readString(
                Path.of("src/main/resources/META-INF/persistence.xml"));

        assertTrue(persistenceXml.contains("transaction-type=\"JTA\""));
        assertTrue(persistenceXml.contains("<jta-data-source>java:/jdbc/LogixoneCoreDS</jta-data-source>"));
        assertTrue(persistenceXml.contains("name=\"hibernate.hbm2ddl.auto\" value=\"validate\""));
        assertTrue(persistenceXml.contains(
                "name=\"jakarta.persistence.schema-generation.database.action\" value=\"none\""));
        assertFalse(persistenceXml.contains("value=\"create\""));
        assertFalse(persistenceXml.contains("value=\"update\""));
        assertFalse(persistenceXml.contains("value=\"drop\""));
    }

    @Test
    void newEntitiesRejectNonZeroVersions() {
        Company company = new Company(
                COMPANY_ID,
                CompanyStatus.ACTIVE,
                new PluginId("custom_company_a"),
                1);
        PluginActivationDecision decision = new PluginActivationDecision(
                COMPANY_ID,
                new PluginId("sales"),
                PluginActivationState.DISABLED,
                1);

        assertThrows(IllegalArgumentException.class, () -> CompanyEntity.newEntity(company));
        assertThrows(IllegalArgumentException.class, () -> PluginActivationEntity.newEntity(decision));
    }

    @Test
    void globalAuthorityMappingsUseCoreScalarKeysAndRoundTrip() {
        SystemRoleId roleId = SystemRoleId.parse(
                "00000000-0000-0000-0000-000000000101");
        AppUserId userId = new AppUserId(UUID.fromString(
                "00000000-0000-0000-0000-000000000201"));
        SystemRole role = new SystemRole(
                roleId,
                new SystemRoleCode("system.administrator"),
                "Administrador global",
                SystemRoleStatus.ACTIVE,
                0);
        AppUserSystemRoleAssignment assignment =
                new AppUserSystemRoleAssignment(userId, roleId);
        SystemRolePermissionGrant grant = new SystemRolePermissionGrant(
                roleId, SystemPermission.SYSTEM_ADMINISTRATION_MANAGE);

        assertEquals("system_role", SystemRoleEntity.class.getAnnotation(Table.class).name());
        assertEquals("core", SystemRoleEntity.class.getAnnotation(Table.class).schema());
        assertEquals(role, SystemRoleEntity.newEntity(role).toDomain());
        assertEquals(
                assignment,
                AppUserSystemRoleEntity.newEntity(assignment).toDomain());
        assertEquals(grant, SystemRolePermissionEntity.newEntity(grant).toDomain());
        assertFalse(Arrays.stream(AppUserSystemRoleEntity.class.getDeclaredFields())
                .anyMatch(field -> field.getType().equals(AppUserEntity.class)
                        || field.getType().equals(SystemRoleEntity.class)));
    }

    @Test
    void auditMappingProducesOnlyTheTechnicalProjection() {
        UUID actorId = UUID.fromString("00000000-0000-0000-0000-000000000201");
        java.time.Instant occurredAt = java.time.Instant.parse("2026-07-28T20:00:00Z");
        AuditEventEntity entity = new AuditEventEntity(
                AuditEventCategory.SYSTEM_AUTHORITY_ACCESS,
                "AUTHORIZE_SYSTEM_OPERATION",
                AuditEventOutcome.DENIED,
                "AUTHENTICATED_USER",
                actorId,
                null,
                null,
                null,
                null,
                null,
                SystemPermission.AUDIT_VIEW.value(),
                null,
                null,
                null,
                "PERMISSION_DENIED",
                null,
                null,
                "request-001",
                occurredAt);

        var view = entity.toView();
        assertEquals("audit_event", AuditEventEntity.class.getAnnotation(Table.class).name());
        assertEquals(AuditEventCategory.SYSTEM_AUTHORITY_ACCESS, view.category());
        assertEquals(AuditEventOutcome.DENIED, view.outcome());
        assertEquals(Optional.of(new AppUserId(actorId)), view.actorUserId());
        assertEquals(Optional.of(SystemPermission.AUDIT_VIEW.value()), view.permissionId());
        assertEquals(Optional.of("request-001"), view.correlationId());
        assertEquals(occurredAt, view.occurredAt());
    }
}
