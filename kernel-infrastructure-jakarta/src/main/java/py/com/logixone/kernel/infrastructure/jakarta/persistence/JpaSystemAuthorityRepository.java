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
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.port.SecurityPersistenceCode;
import py.com.logixone.kernel.application.security.port.SecurityPersistenceException;
import py.com.logixone.kernel.application.security.system.port.SystemAuthorityRepository;
import py.com.logixone.kernel.domain.security.AppUser;
import py.com.logixone.kernel.domain.security.system.AppUserSystemRoleAssignment;
import py.com.logixone.kernel.domain.security.system.SystemRole;
import py.com.logixone.kernel.domain.security.system.SystemRoleCode;
import py.com.logixone.kernel.domain.security.system.SystemRoleId;
import py.com.logixone.kernel.domain.security.system.SystemRolePermissionGrant;

@ApplicationScoped
@Transactional
public class JpaSystemAuthorityRepository implements SystemAuthorityRepository {

    /** Project-owned key; it serializes only global-authority mutations. */
    private static final long AUTHORITY_LOCK_KEY = 7_100_110_400L;

    @PersistenceContext(unitName = CorePersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaSystemAuthorityRepository() {
    }

    JpaSystemAuthorityRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public void lockAuthorityState() {
        try {
            entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:lockKey)")
                    .setParameter("lockKey", AUTHORITY_LOCK_KEY)
                    .getSingleResult();
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public List<AppUser> findAssignedUsers() {
        try {
            return entityManager.createQuery("""
                            SELECT appUser
                            FROM AppUserEntity appUser
                            WHERE appUser.appUserId IN (
                                SELECT assignment.id.appUserId
                                FROM AppUserSystemRoleEntity assignment
                            )
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
    public List<SystemRole> findAllRoles() {
        try {
            return entityManager.createQuery("""
                            SELECT role FROM SystemRoleEntity role
                            ORDER BY role.roleCode, role.systemRoleId
                            """, SystemRoleEntity.class)
                    .getResultStream()
                    .map(SystemRoleEntity::toDomain)
                    .toList();
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public List<AppUserSystemRoleAssignment> findAllAssignments() {
        try {
            return entityManager.createQuery("""
                            SELECT assignment FROM AppUserSystemRoleEntity assignment
                            ORDER BY assignment.id.appUserId, assignment.id.systemRoleId
                            """, AppUserSystemRoleEntity.class)
                    .getResultStream()
                    .map(AppUserSystemRoleEntity::toDomain)
                    .toList();
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public List<SystemRolePermissionGrant> findAllPermissionGrants() {
        try {
            return entityManager.createQuery("""
                            SELECT permission FROM SystemRolePermissionEntity permission
                            ORDER BY permission.id.systemRoleId, permission.id.permissionId
                            """, SystemRolePermissionEntity.class)
                    .getResultStream()
                    .map(SystemRolePermissionEntity::toDomain)
                    .toList();
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public Optional<SystemRole> findRoleById(SystemRoleId roleId) {
        Objects.requireNonNull(roleId, "roleId");
        try {
            return Optional.ofNullable(entityManager.find(SystemRoleEntity.class, roleId.value()))
                    .map(SystemRoleEntity::toDomain);
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public Optional<SystemRole> findRoleByCode(SystemRoleCode roleCode) {
        Objects.requireNonNull(roleCode, "roleCode");
        try {
            return entityManager.createQuery("""
                            SELECT role FROM SystemRoleEntity role
                            WHERE role.roleCode = :roleCode
                            """, SystemRoleEntity.class)
                    .setParameter("roleCode", roleCode.value())
                    .getResultStream()
                    .findFirst()
                    .map(SystemRoleEntity::toDomain);
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public Optional<AppUserSystemRoleAssignment> findAssignment(
            AppUserId userId, SystemRoleId roleId) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(roleId, "roleId");
        try {
            return Optional.ofNullable(entityManager.find(AppUserSystemRoleEntity.class,
                            new AppUserSystemRoleKey(userId, roleId)))
                    .map(AppUserSystemRoleEntity::toDomain);
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public Optional<SystemRolePermissionGrant> findPermissionGrant(
            SystemRoleId roleId, SystemPermission permission) {
        Objects.requireNonNull(roleId, "roleId");
        Objects.requireNonNull(permission, "permission");
        try {
            return Optional.ofNullable(entityManager.find(SystemRolePermissionEntity.class,
                            new SystemRolePermissionKey(roleId, permission)))
                    .map(SystemRolePermissionEntity::toDomain);
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public SystemRole saveRole(SystemRole role) {
        Objects.requireNonNull(role, "role");
        SystemRoleEntity entity;
        try {
            entity = entityManager.find(SystemRoleEntity.class, role.id().value());
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
        if (entity == null) {
            if (role.version() != 0) {
                throw new SecurityPersistenceException(SecurityPersistenceCode.SYSTEM_ROLE_NOT_FOUND);
            }
            if (findRoleByCode(role.code()).isPresent()) {
                throw new SecurityPersistenceException(
                        SecurityPersistenceCode.SYSTEM_ROLE_CODE_ALREADY_EXISTS);
            }
            entity = SystemRoleEntity.newEntity(role);
            try {
                entityManager.persist(entity);
                entityManager.flush();
                return entity.toDomain();
            } catch (PersistenceException failure) {
                throw PostgreSqlSecurityConflictMapper.systemRole(failure);
            }
        }
        if (entity.version() != role.version()) {
            throw new SecurityPersistenceException(
                    SecurityPersistenceCode.SYSTEM_ROLE_VERSION_CONFLICT);
        }
        if (entity.hasSameState(role)) {
            return entity.toDomain();
        }
        entity.apply(role);
        try {
            entityManager.flush();
            return entity.toDomain();
        } catch (OptimisticLockException failure) {
            throw new SecurityPersistenceException(
                    SecurityPersistenceCode.SYSTEM_ROLE_VERSION_CONFLICT, failure);
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.systemRole(failure);
        }
    }

    @Override
    public AppUserSystemRoleAssignment saveAssignment(AppUserSystemRoleAssignment assignment) {
        Objects.requireNonNull(assignment, "assignment");
        AppUserSystemRoleKey key = new AppUserSystemRoleKey(
                assignment.userId(), assignment.roleId());
        try {
            AppUserSystemRoleEntity existing = entityManager.find(
                    AppUserSystemRoleEntity.class, key);
            if (existing != null) {
                return existing.toDomain();
            }
            if (entityManager.find(AppUserEntity.class, assignment.userId().value()) == null) {
                throw new SecurityPersistenceException(SecurityPersistenceCode.USER_NOT_FOUND);
            }
            if (entityManager.find(SystemRoleEntity.class, assignment.roleId().value()) == null) {
                throw new SecurityPersistenceException(SecurityPersistenceCode.SYSTEM_ROLE_NOT_FOUND);
            }
            AppUserSystemRoleEntity entity = AppUserSystemRoleEntity.newEntity(assignment);
            entityManager.persist(entity);
            entityManager.flush();
            return entity.toDomain();
        } catch (SecurityPersistenceException failure) {
            throw failure;
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.systemAssignment(failure);
        }
    }

    @Override
    public SystemRolePermissionGrant savePermissionGrant(SystemRolePermissionGrant grant) {
        Objects.requireNonNull(grant, "grant");
        SystemRolePermissionKey key = new SystemRolePermissionKey(grant.roleId(), grant.permission());
        try {
            SystemRolePermissionEntity existing = entityManager.find(
                    SystemRolePermissionEntity.class, key);
            if (existing != null) {
                return existing.toDomain();
            }
            if (entityManager.find(SystemRoleEntity.class, grant.roleId().value()) == null) {
                throw new SecurityPersistenceException(SecurityPersistenceCode.SYSTEM_ROLE_NOT_FOUND);
            }
            SystemRolePermissionEntity entity = SystemRolePermissionEntity.newEntity(grant);
            entityManager.persist(entity);
            entityManager.flush();
            return entity.toDomain();
        } catch (SecurityPersistenceException failure) {
            throw failure;
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.systemGrant(failure);
        }
    }

    @Override
    public void removeAssignment(AppUserSystemRoleAssignment assignment) {
        Objects.requireNonNull(assignment, "assignment");
        try {
            AppUserSystemRoleEntity entity = entityManager.find(AppUserSystemRoleEntity.class,
                    new AppUserSystemRoleKey(assignment.userId(), assignment.roleId()));
            if (entity != null) {
                entityManager.remove(entity);
                entityManager.flush();
            }
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public void removePermissionGrant(SystemRolePermissionGrant grant) {
        Objects.requireNonNull(grant, "grant");
        try {
            SystemRolePermissionEntity entity = entityManager.find(SystemRolePermissionEntity.class,
                    new SystemRolePermissionKey(grant.roleId(), grant.permission()));
            if (entity != null) {
                entityManager.remove(entity);
                entityManager.flush();
            }
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }
}
