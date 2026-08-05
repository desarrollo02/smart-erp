package py.com.logixone.kernel.application.security.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.SecurityOperationCode;
import py.com.logixone.kernel.application.security.SecurityOperationResult;
import py.com.logixone.kernel.application.security.SecurityOperationStatus;
import py.com.logixone.kernel.application.security.audit.SecurityAuditActor;
import py.com.logixone.kernel.application.security.command.ChangeAppUserStatusCommand;
import py.com.logixone.kernel.application.security.system.audit.SystemAuthorityAuditEvent;
import py.com.logixone.kernel.application.security.system.command.ChangeSystemRoleStatusCommand;
import py.com.logixone.kernel.application.security.system.command.GrantSystemPermissionCommand;
import py.com.logixone.kernel.application.security.system.command.RegisterSystemRoleCommand;
import py.com.logixone.kernel.application.security.system.command.RevokeSystemPermissionCommand;
import py.com.logixone.kernel.application.security.system.command.UnassignSystemRoleCommand;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.kernel.domain.security.UserStatus;
import py.com.logixone.kernel.domain.security.system.AppUserSystemRoleAssignment;
import py.com.logixone.kernel.domain.security.system.SystemRole;
import py.com.logixone.kernel.domain.security.system.SystemRoleCode;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;
import py.com.logixone.kernel.domain.security.system.SystemRolePermissionGrant;
import py.com.logixone.kernel.domain.security.system.SystemRoleStatus;

class SystemAuthorityAdministrationServiceTest {

    private static final AppUserId USER_ID = new AppUserId(UUID.fromString(
            "00000000-0000-0000-0000-000000000201"));
    private static final SystemRoleId ROLE_ID = SystemRoleId.parse(
            "00000000-0000-0000-0000-000000000101");
    private static final SystemRoleId GENERATED_ROLE_ID = SystemRoleId.parse(
            "00000000-0000-0000-0000-000000000109");

    private final SystemAuthorityTestFixture fixture = new SystemAuthorityTestFixture();
    private final List<SystemAuthorityAuditEvent> events = new ArrayList<>();
    private SystemAuthorityAdministrationService service;

    @BeforeEach
    void setUp() {
        service = new SystemAuthorityAdministrationService(
                fixture,
                fixture,
                () -> GENERATED_ROLE_ID,
                events::add,
                Clock.fixed(Instant.parse("2026-07-28T22:00:00Z"), ZoneOffset.UTC),
                SecurityAuditActor.authenticated(USER_ID, "admin-request-1"));
    }

    @Test
    void registersInactiveRoleAndRejectsDuplicateCode() {
        SecurityOperationResult<SystemRole> created = service.registerRole(
                new RegisterSystemRoleCommand(
                        new SystemRoleCode("system.viewer"), "Consulta global"));

        assertEquals(SecurityOperationStatus.CHANGED, created.status());
        assertEquals(SystemRoleStatus.INACTIVE, created.value().orElseThrow().status());
        assertEquals(GENERATED_ROLE_ID, created.value().orElseThrow().id());
        assertEquals(1, fixture.lockCount);

        SecurityOperationResult<SystemRole> duplicate = service.registerRole(
                new RegisterSystemRoleCommand(
                        new SystemRoleCode("system.viewer"), "Otro nombre"));
        assertRejected(duplicate, SecurityOperationCode.SYSTEM_ROLE_CODE_ALREADY_EXISTS);
        assertEquals(2, events.size());
    }

    @Test
    void rejectsEveryMutationThatWouldRemoveTheLastAdministrator() {
        installLastAdministrator();

        assertRejected(
                service.changeUserStatus(new ChangeAppUserStatusCommand(
                        USER_ID, UserStatus.INACTIVE, 0)),
                SecurityOperationCode.SYSTEM_LAST_ADMINISTRATOR_REQUIRED);
        assertRejected(
                service.changeRoleStatus(new ChangeSystemRoleStatusCommand(
                        ROLE_ID, SystemRoleStatus.INACTIVE, 0)),
                SecurityOperationCode.SYSTEM_LAST_ADMINISTRATOR_REQUIRED);
        assertRejected(
                service.unassignRole(new UnassignSystemRoleCommand(USER_ID, ROLE_ID)),
                SecurityOperationCode.SYSTEM_LAST_ADMINISTRATOR_REQUIRED);
        assertRejected(
                service.revokePermission(new RevokeSystemPermissionCommand(
                        ROLE_ID, SystemPermission.SYSTEM_ADMINISTRATION_MANAGE)),
                SecurityOperationCode.SYSTEM_LAST_ADMINISTRATOR_REQUIRED);

        assertEquals(UserStatus.ACTIVE, fixture.users.get(USER_ID).status());
        assertEquals(SystemRoleStatus.ACTIVE, fixture.roles.get(ROLE_ID).status());
        assertEquals(1, fixture.assignments.size());
        assertEquals(1, fixture.grants.size());
        assertEquals(4, events.size());
        assertTrue(events.stream().allMatch(event -> event.code().equals(
                Optional.of(SecurityOperationCode.SYSTEM_LAST_ADMINISTRATOR_REQUIRED))));
    }

    @Test
    void permitsRemovingOneAdministratorWhenAnotherRemainsEffective() {
        installLastAdministrator();
        AppUserId secondUserId = new AppUserId(UUID.fromString(
                "00000000-0000-0000-0000-000000000202"));
        fixture.users.put(secondUserId, user(secondUserId, "second", UserStatus.ACTIVE));
        fixture.assignments.add(new AppUserSystemRoleAssignment(secondUserId, ROLE_ID));

        SecurityOperationResult<?> result = service.unassignRole(
                new UnassignSystemRoleCommand(USER_ID, ROLE_ID));

        assertEquals(SecurityOperationStatus.CHANGED, result.status());
        assertFalse(fixture.assignments.contains(new AppUserSystemRoleAssignment(USER_ID, ROLE_ID)));
        assertTrue(fixture.assignments.contains(new AppUserSystemRoleAssignment(secondUserId, ROLE_ID)));
    }

    @Test
    void rejectsVersionConflictAndUnknownPermission() {
        installLastAdministrator();

        assertRejected(
                service.changeUserStatus(new ChangeAppUserStatusCommand(
                        USER_ID, UserStatus.INACTIVE, 8)),
                SecurityOperationCode.USER_VERSION_CONFLICT);
        assertRejected(
                service.changeRoleStatus(new ChangeSystemRoleStatusCommand(
                        ROLE_ID, SystemRoleStatus.INACTIVE, 8)),
                SecurityOperationCode.SYSTEM_ROLE_VERSION_CONFLICT);
        assertRejected(
                service.grantPermission(new GrantSystemPermissionCommand(
                        ROLE_ID, new SystemPermission("kernel.future.manage"))),
                SecurityOperationCode.SYSTEM_PERMISSION_UNKNOWN);
    }

    private void installLastAdministrator() {
        fixture.users.put(USER_ID, user(USER_ID, "administrator", UserStatus.ACTIVE));
        fixture.roles.put(ROLE_ID, new SystemRole(
                ROLE_ID,
                new SystemRoleCode("system.administrator"),
                "Administrador global",
                SystemRoleStatus.ACTIVE,
                0));
        fixture.assignments.add(new AppUserSystemRoleAssignment(USER_ID, ROLE_ID));
        fixture.grants.add(new SystemRolePermissionGrant(
                ROLE_ID, SystemPermission.SYSTEM_ADMINISTRATION_MANAGE));
    }

    private static AppUser user(AppUserId id, String subject, UserStatus status) {
        return new AppUser(
                id,
                new ExternalIdentity(
                        "https://identity.example.test/realms/logixone", subject),
                Optional.of("Usuario " + subject),
                status,
                0);
    }

    private static void assertRejected(
            SecurityOperationResult<?> result,
            SecurityOperationCode expectedCode) {
        assertEquals(SecurityOperationStatus.REJECTED, result.status());
        assertEquals(Optional.of(expectedCode), result.failure());
        assertTrue(result.value().isEmpty());
    }
}
