package py.com.logixone.kernel.application.security.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.SecurityOperationCode;
import py.com.logixone.kernel.application.security.SecurityOperationResult;
import py.com.logixone.kernel.application.security.SecurityOperationStatus;
import py.com.logixone.kernel.application.security.system.audit.SystemAuthorityAuditEvent;
import py.com.logixone.kernel.application.security.system.command.BootstrapSystemAuthorityCommand;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.kernel.domain.security.system.SystemRoleCode;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;

class SystemAuthorityBootstrapServiceTest {

    private static final AppUserId USER_ID = new AppUserId(UUID.fromString(
            "00000000-0000-0000-0000-000000000201"));
    private static final SystemRoleId ROLE_ID = SystemRoleId.parse(
            "00000000-0000-0000-0000-000000000101");
    private static final Instant NOW = Instant.parse("2026-07-28T20:00:00Z");

    private final SystemAuthorityTestFixture fixture = new SystemAuthorityTestFixture();
    private final List<SystemAuthorityAuditEvent> events = new ArrayList<>();
    private final BootstrapSystemAuthorityCommand command = command(
            Optional.of("Administrador inicial"),
            "Administración global",
            Set.of(
                    SystemPermission.SYSTEM_ADMINISTRATION_MANAGE,
                    SystemPermission.AUDIT_VIEW));
    private SystemAuthorityBootstrapService service;

    @BeforeEach
    void setUp() {
        service = new SystemAuthorityBootstrapService(
                fixture,
                fixture,
                () -> USER_ID,
                () -> ROLE_ID,
                events::add,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsTheFirstAdministratorAtomicallyAndAuditsOnlyTechnicalData() {
        SecurityOperationResult<SystemAuthorityBootstrapState> result = service.bootstrap(command);

        assertEquals(SecurityOperationStatus.CHANGED, result.status());
        SystemAuthorityBootstrapState state = result.value().orElseThrow();
        assertEquals(USER_ID, state.user().id());
        assertEquals(ROLE_ID, state.role().id());
        assertEquals(2, state.grants().size());
        assertEquals(1, fixture.users.size());
        assertEquals(1, fixture.roles.size());
        assertEquals(1, fixture.assignments.size());
        assertEquals(2, fixture.grants.size());
        assertEquals(1, fixture.lockCount);

        assertEquals(1, events.size());
        SystemAuthorityAuditEvent event = events.getFirst();
        assertEquals(NOW, event.occurredAt());
        assertEquals(Optional.of(USER_ID), event.subjectUserId());
        assertEquals(Optional.of(ROLE_ID), event.systemRoleId());
        assertTrue(event.code().isEmpty());
        assertFalse(event.toString().contains(command.externalIdentity().issuer()));
        assertFalse(event.toString().contains(command.externalIdentity().subject()));
        assertFalse(event.toString().contains(command.userDisplayName().orElseThrow()));
    }

    @Test
    void exactRepeatedDeclarationIsIdempotent() {
        assertEquals(SecurityOperationStatus.CHANGED, service.bootstrap(command).status());
        SecurityOperationResult<SystemAuthorityBootstrapState> repeated = service.bootstrap(command);

        assertEquals(SecurityOperationStatus.UNCHANGED, repeated.status());
        assertEquals(1, fixture.users.size());
        assertEquals(1, fixture.roles.size());
        assertEquals(1, fixture.assignments.size());
        assertEquals(2, fixture.grants.size());
        assertEquals(2, fixture.lockCount);
        assertEquals(2, events.size());
    }

    @Test
    void rejectsIncompatibleIdentityRoleAssignmentAndPermissions() {
        service.bootstrap(command);

        assertRejected(
                service.bootstrap(command(Optional.of("Otro nombre"),
                        "Administración global", command.permissions())),
                SecurityOperationCode.SYSTEM_BOOTSTRAP_IDENTITY_INCOMPATIBLE);
        assertRejected(
                service.bootstrap(command(Optional.of("Administrador inicial"),
                        "Otro rol", command.permissions())),
                SecurityOperationCode.SYSTEM_BOOTSTRAP_ROLE_INCOMPATIBLE);

        fixture.assignments.clear();
        assertRejected(
                service.bootstrap(command),
                SecurityOperationCode.SYSTEM_BOOTSTRAP_ASSIGNMENT_INCOMPATIBLE);
        fixture.saveAssignment(new py.com.logixone.kernel.domain.security.system
                .AppUserSystemRoleAssignment(USER_ID, ROLE_ID));
        fixture.grants.removeIf(grant -> grant.permission().equals(SystemPermission.AUDIT_VIEW));
        assertRejected(
                service.bootstrap(command),
                SecurityOperationCode.SYSTEM_BOOTSTRAP_PERMISSION_INCOMPATIBLE);
    }

    @Test
    void commandRejectsMissingAdministratorPermissionAndInvalidPresentationValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> command(Optional.of("Administrador inicial"),
                        "Administración global", Set.of(SystemPermission.AUDIT_VIEW)));
        assertThrows(
                IllegalArgumentException.class,
                () -> command(Optional.of(" nombre"),
                        "Administración global",
                        Set.of(SystemPermission.SYSTEM_ADMINISTRATION_MANAGE)));
        assertThrows(
                IllegalArgumentException.class,
                () -> command(Optional.empty(),
                        "rol\ninválido",
                        Set.of(SystemPermission.SYSTEM_ADMINISTRATION_MANAGE)));
    }

    private static BootstrapSystemAuthorityCommand command(
            Optional<String> userDisplayName,
            String roleDisplayName,
            Set<SystemPermission> permissions) {
        return new BootstrapSystemAuthorityCommand(
                new ExternalIdentity(
                        "https://identity.example.test/realms/logixone",
                        "bootstrap-administrator"),
                userDisplayName,
                new SystemRoleCode("system.administrator"),
                roleDisplayName,
                permissions);
    }

    private static void assertRejected(
            SecurityOperationResult<?> result,
            SecurityOperationCode expected) {
        assertEquals(SecurityOperationStatus.REJECTED, result.status());
        assertEquals(Optional.of(expected), result.failure());
        assertTrue(result.value().isEmpty());
    }
}
