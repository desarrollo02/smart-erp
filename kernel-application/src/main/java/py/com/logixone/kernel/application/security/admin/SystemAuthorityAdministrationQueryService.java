package py.com.logixone.kernel.application.security.admin;

import java.util.List;
import java.util.Objects;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.port.AppUserRepository;
import py.com.logixone.kernel.application.security.system.port.SystemAuthorityRepository;
import py.com.logixone.kernel.domain.security.system.AppUserSystemRoleAssignment;
import py.com.logixone.kernel.domain.security.system.SystemRolePermissionGrant;

/** Neutral read model for kernel-wide authority without company ownership. */
public final class SystemAuthorityAdministrationQueryService {

    private final AppUserRepository userRepository;
    private final SystemAuthorityRepository authorityRepository;

    public SystemAuthorityAdministrationQueryService(
            AppUserRepository userRepository,
            SystemAuthorityRepository authorityRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.authorityRepository = Objects.requireNonNull(authorityRepository, "authorityRepository");
    }

    public SystemAuthorityAdministrationSnapshot snapshot() {
        List<AppUserSystemRoleAssignment> assignments = authorityRepository.findAllAssignments();
        List<SystemRolePermissionGrant> grants = authorityRepository.findAllPermissionGrants();
        return new SystemAuthorityAdministrationSnapshot(
                userRepository.findAll().stream()
                        .sorted((left, right) -> left.id().compareTo(right.id()))
                        .map(SecurityUserView::from)
                        .toList(),
                authorityRepository.findAllRoles().stream()
                        .map(role -> new SystemRoleAdministrationView(
                                role.id(),
                                role.code().value(),
                                role.displayName(),
                                role.status(),
                                role.version(),
                                assignments.stream()
                                        .filter(assignment -> assignment.roleId().equals(role.id()))
                                        .map(AppUserSystemRoleAssignment::userId)
                                        .sorted()
                                        .toList(),
                                grants.stream()
                                        .filter(grant -> grant.roleId().equals(role.id()))
                                        .map(SystemRolePermissionGrant::permission)
                                        .sorted()
                                        .toList()))
                        .toList(),
                SystemPermission.knownPermissions().stream().sorted().toList());
    }
}
