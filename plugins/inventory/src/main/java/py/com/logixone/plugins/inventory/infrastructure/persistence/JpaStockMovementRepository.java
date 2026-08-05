package py.com.logixone.plugins.inventory.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.inventory.api.StockMovementRequest;
import py.com.logixone.plugins.inventory.api.StockSourceReference;
import py.com.logixone.plugins.inventory.application.port.StockMovementRepository;
import py.com.logixone.plugins.inventory.domain.StockMovementLineSnapshot;
import py.com.logixone.plugins.inventory.domain.StockMovementSnapshot;

@ApplicationScoped
@Transactional
public class JpaStockMovementRepository implements StockMovementRepository {
    @PersistenceContext(unitName = InventoryPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaStockMovementRepository() {
    }

    JpaStockMovementRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public Optional<StockMovementSnapshot> findById(CompanyId companyId, StockMovementId movementId) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(movementId, "movementId");
        StockMovementEntity root = entityManager.find(
                StockMovementEntity.class,
                new StockMovementEntity.Key(companyId.value(), movementId.value()));
        return Optional.ofNullable(root).map(this::snapshot);
    }

    @Override
    public Optional<StockMovementSnapshot> findByIdempotencyKey(
            CompanyId companyId, String sourceType, String idempotencyKey) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        return entityManager.createQuery(
                        "SELECT movement FROM StockMovementEntity movement "
                                + "WHERE movement.companyId = :company "
                                + "AND movement.sourceType = :sourceType "
                                + "AND movement.idempotencyKey = :idempotencyKey",
                        StockMovementEntity.class)
                .setParameter("company", companyId.value())
                .setParameter("sourceType", sourceType)
                .setParameter("idempotencyKey", idempotencyKey)
                .getResultStream().findFirst().map(this::snapshot);
    }

    @Override
    public StockMovementSnapshot append(StockMovementSnapshot movement) {
        Objects.requireNonNull(movement, "movement");
        try {
            entityManager.persist(StockMovementEntity.from(movement));
            movement.lines().forEach(line -> entityManager.persist(StockMovementLineEntity.from(
                    movement.companyId().value(), movement.id().value(), line)));
            entityManager.flush();
            return movement;
        } catch (PersistenceException failure) {
            throw PostgreSqlInventoryConflictMapper.map(failure);
        }
    }

    private StockMovementSnapshot snapshot(StockMovementEntity root) {
        List<StockMovementLineSnapshot> lines = entityManager.createQuery(
                        "SELECT line FROM StockMovementLineEntity line "
                                + "WHERE line.companyId = :company AND line.stockMovementId = :movement "
                                + "ORDER BY line.lineNumber",
                        StockMovementLineEntity.class)
                .setParameter("company", root.companyId())
                .setParameter("movement", root.stockMovementId())
                .getResultList().stream().map(StockMovementLineEntity::snapshot).toList();
        StockMovementRequest request = new StockMovementRequest(
                root.type(), root.reasonCode(), new StockSourceReference(root.sourceType(), root.sourceId()),
                root.idempotencyKey(), lines.stream().map(StockMovementLineSnapshot::line).toList(),
                Optional.ofNullable(root.reversalOfMovementId()).map(StockMovementId::new));
        return new StockMovementSnapshot(
                new CompanyId(root.companyId()), new StockMovementId(root.stockMovementId()),
                request, root.postedAt(), lines);
    }
}
