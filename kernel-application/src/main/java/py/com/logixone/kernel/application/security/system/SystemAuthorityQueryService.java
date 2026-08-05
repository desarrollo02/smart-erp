package py.com.logixone.kernel.application.security.system;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.port.AppUserRepository;
import py.com.logixone.kernel.application.security.system.port.SystemAuthorityRepository;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.system.EffectiveSystemPermissionPolicy;
import py.com.logixone.kernel.domain.security.system.EffectiveSystemPermissionResolution;

/** Read-only resolution used by the future trusted administrative web boundary. */
public final class SystemAuthorityQueryService {

    private final AppUserRepository userRepository;
    private final SystemAuthorityRepository authorityRepository;
    private final EffectiveSystemPermissionPolicy permissionPolicy =
            new EffectiveSystemPermissionPolicy();

    public SystemAuthorityQueryService(
            AppUserRepository userRepository,
            SystemAuthorityRepository authorityRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.authorityRepository = Objects.requireNonNull(authorityRepository, "authorityRepository");
    }

    public Optional<EffectiveSystemPermissionResolution> resolvePermissions(AppUserId userId) {
        Objects.requireNonNull(userId, "userId");
        AppUser user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Optional.empty();
        }
        return Optional.of(permissionPolicy.resolve(
                user,
                authorityRepository.findAllRoles(),
                authorityRepository.findAllAssignments().stream()
                        .filter(assignment -> assignment.userId().equals(userId))
                        .toList(),
                authorityRepository.findAllPermissionGrants(),
                SystemPermission.knownPermissions()));
    }
}
