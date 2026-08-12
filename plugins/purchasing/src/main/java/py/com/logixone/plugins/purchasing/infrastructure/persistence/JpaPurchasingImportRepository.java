package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.purchasing.application.port.PurchasingImportRecord;
import py.com.logixone.plugins.purchasing.application.port.PurchasingImportRepository;

@ApplicationScoped
@Transactional
public class JpaPurchasingImportRepository implements PurchasingImportRepository {
    @PersistenceContext(unitName = PurchasingPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaPurchasingImportRepository() { }

    JpaPurchasingImportRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public Optional<PurchasingImportRecord> find(
            CompanyId companyId, String sourceSystem, String sourceRecordKey) {
        return Optional.ofNullable(entityManager.find(
                        PurchasingImportEntity.class,
                        new PurchasingImportEntity.Key(
                                Objects.requireNonNull(companyId, "companyId").value(),
                                Objects.requireNonNull(sourceSystem, "sourceSystem"),
                                Objects.requireNonNull(sourceRecordKey, "sourceRecordKey"))))
                .map(PurchasingImportEntity::record);
    }

    @Override
    public void append(PurchasingImportRecord record) {
        try {
            entityManager.persist(PurchasingImportEntity.from(
                    Objects.requireNonNull(record, "record")));
            entityManager.flush();
        } catch (PersistenceException failure) {
            throw PostgreSqlPurchasingConflictMapper.map(failure);
        }
    }
}
