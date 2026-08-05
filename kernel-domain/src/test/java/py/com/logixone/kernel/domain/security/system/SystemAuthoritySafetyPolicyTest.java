package py.com.logixone.kernel.domain.security.system;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.ExternalIdentity;
import py.com.logixone.kernel.domain.security.UserStatus;

class SystemAuthoritySafetyPolicyTest {

    private final SystemAuthoritySafetyPolicy policy = new SystemAuthoritySafetyPolicy();
    private final AppUser administrator = user(
            "00000000-0000-0000-0000-000000000201", UserStatus.ACTIVE, 0);
    private final SystemRoleId administratorRoleId =
            SystemRoleId.parse("00000000-0000-0000-0000-000000000101");
    private final SystemRole administratorRole = role(
            administratorRoleId, SystemRoleStatus.ACTIVE, 0);
    private final AppUserSystemRoleAssignment assignment =
            new AppUserSystemRoleAssignment(administrator.id(), administratorRoleId);
    private final SystemRolePermissionGrant administratorGrant =
            new SystemRolePermissionGrant(
                    administratorRoleId,
                    SystemPermission.SYSTEM_ADMINISTRATION_MANAGE);

    @Test
    void acceptsOneOrSeveralEffectiveAdministrators() {
        assertEquals(
                SystemAuthoritySafetyStatus.SAFE,
                evaluate(
                        List.of(administrator),
                        List.of(administratorRole),
                        List.of(assignment),
                        List.of(administratorGrant)));

        AppUser second = user(
                "00000000-0000-0000-0000-000000000202", UserStatus.ACTIVE, 0);
        assertEquals(
                SystemAuthoritySafetyStatus.SAFE,
                evaluate(
                        List.of(administrator, second),
                        List.of(administratorRole),
                        List.of(
                                assignment,
                                new AppUserSystemRoleAssignment(second.id(), administratorRoleId)),
                        List.of(administratorGrant)));
    }

    @Test
    void rejectsEveryWayOfRemovingTheLastEffectiveAdministrator() {
        assertEquals(
                SystemAuthoritySafetyStatus.ADMINISTRATOR_REQUIRED,
                evaluate(List.of(administrator), List.of(administratorRole), List.of(), List.of(administratorGrant)));
        assertEquals(
                SystemAuthoritySafetyStatus.ADMINISTRATOR_REQUIRED,
                evaluate(List.of(administrator), List.of(administratorRole), List.of(assignment), List.of()));
        assertEquals(
                SystemAuthoritySafetyStatus.ADMINISTRATOR_REQUIRED,
                evaluate(
                        List.of(user(administrator.id().toString(), UserStatus.INACTIVE, 1)),
                        List.of(administratorRole),
                        List.of(assignment),
                        List.of(administratorGrant)));
        assertEquals(
                SystemAuthoritySafetyStatus.ADMINISTRATOR_REQUIRED,
                evaluate(
                        List.of(administrator),
                        List.of(role(administratorRoleId, SystemRoleStatus.INACTIVE, 1)),
                        List.of(assignment),
                        List.of(administratorGrant)));
    }

    @Test
    void rejectsMissingReferencesInAssignmentsAndGrants() {
        AppUserId missingUser = new AppUserId(UUID.fromString(
                "00000000-0000-0000-0000-000000000299"));
        SystemRoleId missingRole =
                SystemRoleId.parse("00000000-0000-0000-0000-000000000199");

        assertEquals(
                SystemAuthoritySafetyStatus.INVALID_CONTEXT,
                evaluate(
                        List.of(administrator),
                        List.of(administratorRole),
                        List.of(new AppUserSystemRoleAssignment(missingUser, administratorRoleId)),
                        List.of(administratorGrant)));
        assertEquals(
                SystemAuthoritySafetyStatus.INVALID_CONTEXT,
                evaluate(
                        List.of(administrator),
                        List.of(administratorRole),
                        List.of(new AppUserSystemRoleAssignment(administrator.id(), missingRole)),
                        List.of(administratorGrant)));
        assertEquals(
                SystemAuthoritySafetyStatus.INVALID_CONTEXT,
                evaluate(
                        List.of(administrator),
                        List.of(administratorRole),
                        List.of(assignment),
                        List.of(new SystemRolePermissionGrant(
                                missingRole, SystemPermission.SYSTEM_ADMINISTRATION_MANAGE))));
    }

    @Test
    void rejectsConflictingDuplicateUsersAndRoles() {
        List<AppUser> users = new ArrayList<>();
        users.add(administrator);
        users.add(user(administrator.id().toString(), UserStatus.INACTIVE, 1));
        assertEquals(
                SystemAuthoritySafetyStatus.INVALID_CONTEXT,
                evaluate(users, List.of(administratorRole), List.of(assignment), List.of(administratorGrant)));

        SystemRole conflicting = role(administratorRoleId, SystemRoleStatus.INACTIVE, 1);
        assertEquals(
                SystemAuthoritySafetyStatus.INVALID_CONTEXT,
                evaluate(
                        List.of(administrator),
                        List.of(administratorRole, conflicting),
                        List.of(assignment),
                        List.of(administratorGrant)));
    }

    private SystemAuthoritySafetyStatus evaluate(
            List<AppUser> users,
            List<SystemRole> roles,
            List<AppUserSystemRoleAssignment> assignments,
            List<SystemRolePermissionGrant> grants) {
        return policy.evaluate(users, roles, assignments, grants);
    }

    private static AppUser user(String id, UserStatus status, long version) {
        return new AppUser(
                new AppUserId(UUID.fromString(id)),
                new ExternalIdentity("https://identity.example.test/realms/logixone", "subject-" + id),
                Optional.of("Usuario de prueba"),
                status,
                version);
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
