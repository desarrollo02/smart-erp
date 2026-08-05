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
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.application.security.port.CompanyAuthorizationRepository;
import py.com.logixone.kernel.application.security.port.SecurityPersistenceCode;
import py.com.logixone.kernel.application.security.port.SecurityPersistenceException;
import py.com.logixone.kernel.domain.security.CompanyRole;
import py.com.logixone.kernel.domain.security.MembershipRoleAssignment;
import py.com.logixone.kernel.domain.security.RoleCode;
import py.com.logixone.kernel.domain.security.RoleId;
import py.com.logixone.kernel.domain.security.RolePermissionGrant;
import py.com.logixone.plugin.api.ContributionId;

@ApplicationScoped
@Transactional
public class JpaCompanyAuthorizationRepository implements CompanyAuthorizationRepository {

    @PersistenceContext(unitName = CorePersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaCompanyAuthorizationRepository() {
    }

    JpaCompanyAuthorizationRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public List<CompanyRole> findRolesByCompanyId(CompanyId companyId) {
        Objects.requireNonNull(companyId, "companyId");
        try {
            return entityManager.createQuery("""
                            SELECT role
                            FROM SecurityRoleEntity role
                            WHERE role.companyId = :companyId
                            ORDER BY role.roleCode, role.roleId
                            """, SecurityRoleEntity.class)
                    .setParameter("companyId", companyId.value())
                    .getResultStream()
                    .map(SecurityRoleEntity::toDomain)
                    .toList();
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public Optional<CompanyRole> findRoleById(RoleId roleId) {
        Objects.requireNonNull(roleId, "roleId");
        try {
            return Optional.ofNullable(entityManager.find(SecurityRoleEntity.class, roleId.value()))
                    .map(SecurityRoleEntity::toDomain);
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public Optional<CompanyRole> findRoleByCompanyAndCode(
            CompanyId companyId,
            RoleCode roleCode) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(roleCode, "roleCode");
        try {
            return entityManager.createQuery("""
                            SELECT role
                            FROM SecurityRoleEntity role
                            WHERE role.companyId = :companyId
                              AND role.roleCode = :roleCode
                            """, SecurityRoleEntity.class)
                    .setParameter("companyId", companyId.value())
                    .setParameter("roleCode", roleCode.value())
                    .getResultStream()
                    .findFirst()
                    .map(SecurityRoleEntity::toDomain);
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public List<MembershipRoleAssignment> findAssignments(
            AppUserId userId,
            CompanyId companyId) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(companyId, "companyId");
        try {
            return entityManager.createQuery("""
                            SELECT assignment
                            FROM MembershipRoleEntity assignment
                            WHERE assignment.id.appUserId = :userId
                              AND assignment.id.companyId = :companyId
                            ORDER BY assignment.id.roleId
                            """, MembershipRoleEntity.class)
                    .setParameter("userId", userId.value())
                    .setParameter("companyId", companyId.value())
                    .getResultStream()
                    .map(MembershipRoleEntity::toDomain)
                    .toList();
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public List<MembershipRoleAssignment> findAssignmentsByCompanyId(CompanyId companyId) {
        Objects.requireNonNull(companyId, "companyId");
        try {
            return entityManager.createQuery("""
                            SELECT assignment
                            FROM MembershipRoleEntity assignment
                            WHERE assignment.id.companyId = :companyId
                            ORDER BY assignment.id.appUserId, assignment.id.roleId
                            """, MembershipRoleEntity.class)
                    .setParameter("companyId", companyId.value())
                    .getResultStream()
                    .map(MembershipRoleEntity::toDomain)
                    .toList();
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public Optional<MembershipRoleAssignment> findAssignment(
            AppUserId userId,
            CompanyId companyId,
            RoleId roleId) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(roleId, "roleId");
        try {
            return Optional.ofNullable(entityManager.find(
                            MembershipRoleEntity.class,
                            new MembershipRoleKey(userId, companyId, roleId)))
                    .map(MembershipRoleEntity::toDomain);
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public List<RolePermissionGrant> findPermissionGrants(CompanyId companyId) {
        Objects.requireNonNull(companyId, "companyId");
        try {
            return entityManager.createQuery("""
                            SELECT permission
                            FROM RolePermissionEntity permission
                            WHERE permission.id.companyId = :companyId
                            ORDER BY permission.id.roleId, permission.id.permissionId
                            """, RolePermissionEntity.class)
                    .setParameter("companyId", companyId.value())
                    .getResultStream()
                    .map(RolePermissionEntity::toDomain)
                    .toList();
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public Optional<RolePermissionGrant> findPermissionGrant(
            CompanyId companyId,
            RoleId roleId,
            ContributionId permissionId) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(roleId, "roleId");
        Objects.requireNonNull(permissionId, "permissionId");
        try {
            return Optional.ofNullable(entityManager.find(
                            RolePermissionEntity.class,
                            new RolePermissionKey(companyId, roleId, permissionId)))
                    .map(RolePermissionEntity::toDomain);
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public CompanyRole saveRole(CompanyRole role) {
        Objects.requireNonNull(role, "role");
        SecurityRoleEntity entity;
        try {
            entity = entityManager.find(SecurityRoleEntity.class, role.id().value());
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
        if (entity == null) {
            if (role.version() != 0) {
                throw new SecurityPersistenceException(SecurityPersistenceCode.ROLE_NOT_FOUND);
            }
            requireCompany(role.companyId());
            Optional<CompanyRole> assigned = findRoleByCompanyAndCode(role.companyId(), role.code());
            if (assigned.isPresent()) {
                throw new SecurityPersistenceException(SecurityPersistenceCode.ROLE_CODE_ALREADY_EXISTS);
            }
            entity = SecurityRoleEntity.newEntity(role);
            try {
                entityManager.persist(entity);
                entityManager.flush();
                return entity.toDomain();
            } catch (PersistenceException failure) {
                throw PostgreSqlSecurityConflictMapper.role(failure);
            }
        }
        if (entity.version() != role.version()) {
            throw new SecurityPersistenceException(SecurityPersistenceCode.ROLE_VERSION_CONFLICT);
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
                    SecurityPersistenceCode.ROLE_VERSION_CONFLICT, failure);
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.role(failure);
        }
    }

    @Override
    public MembershipRoleAssignment saveAssignment(MembershipRoleAssignment assignment) {
        Objects.requireNonNull(assignment, "assignment");
        MembershipRoleKey key = new MembershipRoleKey(
                assignment.userId(), assignment.companyId(), assignment.roleId());
        try {
            MembershipRoleEntity existing = entityManager.find(MembershipRoleEntity.class, key);
            if (existing != null) {
                return existing.toDomain();
            }
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
        requireAssignmentParents(assignment);
        MembershipRoleEntity entity = MembershipRoleEntity.newEntity(assignment);
        try {
            entityManager.persist(entity);
            entityManager.flush();
            return entity.toDomain();
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.assignment(failure);
        }
    }

    @Override
    public RolePermissionGrant savePermissionGrant(RolePermissionGrant grant) {
        Objects.requireNonNull(grant, "grant");
        RolePermissionKey key = new RolePermissionKey(
                grant.companyId(), grant.roleId(), grant.permissionId());
        try {
            RolePermissionEntity existing = entityManager.find(RolePermissionEntity.class, key);
            if (existing != null) {
                return existing.toDomain();
            }
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
        requireRoleInCompany(grant.roleId(), grant.companyId());
        RolePermissionEntity entity = RolePermissionEntity.newEntity(grant);
        try {
            entityManager.persist(entity);
            entityManager.flush();
            return entity.toDomain();
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.grant(failure);
        }
    }

    @Override
    public boolean removeAssignment(MembershipRoleAssignment assignment) {
        Objects.requireNonNull(assignment, "assignment");
        MembershipRoleKey key = new MembershipRoleKey(
                assignment.userId(), assignment.companyId(), assignment.roleId());
        try {
            MembershipRoleEntity entity = entityManager.find(MembershipRoleEntity.class, key);
            if (entity == null) {
                return false;
            }
            entityManager.remove(entity);
            entityManager.flush();
            return true;
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public boolean removePermissionGrant(RolePermissionGrant grant) {
        Objects.requireNonNull(grant, "grant");
        RolePermissionKey key = new RolePermissionKey(
                grant.companyId(), grant.roleId(), grant.permissionId());
        try {
            RolePermissionEntity entity = entityManager.find(RolePermissionEntity.class, key);
            if (entity == null) {
                return false;
            }
            entityManager.remove(entity);
            entityManager.flush();
            return true;
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    private void requireCompany(CompanyId companyId) {
        try {
            if (entityManager.find(CompanyEntity.class, companyId.value()) == null) {
                throw new SecurityPersistenceException(SecurityPersistenceCode.COMPANY_NOT_FOUND);
            }
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    private void requireAssignmentParents(MembershipRoleAssignment assignment) {
        try {
            CompanyMembershipKey membershipKey = new CompanyMembershipKey(
                    assignment.userId(), assignment.companyId());
            if (entityManager.find(CompanyMembershipEntity.class, membershipKey) == null) {
                throw new SecurityPersistenceException(SecurityPersistenceCode.MEMBERSHIP_NOT_FOUND);
            }
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
        requireRoleInCompany(assignment.roleId(), assignment.companyId());
    }

    private void requireRoleInCompany(RoleId roleId, CompanyId companyId) {
        CompanyRole role = findRoleById(roleId).orElseThrow(
                () -> new SecurityPersistenceException(SecurityPersistenceCode.ROLE_NOT_FOUND));
        if (!role.companyId().equals(companyId)) {
            throw new SecurityPersistenceException(SecurityPersistenceCode.ROLE_COMPANY_MISMATCH);
        }
    }
}
