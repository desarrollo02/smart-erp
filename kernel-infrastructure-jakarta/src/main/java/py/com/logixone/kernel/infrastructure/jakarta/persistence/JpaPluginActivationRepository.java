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
import py.com.logixone.kernel.application.company.port.PersistenceConflictCode;
import py.com.logixone.kernel.application.company.port.PersistenceConflictException;
import py.com.logixone.kernel.application.company.port.PluginActivationRepository;
import py.com.logixone.kernel.domain.company.PluginActivationDecision;
import py.com.logixone.plugin.api.PluginId;

@ApplicationScoped
@Transactional
public class JpaPluginActivationRepository implements PluginActivationRepository {

    @PersistenceContext(unitName = CorePersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaPluginActivationRepository() {
    }

    JpaPluginActivationRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public List<PluginActivationDecision> findByCompanyId(CompanyId companyId) {
        Objects.requireNonNull(companyId, "companyId");
        return entityManager.createQuery("""
                        SELECT activation
                        FROM PluginActivationEntity activation
                        WHERE activation.id.companyId = :companyId
                        ORDER BY activation.id.pluginId
                        """, PluginActivationEntity.class)
                .setParameter("companyId", companyId.value())
                .getResultStream()
                .map(PluginActivationEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<PluginActivationDecision> findByCompanyAndPlugin(
            CompanyId companyId,
            PluginId pluginId) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(pluginId, "pluginId");
        PluginActivationEntity entity = entityManager.find(
                PluginActivationEntity.class,
                new PluginActivationKey(companyId, pluginId));
        return Optional.ofNullable(entity).map(PluginActivationEntity::toDomain);
    }

    @Override
    public PluginActivationDecision save(PluginActivationDecision decision) {
        Objects.requireNonNull(decision, "decision");
        PluginActivationKey key = new PluginActivationKey(decision.companyId(), decision.pluginId());
        PluginActivationEntity entity = entityManager.find(PluginActivationEntity.class, key);
        if (entity == null) {
            if (decision.version() != 0) {
                throw new PersistenceConflictException(PersistenceConflictCode.ACTIVATION_NOT_FOUND);
            }
            if (entityManager.find(CompanyEntity.class, decision.companyId().value()) == null) {
                throw new PersistenceConflictException(PersistenceConflictCode.COMPANY_NOT_FOUND);
            }
            entity = PluginActivationEntity.newEntity(decision);
            try {
                entityManager.persist(entity);
                entityManager.flush();
                return entity.toDomain();
            } catch (PersistenceException failure) {
                throw PostgreSqlConflictMapper.activation(failure);
            }
        }
        if (entity.version() != decision.version()) {
            throw new PersistenceConflictException(PersistenceConflictCode.ACTIVATION_VERSION_CONFLICT);
        }
        if (entity.hasSameState(decision)) {
            return entity.toDomain();
        }
        entity.apply(decision);
        try {
            entityManager.flush();
            return entity.toDomain();
        } catch (OptimisticLockException failure) {
            throw new PersistenceConflictException(
                    PersistenceConflictCode.ACTIVATION_VERSION_CONFLICT, failure);
        } catch (PersistenceException failure) {
            throw PostgreSqlConflictMapper.activation(failure);
        }
    }
}
