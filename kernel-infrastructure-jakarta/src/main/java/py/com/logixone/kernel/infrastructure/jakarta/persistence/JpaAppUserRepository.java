package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.application.security.port.AppUserRepository;
import py.com.logixone.kernel.application.security.port.SecurityPersistenceCode;
import py.com.logixone.kernel.application.security.port.SecurityPersistenceException;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.ExternalIdentity;

@ApplicationScoped
@Transactional
public class JpaAppUserRepository implements AppUserRepository {

    @PersistenceContext(unitName = CorePersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaAppUserRepository() {
    }

    JpaAppUserRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public List<AppUser> findAll() {
        try {
            return entityManager.createQuery("""
                            SELECT appUser
                            FROM AppUserEntity appUser
                            ORDER BY appUser.appUserId
                            """, AppUserEntity.class)
                    .getResultStream()
                    .map(AppUserEntity::toDomain)
                    .toList();
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public Optional<AppUser> findById(AppUserId userId) {
        Objects.requireNonNull(userId, "userId");
        try {
            return Optional.ofNullable(entityManager.find(AppUserEntity.class, userId.value()))
                    .map(AppUserEntity::toDomain);
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public Optional<AppUser> findByExternalIdentity(ExternalIdentity externalIdentity) {
        Objects.requireNonNull(externalIdentity, "externalIdentity");
        try {
            return entityManager.createQuery("""
                            SELECT appUser
                            FROM AppUserEntity appUser
                            WHERE appUser.issuer = :issuer
                              AND appUser.subject = :subject
                            """, AppUserEntity.class)
                    .setParameter("issuer", externalIdentity.issuer())
                    .setParameter("subject", externalIdentity.subject())
                    .getResultStream()
                    .findFirst()
                    .map(AppUserEntity::toDomain);
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public AppUser save(AppUser user) {
        Objects.requireNonNull(user, "user");
        AppUserEntity entity;
        try {
            entity = entityManager.find(AppUserEntity.class, user.id().value());
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
        if (entity == null) {
            if (user.version() != 0) {
                throw new SecurityPersistenceException(SecurityPersistenceCode.USER_NOT_FOUND);
            }
            Optional<AppUser> assigned = findByExternalIdentity(user.externalIdentity());
            if (assigned.isPresent()) {
                throw new SecurityPersistenceException(
                        SecurityPersistenceCode.EXTERNAL_IDENTITY_ALREADY_EXISTS);
            }
            entity = AppUserEntity.newEntity(user);
            try {
                entityManager.persist(entity);
                entityManager.flush();
                return entity.toDomain();
            } catch (PersistenceException failure) {
                throw PostgreSqlSecurityConflictMapper.user(failure);
            }
        }
        if (entity.version() != user.version()) {
            throw new SecurityPersistenceException(SecurityPersistenceCode.USER_VERSION_CONFLICT);
        }
        if (entity.hasSameState(user)) {
            return entity.toDomain();
        }
        entity.apply(user);
        try {
            entityManager.flush();
            return entity.toDomain();
        } catch (OptimisticLockException failure) {
            throw new SecurityPersistenceException(
                    SecurityPersistenceCode.USER_VERSION_CONFLICT, failure);
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.user(failure);
        }
    }
}
