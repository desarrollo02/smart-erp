package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptState;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderState;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestState;
import py.com.logixone.plugins.purchasing.api.SupplierReturnId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnState;
import py.com.logixone.plugins.purchasing.application.port.PurchasingDirectoryRepository;
import py.com.logixone.plugins.purchasing.application.query.PurchasingDirectoryQueries;

@ApplicationScoped
@Transactional(TxType.SUPPORTS)
public class JpaPurchasingDirectoryRepository implements PurchasingDirectoryRepository {
    @PersistenceContext(unitName = PurchasingPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaPurchasingDirectoryRepository() {
    }

    JpaPurchasingDirectoryRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public PurchasingDirectoryQueries.Page<PurchasingDirectoryQueries.RequestSummary> requests(
            CompanyId companyId, PurchasingDirectoryQueries.RequestCriteria criteria) {
        String predicate = predicate("request", criteria.text(), criteria.state(),
                "LOWER(request.number) LIKE :pattern OR EXISTS ("
                        + "SELECT line.purchaseRequestLineId "
                        + "FROM PurchaseRequestLineEntity line "
                        + "WHERE line.companyId = request.companyId "
                        + "AND line.purchaseRequestId = request.purchaseRequestId "
                        + "AND LOWER(line.item.description) LIKE :pattern)");
        var query = entityManager.createQuery(
                "SELECT request.purchaseRequestId, request.number, request.requestedOn, "
                        + "request.state, request.version FROM PurchaseRequestEntity request WHERE "
                        + predicate + " ORDER BY request.requestedOn DESC, request.number",
                Object[].class);
        var count = entityManager.createQuery(
                "SELECT COUNT(request) FROM PurchaseRequestEntity request WHERE " + predicate,
                Long.class);
        bind(query, companyId, criteria.text(), criteria.state());
        bind(count, companyId, criteria.text(), criteria.state());
        List<PurchasingDirectoryQueries.RequestSummary> items = page(query, criteria.offset(),
                criteria.limit()).stream().map(row -> new PurchasingDirectoryQueries.RequestSummary(
                        new PurchaseRequestId((UUID) row[0]), (String) row[1],
                        (java.time.LocalDate) row[2], (PurchaseRequestState) row[3],
                        lineCount("PurchaseRequestLineEntity", "purchaseRequestId", companyId,
                                (UUID) row[0]), ((Number) row[4]).longValue())).toList();
        return new PurchasingDirectoryQueries.Page<>(items, count.getSingleResult(),
                criteria.offset(), criteria.limit());
    }

    @Override
    public PurchasingDirectoryQueries.Page<PurchasingDirectoryQueries.OrderSummary> orders(
            CompanyId companyId, PurchasingDirectoryQueries.OrderCriteria criteria) {
        String predicate = predicate("purchaseOrder", criteria.text(), criteria.state(),
                "LOWER(purchaseOrder.number) LIKE :pattern "
                        + "OR LOWER(purchaseOrder.supplier.code) LIKE :pattern "
                        + "OR LOWER(purchaseOrder.supplier.displayName) LIKE :pattern");
        var query = entityManager.createQuery(
                "SELECT purchaseOrder.purchaseOrderId, purchaseOrder.number, "
                        + "purchaseOrder.supplier.displayName, purchaseOrder.currency.code, "
                        + "purchaseOrder.state, purchaseOrder.version "
                        + "FROM PurchaseOrderEntity purchaseOrder WHERE " + predicate
                        + " ORDER BY purchaseOrder.purchaseOrderId DESC",
                Object[].class);
        var count = entityManager.createQuery(
                "SELECT COUNT(purchaseOrder) FROM PurchaseOrderEntity purchaseOrder WHERE "
                        + predicate, Long.class);
        bind(query, companyId, criteria.text(), criteria.state());
        bind(count, companyId, criteria.text(), criteria.state());
        List<PurchasingDirectoryQueries.OrderSummary> items = page(query, criteria.offset(),
                criteria.limit()).stream().map(row -> new PurchasingDirectoryQueries.OrderSummary(
                        new PurchaseOrderId((UUID) row[0]), (String) row[1], (String) row[2],
                        (String) row[3], (PurchaseOrderState) row[4],
                        lineCount("PurchaseOrderLineEntity", "purchaseOrderId", companyId,
                                (UUID) row[0]), ((Number) row[5]).longValue())).toList();
        return new PurchasingDirectoryQueries.Page<>(items, count.getSingleResult(),
                criteria.offset(), criteria.limit());
    }

    @Override
    public PurchasingDirectoryQueries.Page<PurchasingDirectoryQueries.ReceiptSummary> receipts(
            CompanyId companyId, PurchasingDirectoryQueries.ReceiptCriteria criteria) {
        String predicate = predicate("receipt", criteria.text(), criteria.state(),
                "LOWER(receipt.number) LIKE :pattern OR EXISTS ("
                        + "SELECT purchaseOrder.purchaseOrderId "
                        + "FROM PurchaseOrderEntity purchaseOrder "
                        + "WHERE purchaseOrder.companyId = receipt.companyId "
                        + "AND purchaseOrder.purchaseOrderId = receipt.purchaseOrderId "
                        + "AND LOWER(purchaseOrder.number) LIKE :pattern)");
        var query = entityManager.createQuery(
                "SELECT receipt.goodsReceiptId, receipt.number, receipt.purchaseOrderId, "
                        + "receipt.state, receipt.version FROM GoodsReceiptEntity receipt WHERE "
                        + predicate + " ORDER BY receipt.goodsReceiptId DESC", Object[].class);
        var count = entityManager.createQuery(
                "SELECT COUNT(receipt) FROM GoodsReceiptEntity receipt WHERE " + predicate,
                Long.class);
        bind(query, companyId, criteria.text(), criteria.state());
        bind(count, companyId, criteria.text(), criteria.state());
        List<PurchasingDirectoryQueries.ReceiptSummary> items = page(query, criteria.offset(),
                criteria.limit()).stream().map(row -> new PurchasingDirectoryQueries.ReceiptSummary(
                        new GoodsReceiptId((UUID) row[0]), (String) row[1],
                        new PurchaseOrderId((UUID) row[2]), (GoodsReceiptState) row[3],
                        lineCount("GoodsReceiptLineEntity", "goodsReceiptId", companyId,
                                (UUID) row[0]), ((Number) row[4]).longValue())).toList();
        return new PurchasingDirectoryQueries.Page<>(items, count.getSingleResult(),
                criteria.offset(), criteria.limit());
    }

    @Override
    public PurchasingDirectoryQueries.Page<PurchasingDirectoryQueries.ReturnSummary> returns(
            CompanyId companyId, PurchasingDirectoryQueries.ReturnCriteria criteria) {
        String predicate = predicate("supplierReturn", criteria.text(), criteria.state(),
                "LOWER(supplierReturn.number) LIKE :pattern "
                        + "OR LOWER(supplierReturn.reason) LIKE :pattern OR EXISTS ("
                        + "SELECT purchaseOrder.purchaseOrderId "
                        + "FROM PurchaseOrderEntity purchaseOrder "
                        + "WHERE purchaseOrder.companyId = supplierReturn.companyId "
                        + "AND purchaseOrder.purchaseOrderId = supplierReturn.purchaseOrderId "
                        + "AND LOWER(purchaseOrder.number) LIKE :pattern)");
        var query = entityManager.createQuery(
                "SELECT supplierReturn.supplierReturnId, supplierReturn.number, "
                        + "supplierReturn.purchaseOrderId, supplierReturn.reason, "
                        + "supplierReturn.state, supplierReturn.version "
                        + "FROM SupplierReturnEntity supplierReturn WHERE " + predicate
                        + " ORDER BY supplierReturn.supplierReturnId DESC", Object[].class);
        var count = entityManager.createQuery(
                "SELECT COUNT(supplierReturn) FROM SupplierReturnEntity supplierReturn WHERE "
                        + predicate, Long.class);
        bind(query, companyId, criteria.text(), criteria.state());
        bind(count, companyId, criteria.text(), criteria.state());
        List<PurchasingDirectoryQueries.ReturnSummary> items = page(query, criteria.offset(),
                criteria.limit()).stream().map(row -> new PurchasingDirectoryQueries.ReturnSummary(
                        new SupplierReturnId((UUID) row[0]), (String) row[1],
                        new PurchaseOrderId((UUID) row[2]), (String) row[3],
                        (SupplierReturnState) row[4],
                        lineCount("SupplierReturnLineEntity", "supplierReturnId", companyId,
                                (UUID) row[0]), ((Number) row[5]).longValue())).toList();
        return new PurchasingDirectoryQueries.Page<>(items, count.getSingleResult(),
                criteria.offset(), criteria.limit());
    }

    private long lineCount(
            String entity, String ownerField, CompanyId companyId, UUID ownerId) {
        return entityManager.createQuery(
                        "SELECT COUNT(line) FROM " + entity + " line WHERE "
                                + "line.companyId = :company AND line." + ownerField + " = :owner",
                        Long.class)
                .setParameter("company", companyId.value())
                .setParameter("owner", ownerId)
                .getSingleResult();
    }

    private static String predicate(
            String alias, Optional<String> text, Optional<?> state,
            String textPredicate) {
        StringBuilder value = new StringBuilder(alias + ".companyId = :company");
        if (text.isPresent()) {
            value.append(" AND (").append(textPredicate).append(')');
        }
        if (state.isPresent()) {
            value.append(" AND ").append(alias).append(".state = :state");
        }
        return value.toString();
    }

    private static void bind(
            Query query, CompanyId companyId, Optional<String> text, Optional<?> state) {
        query.setParameter("company", Objects.requireNonNull(companyId, "companyId").value());
        text.ifPresent(value -> query.setParameter(
                "pattern", "%" + value.toLowerCase(Locale.ROOT) + "%"));
        state.ifPresent(value -> query.setParameter("state", value));
    }

    private static List<Object[]> page(
            jakarta.persistence.TypedQuery<Object[]> query, int offset, int limit) {
        return query.setFirstResult(offset).setMaxResults(limit).getResultList();
    }
}
