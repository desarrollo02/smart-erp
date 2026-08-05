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
import py.com.logixone.kernel.application.security.system.access.SystemAuthorityAccess;
import py.com.logixone.kernel.application.security.system.access.SystemAuthorityAccessCode;
import py.com.logixone.kernel.application.security.system.access.SystemAuthorityAccessService;
import py.com.logixone.kernel.application.security.system.audit.SystemAuthorityAccessAuditEvent;
import py.com.logixone.kernel.application.security.system.audit.SystemAuthorityAccessAuditOutcome;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.kernel.domain.security.UserStatus;
import py.com.logixone.kernel.domain.security.system.AppUserSystemRoleAssignment;
import py.com.logixone.kernel.domain.security.system.SystemRole;
import py.com.logixone.kernel.domain.security.system.SystemRoleCode;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;
import py.com.logixone.kernel.domain.security.system.SystemRolePermissionGrant;
import py.com.logixone.kernel.domain.security.system.SystemRoleStatus;

class SystemAuthorityAccessServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-28T21:00:00Z");
    private static final ExternalIdentity IDENTITY = new ExternalIdentity(
            "https://identity.example.test/realms/logixone", "administrator");
    private static final AppUserId USER_ID = new AppUserId(UUID.fromString(
            "00000000-0000-0000-0000-000000000201"));
    private static final SystemRoleId ROLE_ID = SystemRoleId.parse(
            "00000000-0000-0000-0000-000000000101");

    private final SystemAuthorityTestFixture fixture = new SystemAuthorityTestFixture();
    private final List<SystemAuthorityAccessAuditEvent> events = new ArrayList<>();
    private SystemAuthorityAccessService service;

    @BeforeEach
    void setUp() {
        service = new SystemAuthorityAccessService(
                fixture,
                fixture,
                events::add,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void allowsCurrentKnownPermissionAndBuildsContextFromLocalState() {
        installAuthority(UserStatus.ACTIVE, SystemRoleStatus.ACTIVE);

        SystemAuthorityAccess access = service.authorize(
                IDENTITY, SystemPermission.AUDIT_VIEW, "request-001");

        assertTrue(access.authorized());
        assertEquals(USER_ID, access.context().orElseThrow().actorUserId());
        assertEquals(
                java.util.Set.of(SystemPermission.AUDIT_VIEW),
                access.context().orElseThrow().permissions());
        assertEquals(1, events.size());
        assertEquals(SystemAuthorityAccessAuditOutcome.ALLOWED, events.getFirst().outcome());
        assertEquals(Optional.empty(), events.getFirst().code());
        assertEquals("request-001", events.getFirst().correlationId());
    }

    @Test
    void authorizeAnyRequiresAtLeastOneEffectivePermission() {
        installAuthority(UserStatus.ACTIVE, SystemRoleStatus.ACTIVE);
        fixture.grants.clear();

        SystemAuthorityAccess denied = service.authorizeAny(IDENTITY, "request-002");

        assertFalse(denied.authorized());
        assertEquals(
                Optional.of(SystemAuthorityAccessCode.PERMISSION_DENIED),
                events.getFirst().code());
    }

    @Test
    void deniesUnknownIdentityInactiveUserAndMissingPermissionWithoutEnumeration() {
        assertFalse(service.authorize(
                new ExternalIdentity(IDENTITY.issuer(), "missing"),
                SystemPermission.AUDIT_VIEW,
                "request-003").authorized());
        assertEquals(
                Optional.of(SystemAuthorityAccessCode.IDENTITY_NOT_FOUND),
                events.getLast().code());

        installAuthority(UserStatus.INACTIVE, SystemRoleStatus.ACTIVE);
        assertFalse(service.authorize(
                IDENTITY, SystemPermission.AUDIT_VIEW, "request-004").authorized());
        assertEquals(Optional.of(SystemAuthorityAccessCode.USER_INACTIVE), events.getLast().code());

        fixture.users.clear();
        fixture.roles.clear();
        fixture.assignments.clear();
        fixture.grants.clear();
        installAuthority(UserStatus.ACTIVE, SystemRoleStatus.ACTIVE);
        assertFalse(service.authorize(
                IDENTITY, SystemPermission.COMPANY_MANAGE, "request-005").authorized());
        assertEquals(
                Optional.of(SystemAuthorityAccessCode.PERMISSION_DENIED),
                events.getLast().code());
    }

    @Test
    void rejectsUnknownPermissionAndInvalidAuthorityContextFailClosed() {
        SystemPermission unknown = new SystemPermission("kernel.future.manage");
        assertFalse(service.authorize(IDENTITY, unknown, "request-006").authorized());
        assertEquals(Optional.of(SystemAuthorityAccessCode.PERMISSION_UNKNOWN), events.getLast().code());

        installAuthority(UserStatus.ACTIVE, SystemRoleStatus.ACTIVE);
        fixture.assignments.add(new AppUserSystemRoleAssignment(
                USER_ID,
                SystemRoleId.parse("00000000-0000-0000-0000-000000000199")));
        assertFalse(service.authorize(
                IDENTITY, SystemPermission.AUDIT_VIEW, "request-007").authorized());
        assertEquals(Optional.of(SystemAuthorityAccessCode.CONTEXT_INVALID), events.getLast().code());
        assertEquals(Optional.of(USER_ID), events.getLast().actorUserId());
        assertEquals(NOW, events.getLast().occurredAt());
    }

    private void installAuthority(UserStatus userStatus, SystemRoleStatus roleStatus) {
        AppUser user = new AppUser(USER_ID, IDENTITY, Optional.of("Administrador"), userStatus, 0);
        SystemRole role = new SystemRole(
                ROLE_ID,
                new SystemRoleCode("system.auditor"),
                "Auditor global",
                roleStatus,
                0);
        fixture.users.put(USER_ID, user);
        fixture.roles.put(ROLE_ID, role);
        fixture.assignments.add(new AppUserSystemRoleAssignment(USER_ID, ROLE_ID));
        fixture.grants.add(new SystemRolePermissionGrant(ROLE_ID, SystemPermission.AUDIT_VIEW));
    }
}
