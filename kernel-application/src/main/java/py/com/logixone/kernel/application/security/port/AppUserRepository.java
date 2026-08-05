package py.com.logixone.kernel.application.security.port;

import java.util.List;
import java.util.Optional;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.ExternalIdentity;

public interface AppUserRepository {

    List<AppUser> findAll();

    Optional<AppUser> findById(AppUserId userId);

    Optional<AppUser> findByExternalIdentity(ExternalIdentity externalIdentity);

    /** Persists a new user or an idempotent/versioned replacement and returns stored state. */
    AppUser save(AppUser user);
}
