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
import py.com.logixone.kernel.application.security.port.CompanyMembershipRepository;
import py.com.logixone.kernel.application.security.port.SecurityPersistenceCode;
import py.com.logixone.kernel.application.security.port.SecurityPersistenceException;
import py.com.logixone.kernel.domain.security.CompanyMembership;

@ApplicationScoped
@Transactional
public class JpaCompanyMembershipRepository implements CompanyMembershipRepository {

    @PersistenceContext(unitName = CorePersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaCompanyMembershipRepository() {
    }

    JpaCompanyMembershipRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public List<CompanyMembership> findByUserId(AppUserId userId) {
        Objects.requireNonNull(userId, "userId");
        try {
            return entityManager.createQuery("""
                            SELECT membership
                            FROM CompanyMembershipEntity membership
                            WHERE membership.id.appUserId = :userId
                            ORDER BY membership.id.companyId
                            """, CompanyMembershipEntity.class)
                    .setParameter("userId", userId.value())
                    .getResultStream()
                    .map(CompanyMembershipEntity::toDomain)
                    .toList();
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public List<CompanyMembership> findByCompanyId(CompanyId companyId) {
        Objects.requireNonNull(companyId, "companyId");
        try {
            return entityManager.createQuery("""
                            SELECT membership
                            FROM CompanyMembershipEntity membership
                            WHERE membership.id.companyId = :companyId
                            ORDER BY membership.id.appUserId
                            """, CompanyMembershipEntity.class)
                    .setParameter("companyId", companyId.value())
                    .getResultStream()
                    .map(CompanyMembershipEntity::toDomain)
                    .toList();
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public Optional<CompanyMembership> findByUserAndCompany(
            AppUserId userId,
            CompanyId companyId) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(companyId, "companyId");
        try {
            return Optional.ofNullable(entityManager.find(
                            CompanyMembershipEntity.class,
                            new CompanyMembershipKey(userId, companyId)))
                    .map(CompanyMembershipEntity::toDomain);
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }

    @Override
    public CompanyMembership save(CompanyMembership membership) {
        Objects.requireNonNull(membership, "membership");
        CompanyMembershipKey key = new CompanyMembershipKey(
                membership.userId(), membership.companyId());
        CompanyMembershipEntity entity;
        try {
            entity = entityManager.find(CompanyMembershipEntity.class, key);
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
        if (entity == null) {
            if (membership.version() != 0) {
                throw new SecurityPersistenceException(SecurityPersistenceCode.MEMBERSHIP_NOT_FOUND);
            }
            requireParents(membership);
            entity = CompanyMembershipEntity.newEntity(membership);
            try {
                entityManager.persist(entity);
                entityManager.flush();
                return entity.toDomain();
            } catch (PersistenceException failure) {
                throw PostgreSqlSecurityConflictMapper.membership(failure);
            }
        }
        if (entity.version() != membership.version()) {
            throw new SecurityPersistenceException(
                    SecurityPersistenceCode.MEMBERSHIP_VERSION_CONFLICT);
        }
        if (entity.hasSameState(membership)) {
            return entity.toDomain();
        }
        entity.apply(membership);
        try {
            entityManager.flush();
            return entity.toDomain();
        } catch (OptimisticLockException failure) {
            throw new SecurityPersistenceException(
                    SecurityPersistenceCode.MEMBERSHIP_VERSION_CONFLICT, failure);
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.membership(failure);
        }
    }

    private void requireParents(CompanyMembership membership) {
        try {
            if (entityManager.find(AppUserEntity.class, membership.userId().value()) == null) {
                throw new SecurityPersistenceException(SecurityPersistenceCode.USER_NOT_FOUND);
            }
            if (entityManager.find(CompanyEntity.class, membership.companyId().value()) == null) {
                throw new SecurityPersistenceException(SecurityPersistenceCode.COMPANY_NOT_FOUND);
            }
        } catch (PersistenceException failure) {
            throw PostgreSqlSecurityConflictMapper.generic(failure);
        }
    }
}
