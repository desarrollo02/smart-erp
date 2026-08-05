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
import py.com.logixone.kernel.application.company.port.CompanyRepository;
import py.com.logixone.kernel.application.company.port.PersistenceConflictCode;
import py.com.logixone.kernel.application.company.port.PersistenceConflictException;
import py.com.logixone.kernel.domain.company.Company;
import py.com.logixone.plugin.api.PluginId;

@ApplicationScoped
@Transactional
public class JpaCompanyRepository implements CompanyRepository {

    @PersistenceContext(unitName = CorePersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaCompanyRepository() {
    }

    JpaCompanyRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public List<Company> findAll() {
        return entityManager.createQuery("""
                        SELECT company
                        FROM CompanyEntity company
                        ORDER BY company.companyId
                        """, CompanyEntity.class)
                .getResultList()
                .stream()
                .map(CompanyEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Company> findById(CompanyId companyId) {
        Objects.requireNonNull(companyId, "companyId");
        CompanyEntity entity = entityManager.find(CompanyEntity.class, companyId.value());
        return Optional.ofNullable(entity).map(CompanyEntity::toDomain);
    }

    @Override
    public Optional<Company> findByCustomizationPluginId(PluginId customizationPluginId) {
        Objects.requireNonNull(customizationPluginId, "customizationPluginId");
        return entityManager.createQuery("""
                        SELECT company
                        FROM CompanyEntity company
                        WHERE company.customizationPluginId = :pluginId
                        """, CompanyEntity.class)
                .setParameter("pluginId", customizationPluginId.value())
                .getResultStream()
                .findFirst()
                .map(CompanyEntity::toDomain);
    }

    @Override
    public Company save(Company company) {
        Objects.requireNonNull(company, "company");
        CompanyEntity entity = entityManager.find(CompanyEntity.class, company.id().value());
        if (entity == null) {
            if (company.version() != 0) {
                throw new PersistenceConflictException(PersistenceConflictCode.COMPANY_NOT_FOUND);
            }
            entity = CompanyEntity.newEntity(company);
            try {
                entityManager.persist(entity);
                entityManager.flush();
                return entity.toDomain();
            } catch (PersistenceException failure) {
                throw PostgreSqlConflictMapper.company(failure);
            }
        }
        if (entity.version() != company.version()) {
            throw new PersistenceConflictException(PersistenceConflictCode.COMPANY_VERSION_CONFLICT);
        }
        if (entity.hasSameState(company)) {
            return entity.toDomain();
        }
        entity.apply(company);
        try {
            entityManager.flush();
            return entity.toDomain();
        } catch (OptimisticLockException failure) {
            throw new PersistenceConflictException(
                    PersistenceConflictCode.COMPANY_VERSION_CONFLICT, failure);
        } catch (PersistenceException failure) {
            throw PostgreSqlConflictMapper.company(failure);
        }
    }

    @Override
    public boolean isCustomizationAssignedToAnotherCompany(
            PluginId customizationPluginId,
            CompanyId companyId) {
        Objects.requireNonNull(customizationPluginId, "customizationPluginId");
        Objects.requireNonNull(companyId, "companyId");
        Long assignments = entityManager.createQuery("""
                        SELECT COUNT(company)
                        FROM CompanyEntity company
                        WHERE company.customizationPluginId = :pluginId
                          AND company.companyId <> :companyId
                        """, Long.class)
                .setParameter("pluginId", customizationPluginId.value())
                .setParameter("companyId", companyId.value())
                .getSingleResult();
        return assignments > 0;
    }
}
