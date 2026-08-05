package py.com.logixone.plugins.businesspartners.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerDefinitionRepository;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerPersistenceCode;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerPersistenceException;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAttributeCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionKind;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionRevision;

@ApplicationScoped
@Transactional
public class JpaBusinessPartnerDefinitionRepository implements BusinessPartnerDefinitionRepository {

    @PersistenceContext(unitName = BusinessPartnersPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaBusinessPartnerDefinitionRepository() {
    }

    JpaBusinessPartnerDefinitionRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public List<BusinessPartnerDefinition> findAll(
            CompanyId companyId, BusinessPartnerDefinitionKind kind) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(kind, "kind");
        return entityManager.createQuery("""
                        SELECT definition
                        FROM BusinessPartnerDefinitionEntity definition
                        WHERE definition.id.companyId = :companyId
                          AND definition.id.kind = :kind
                        ORDER BY definition.displayName, definition.id.code
                        """, BusinessPartnerDefinitionEntity.class)
                .setParameter("companyId", companyId.value())
                .setParameter("kind", kind)
                .getResultList().stream()
                .map(BusinessPartnerDefinitionEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<BusinessPartnerDefinition> findByCode(
            CompanyId companyId,
            BusinessPartnerDefinitionKind kind,
            BusinessPartnerAttributeCode code) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(code, "code");
        BusinessPartnerDefinitionEntity entity = entityManager.find(
                BusinessPartnerDefinitionEntity.class,
                new BusinessPartnerDefinitionEntityId(companyId.value(), kind, code.value()));
        return Optional.ofNullable(entity).map(BusinessPartnerDefinitionEntity::toDomain);
    }

    @Override
    public Optional<BusinessPartnerDefinition> findByCodeForReference(
            CompanyId companyId,
            BusinessPartnerDefinitionKind kind,
            BusinessPartnerAttributeCode code) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(code, "code");
        BusinessPartnerDefinitionEntity entity = entityManager.find(
                BusinessPartnerDefinitionEntity.class,
                new BusinessPartnerDefinitionEntityId(companyId.value(), kind, code.value()),
                LockModeType.PESSIMISTIC_READ);
        return Optional.ofNullable(entity).map(BusinessPartnerDefinitionEntity::toDomain);
    }

    @Override
    public List<BusinessPartnerDefinitionRevision> history(
            CompanyId companyId,
            BusinessPartnerDefinitionKind kind,
            BusinessPartnerAttributeCode code) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(code, "code");
        return entityManager.createQuery("""
                        SELECT revision
                        FROM BusinessPartnerDefinitionRevisionEntity revision
                        WHERE revision.id.companyId = :companyId
                          AND revision.id.kind = :kind
                          AND revision.id.code = :code
                        ORDER BY revision.id.version DESC
                        """, BusinessPartnerDefinitionRevisionEntity.class)
                .setParameter("companyId", companyId.value())
                .setParameter("kind", kind)
                .setParameter("code", code.value())
                .getResultList().stream()
                .map(BusinessPartnerDefinitionRevisionEntity::toDomain)
                .toList();
    }

    @Override
    public BusinessPartnerDefinition insert(BusinessPartnerDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        try {
            BusinessPartnerDefinitionEntity entity = BusinessPartnerDefinitionEntity.from(definition);
            entityManager.persist(entity);
            entityManager.flush();
            BusinessPartnerDefinition inserted = entity.toDomain();
            entityManager.persist(BusinessPartnerDefinitionRevisionEntity.from(inserted));
            entityManager.flush();
            return inserted;
        } catch (PersistenceException failure) {
            throw PostgreSqlBusinessPartnerConflictMapper.map(failure);
        }
    }

    @Override
    public BusinessPartnerDefinition update(
            BusinessPartnerDefinition definition, long expectedPersistedVersion) {
        Objects.requireNonNull(definition, "definition");
        if (expectedPersistedVersion < 0) {
            throw new IllegalArgumentException("expectedPersistedVersion must not be negative");
        }
        BusinessPartnerDefinitionEntity entity = entityManager.find(
                BusinessPartnerDefinitionEntity.class,
                new BusinessPartnerDefinitionEntityId(
                        definition.companyId().value(),
                        definition.kind(),
                        definition.code().value()));
        if (entity == null) {
            throw new BusinessPartnerPersistenceException(
                    BusinessPartnerPersistenceCode.DEFINITION_NOT_FOUND);
        }
        if (entity.version() != expectedPersistedVersion) {
            throw new BusinessPartnerPersistenceException(
                    BusinessPartnerPersistenceCode.VERSION_CONFLICT);
        }
        if (definition.version() != expectedPersistedVersion + 1) {
            throw new IllegalArgumentException(
                    "definition version must advance expectedPersistedVersion by one");
        }
        try {
            entity.apply(definition);
            entityManager.flush();
            BusinessPartnerDefinition updated = entity.toDomain();
            entityManager.persist(BusinessPartnerDefinitionRevisionEntity.from(updated));
            entityManager.flush();
            return updated;
        } catch (OptimisticLockException failure) {
            throw new BusinessPartnerPersistenceException(
                    BusinessPartnerPersistenceCode.VERSION_CONFLICT, failure);
        } catch (PersistenceException failure) {
            throw PostgreSqlBusinessPartnerConflictMapper.map(failure);
        }
    }
}
