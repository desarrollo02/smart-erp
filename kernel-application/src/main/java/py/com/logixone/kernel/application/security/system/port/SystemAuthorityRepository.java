package py.com.logixone.kernel.application.security.system.port;

import java.util.List;
import java.util.Optional;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.system.AppUserSystemRoleAssignment;
import py.com.logixone.kernel.domain.security.system.SystemRole;
import py.com.logixone.kernel.domain.security.system.SystemRoleCode;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;
import py.com.logixone.kernel.domain.security.system.SystemRolePermissionGrant;

/** Neutral persistence boundary for kernel-wide roles, assignments and grants. */
public interface SystemAuthorityRepository {

    /** Serializes authority-reducing changes for the lifetime of the active transaction. */
    void lockAuthorityState();

    List<AppUser> findAssignedUsers();

    List<SystemRole> findAllRoles();

    List<AppUserSystemRoleAssignment> findAllAssignments();

    List<SystemRolePermissionGrant> findAllPermissionGrants();

    Optional<SystemRole> findRoleById(SystemRoleId roleId);

    Optional<SystemRole> findRoleByCode(SystemRoleCode roleCode);

    Optional<AppUserSystemRoleAssignment> findAssignment(
            AppUserId userId, SystemRoleId roleId);

    Optional<SystemRolePermissionGrant> findPermissionGrant(
            SystemRoleId roleId, SystemPermission permission);

    SystemRole saveRole(SystemRole role);

    AppUserSystemRoleAssignment saveAssignment(AppUserSystemRoleAssignment assignment);

    SystemRolePermissionGrant savePermissionGrant(SystemRolePermissionGrant grant);

    void removeAssignment(AppUserSystemRoleAssignment assignment);

    void removePermissionGrant(SystemRolePermissionGrant grant);
}
