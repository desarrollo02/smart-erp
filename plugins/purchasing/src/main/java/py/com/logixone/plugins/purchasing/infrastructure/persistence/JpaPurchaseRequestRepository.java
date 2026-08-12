package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestState;
import py.com.logixone.plugins.purchasing.application.port.PurchaseRequestRepository;
import py.com.logixone.plugins.purchasing.application.port.PurchasingPersistenceCode;
import py.com.logixone.plugins.purchasing.application.port.PurchasingPersistenceException;
import py.com.logixone.plugins.purchasing.domain.PurchaseRequest;

@ApplicationScoped
@Transactional
public class JpaPurchaseRequestRepository implements PurchaseRequestRepository {
    @PersistenceContext(unitName = PurchasingPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaPurchaseRequestRepository() {
    }

    JpaPurchaseRequestRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public Optional<PurchaseRequest> findById(CompanyId companyId, PurchaseRequestId requestId) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(requestId, "requestId");
        PurchaseRequestEntity entity = entityManager.find(
                PurchaseRequestEntity.class,
                new PurchaseRequestEntity.Key(companyId.value(), requestId.value()));
        return Optional.ofNullable(entity).map(this::restore);
    }

    @Override
    public Optional<PurchaseRequest> findByNumber(CompanyId companyId, String number) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(number, "number");
        return entityManager.createQuery(
                        "SELECT request FROM PurchaseRequestEntity request "
                                + "WHERE request.companyId = :company AND request.number = :number",
                        PurchaseRequestEntity.class)
                .setParameter("company", companyId.value())
                .setParameter("number", number)
                .getResultStream().findFirst().map(this::restore);
    }

    @Override
    public PurchaseRequest insert(PurchaseRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.state() != PurchaseRequestState.DRAFT) {
            throw new PurchasingPersistenceException(
                    PurchasingPersistenceCode.IMMUTABLE_DOCUMENT);
        }
        PurchaseRequest.Snapshot snapshot = request.snapshot();
        try {
            entityManager.persist(PurchaseRequestEntity.from(snapshot));
            persistLines(snapshot.companyId().value(), snapshot.id().value(), snapshot.lines());
            entityManager.flush();
            return request;
        } catch (PersistenceException failure) {
            throw PostgreSqlPurchasingConflictMapper.map(failure);
        }
    }

    @Override
    public PurchaseRequest update(PurchaseRequest request, long expectedPersistedVersion) {
        Objects.requireNonNull(request, "request");
        PurchaseRequest.Snapshot snapshot = request.snapshot();
        PurchaseRequestEntity entity = requireEntity(
                snapshot.companyId().value(), snapshot.id().value(), expectedPersistedVersion,
                snapshot.version());
        try {
            List<PurchaseRequestLineEntity> stored = lines(entity.companyId(), entity.id());
            List<PurchaseRequest.Line> current = stored.stream()
                    .map(PurchaseRequestLineEntity::snapshot).toList();
            if (!current.equals(snapshot.lines())) {
                if (entity.state() != PurchaseRequestState.DRAFT
                        || snapshot.state() != PurchaseRequestState.DRAFT) {
                    throw new PurchasingPersistenceException(
                            PurchasingPersistenceCode.IMMUTABLE_DOCUMENT);
                }
                stored.forEach(entityManager::remove);
                entityManager.flush();
                persistLines(entity.companyId(), entity.id(), snapshot.lines());
            }
            entity.apply(snapshot);
            entityManager.flush();
            return request;
        } catch (RuntimeException failure) {
            throw PostgreSqlPurchasingConflictMapper.map(failure);
        }
    }

    private PurchaseRequestEntity requireEntity(
            UUID companyId, UUID requestId, long expectedVersion, long domainVersion) {
        if (expectedVersion < 0 || domainVersion != expectedVersion + 1) {
            throw new PurchasingPersistenceException(PurchasingPersistenceCode.VERSION_CONFLICT);
        }
        PurchaseRequestEntity entity = entityManager.find(
                PurchaseRequestEntity.class, new PurchaseRequestEntity.Key(companyId, requestId));
        if (entity == null) {
            throw new PurchasingPersistenceException(PurchasingPersistenceCode.NOT_FOUND);
        }
        if (entity.version() != expectedVersion) {
            throw new PurchasingPersistenceException(PurchasingPersistenceCode.VERSION_CONFLICT);
        }
        return entity;
    }

    private PurchaseRequest restore(PurchaseRequestEntity entity) {
        return PurchaseRequest.restore(entity.snapshot(lines(entity.companyId(), entity.id()).stream()
                .map(PurchaseRequestLineEntity::snapshot).toList()));
    }

    private List<PurchaseRequestLineEntity> lines(UUID companyId, UUID requestId) {
        return entityManager.createQuery(
                        "SELECT line FROM PurchaseRequestLineEntity line "
                                + "WHERE line.companyId = :company AND line.purchaseRequestId = :request "
                                + "ORDER BY line.position",
                        PurchaseRequestLineEntity.class)
                .setParameter("company", companyId)
                .setParameter("request", requestId)
                .getResultList();
    }

    private void persistLines(UUID companyId, UUID requestId, List<PurchaseRequest.Line> lines) {
        for (int index = 0; index < lines.size(); index++) {
            entityManager.persist(PurchaseRequestLineEntity.from(
                    companyId, requestId, index + 1, lines.get(index)));
        }
    }
}
