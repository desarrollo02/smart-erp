package py.com.logixone.kernel.domain.security.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.kernel.domain.security.UserStatus;

class EffectiveSystemPermissionPolicyTest {

    private final EffectiveSystemPermissionPolicy policy = new EffectiveSystemPermissionPolicy();
    private final AppUser activeUser = user("00000000-0000-0000-0000-000000000201", UserStatus.ACTIVE);
    private final SystemRoleId activeRoleId = roleId("00000000-0000-0000-0000-000000000101");
    private final SystemRole activeRole = role(activeRoleId, SystemRoleStatus.ACTIVE, 0);

    @Test
    void resolvesOnlyAvailablePermissionsFromAssignedActiveRoles() {
        EffectiveSystemPermissionResolution result = policy.resolve(
                activeUser,
                List.of(activeRole),
                List.of(new AppUserSystemRoleAssignment(activeUser.id(), activeRoleId)),
                List.of(
                        new SystemRolePermissionGrant(activeRoleId, SystemPermission.COMPANY_MANAGE),
                        new SystemRolePermissionGrant(activeRoleId, SystemPermission.AUDIT_VIEW)),
                Set.of(SystemPermission.COMPANY_MANAGE));

        assertTrue(result.authorized());
        assertEquals(Set.of(SystemPermission.COMPANY_MANAGE), result.permissions());
        assertTrue(result.failure().isEmpty());
    }

    @Test
    void inactiveUserIsDeniedAndInactiveRoleContributesNothing() {
        AppUser inactiveUser = user(activeUser.id().toString(), UserStatus.INACTIVE);
        EffectiveSystemPermissionResolution denied = policy.resolve(
                inactiveUser, List.of(activeRole), List.of(), List.of(), Set.of());

        assertFalse(denied.authorized());
        assertEquals(Optional.of(SystemSecurityDiagnosticCode.USER_INACTIVE), denied.failure());

        SystemRole inactiveRole = role(activeRoleId, SystemRoleStatus.INACTIVE, 0);
        EffectiveSystemPermissionResolution empty = policy.resolve(
                activeUser,
                List.of(inactiveRole),
                List.of(new AppUserSystemRoleAssignment(activeUser.id(), activeRoleId)),
                List.of(new SystemRolePermissionGrant(activeRoleId, SystemPermission.AUDIT_VIEW)),
                Set.of(SystemPermission.AUDIT_VIEW));

        assertTrue(empty.authorized());
        assertTrue(empty.permissions().isEmpty());
    }

    @Test
    void rejectsAssignmentForAnotherUserAndMissingRole() {
        AppUserId anotherUser = new AppUserId(UUID.fromString(
                "00000000-0000-0000-0000-000000000202"));

        assertInvalid(policy.resolve(
                activeUser,
                List.of(activeRole),
                List.of(new AppUserSystemRoleAssignment(anotherUser, activeRoleId)),
                List.of(),
                Set.of()));
        assertInvalid(policy.resolve(
                activeUser,
                List.of(activeRole),
                List.of(new AppUserSystemRoleAssignment(
                        activeUser.id(), roleId("00000000-0000-0000-0000-000000000102"))),
                List.of(),
                Set.of()));
    }

    @Test
    void rejectsGrantForMissingRoleAndConflictingDuplicateRole() {
        SystemRoleId missingRoleId = roleId("00000000-0000-0000-0000-000000000102");
        assertInvalid(policy.resolve(
                activeUser,
                List.of(activeRole),
                List.of(),
                List.of(new SystemRolePermissionGrant(missingRoleId, SystemPermission.AUDIT_VIEW)),
                Set.of(SystemPermission.AUDIT_VIEW)));

        SystemRole conflicting = role(activeRoleId, SystemRoleStatus.INACTIVE, 1);
        assertInvalid(policy.resolve(
                activeUser,
                List.of(activeRole, conflicting),
                List.of(),
                List.of(),
                Set.of()));
    }

    private static void assertInvalid(EffectiveSystemPermissionResolution result) {
        assertFalse(result.authorized());
        assertTrue(result.permissions().isEmpty());
        assertEquals(
                Optional.of(SystemSecurityDiagnosticCode.SYSTEM_ROLE_CONTEXT_INVALID),
                result.failure());
    }

    private static AppUser user(String id, UserStatus status) {
        return new AppUser(
                new AppUserId(UUID.fromString(id)),
                new ExternalIdentity("https://identity.example.test/realms/logixone", "subject-1"),
                Optional.of("Usuario de prueba"),
                status,
                0);
    }

    private static SystemRoleId roleId(String value) {
        return SystemRoleId.parse(value);
    }

    private static SystemRole role(SystemRoleId id, SystemRoleStatus status, long version) {
        return new SystemRole(
                id,
                new SystemRoleCode("system.admin"),
                "Administrador global",
                status,
                version);
    }
}
