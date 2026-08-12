package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptLineId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptState;
import py.com.logixone.plugins.purchasing.api.PurchaseLineKind;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderLineId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderState;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestLineId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestState;
import py.com.logixone.plugins.purchasing.api.SupplierReturnId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnLineId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnState;
import py.com.logixone.plugins.purchasing.domain.CurrencySnapshot;
import py.com.logixone.plugins.purchasing.domain.GoodsReceipt;
import py.com.logixone.plugins.purchasing.domain.PurchaseOrder;
import py.com.logixone.plugins.purchasing.domain.PurchaseRequest;
import py.com.logixone.plugins.purchasing.domain.PurchasedItemSnapshot;
import py.com.logixone.plugins.purchasing.domain.SupplierReturn;
import py.com.logixone.plugins.purchasing.domain.SupplierSnapshot;
import py.com.logixone.plugins.referencedata.api.CurrencyCode;

@Testcontainers
class PurchasingJpaValidationPostgreSqlIT {
    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse(
                    "postgres:18.4-bookworm@sha256:16fa100a3a6e92c0556632870455e7f8c6f3df5cefddd67d6b95292732bd7ff0")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("logixone_purchasing_jpa_validation")
            .withUsername("logixone_test")
            .withPassword("test-only-password");

    private static EntityManagerFactory entityManagerFactory;

    @BeforeAll
    static void migrateAndValidate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(PurchasingPersistenceNames.SCHEMA)
                .defaultSchema(PurchasingPersistenceNames.SCHEMA)
                .locations("classpath:db/migration/purchasing")
                .createSchemas(true)
                .cleanDisabled(true)
                .load().migrate();
        entityManagerFactory = Persistence.createEntityManagerFactory(
                PurchasingPersistenceNames.TEST_UNIT_NAME,
                Map.of(
                        "jakarta.persistence.jdbc.url", POSTGRES.getJdbcUrl(),
                        "jakarta.persistence.jdbc.user", POSTGRES.getUsername(),
                        "jakarta.persistence.jdbc.password", POSTGRES.getPassword(),
                        "jakarta.persistence.jdbc.driver", "org.postgresql.Driver"));
    }

    @AfterAll
    static void closeFactory() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @Test
    void validatesAllMappedTablesWithoutCreatingOrUpdatingDdl() {
        assertDoesNotThrow(() -> {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            entityManager.close();
        });
    }

    @Test
    void persistsAndRebuildsTheFourCompanyScopedAggregates() {
        Fixture fixture = new Fixture();
        PurchaseRequest request = fixture.draftRequest();
        PurchaseOrder order = fixture.draftOrder();
        GoodsReceipt receipt = fixture.draftReceipt();

        inTransaction(entityManager -> {
            new JpaPurchaseRequestRepository(entityManager).insert(request);
            return null;
        });
        inTransaction(entityManager -> {
            request.submit(
                    fixture.requester, Instant.parse("2026-08-11T12:00:00Z"), request.version());
            new JpaPurchaseRequestRepository(entityManager).update(request, 0);
            return null;
        });
        inTransaction(entityManager -> {
            request.approve(
                    fixture.approver, Instant.parse("2026-08-11T13:00:00Z"), request.version());
            new JpaPurchaseRequestRepository(entityManager).update(request, 1);
            return null;
        });
        inTransaction(entityManager -> {
            new JpaPurchaseOrderRepository(entityManager).insert(order);
            return null;
        });
        inTransaction(entityManager -> {
            order.issue(
                    fixture.approver, Instant.parse("2026-08-11T13:30:00Z"), order.version());
            new JpaPurchaseOrderRepository(entityManager).update(order, 0);
            new JpaGoodsReceiptRepository(entityManager).insert(receipt);
            return null;
        });

        inTransaction(entityManager -> {
            JpaPurchaseOrderRepository orders = new JpaPurchaseOrderRepository(entityManager);
            PurchaseOrder storedOrder = orders.findById(fixture.companyId, fixture.orderId).orElseThrow();
            long orderVersion = storedOrder.version();
            storedOrder.applyReceipt(Map.of(fixture.orderLineId, new BigDecimal("2")), orderVersion);
            orders.update(storedOrder, orderVersion);

            JpaGoodsReceiptRepository receipts = new JpaGoodsReceiptRepository(entityManager);
            GoodsReceipt storedReceipt = receipts.findById(
                    fixture.companyId, fixture.receiptId).orElseThrow();
            long receiptVersion = storedReceipt.version();
            storedReceipt.confirm(
                    fixture.approver, Instant.parse("2026-08-11T14:00:00Z"),
                    Map.of(fixture.receiptLineId, fixture.receiptMovementId), receiptVersion);
            receipts.update(storedReceipt, receiptVersion);
            return null;
        });

        SupplierReturn supplierReturn = fixture.draftReturn();
        inTransaction(entityManager -> {
            new JpaSupplierReturnRepository(entityManager).insert(supplierReturn);
            return null;
        });
        inTransaction(entityManager -> {
            JpaPurchaseOrderRepository orders = new JpaPurchaseOrderRepository(entityManager);
            PurchaseOrder storedOrder = orders.findById(fixture.companyId, fixture.orderId).orElseThrow();
            long orderVersion = storedOrder.version();
            storedOrder.applyReturn(Map.of(fixture.orderLineId, BigDecimal.ONE), orderVersion);
            orders.update(storedOrder, orderVersion);

            JpaSupplierReturnRepository returns = new JpaSupplierReturnRepository(entityManager);
            SupplierReturn storedReturn = returns.findById(
                    fixture.companyId, fixture.returnId).orElseThrow();
            long returnVersion = storedReturn.version();
            storedReturn.confirm(
                    fixture.approver, Instant.parse("2026-08-11T15:00:00Z"),
                    Map.of(fixture.returnLineId, fixture.returnMovementId), returnVersion);
            returns.update(storedReturn, returnVersion);
            return null;
        });

        inTransaction(entityManager -> {
            assertEquals(request.snapshot(), new JpaPurchaseRequestRepository(entityManager)
                    .findById(fixture.companyId, fixture.requestId).orElseThrow().snapshot());
            assertEquals(PurchaseRequestState.APPROVED, request.state());
            assertEquals(PurchaseOrderState.ISSUED, new JpaPurchaseOrderRepository(entityManager)
                    .findById(fixture.companyId, fixture.orderId).orElseThrow().state());
            assertEquals(GoodsReceiptState.CONFIRMED, new JpaGoodsReceiptRepository(entityManager)
                    .findById(fixture.companyId, fixture.receiptId).orElseThrow().state());
            assertEquals(SupplierReturnState.CONFIRMED, new JpaSupplierReturnRepository(entityManager)
                    .findById(fixture.companyId, fixture.returnId).orElseThrow().state());
            return null;
        });

        CompanyId otherCompany = new CompanyId(UUID.randomUUID());
        boolean visibleFromAnotherCompany = inTransaction(entityManager ->
                new JpaPurchaseOrderRepository(entityManager)
                        .findById(otherCompany, fixture.orderId).isPresent());
        assertFalse(visibleFromAnotherCompany);
    }

    private static <T> T inTransaction(Function<EntityManager, T> operation) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            T result = operation.apply(entityManager);
            entityManager.getTransaction().commit();
            return result;
        } catch (RuntimeException failure) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw failure;
        } finally {
            entityManager.close();
        }
    }

    private static final class Fixture {
        private final CompanyId companyId = new CompanyId(UUID.randomUUID());
        private final AppUserId requester = new AppUserId(UUID.randomUUID());
        private final AppUserId approver = new AppUserId(UUID.randomUUID());
        private final PurchaseRequestId requestId = new PurchaseRequestId(UUID.randomUUID());
        private final PurchaseRequestLineId requestLineId = new PurchaseRequestLineId(UUID.randomUUID());
        private final PurchaseOrderId orderId = new PurchaseOrderId(UUID.randomUUID());
        private final PurchaseOrderLineId orderLineId = new PurchaseOrderLineId(UUID.randomUUID());
        private final GoodsReceiptId receiptId = new GoodsReceiptId(UUID.randomUUID());
        private final GoodsReceiptLineId receiptLineId = new GoodsReceiptLineId(UUID.randomUUID());
        private final SupplierReturnId returnId = new SupplierReturnId(UUID.randomUUID());
        private final SupplierReturnLineId returnLineId = new SupplierReturnLineId(UUID.randomUUID());
        private final WarehouseId warehouseId = new WarehouseId(UUID.randomUUID());
        private final StockLocationId locationId = new StockLocationId(UUID.randomUUID());
        private final StockMovementId receiptMovementId = new StockMovementId(UUID.randomUUID());
        private final StockMovementId returnMovementId = new StockMovementId(UUID.randomUUID());
        private final PurchasedItemSnapshot item = new PurchasedItemSnapshot(
                Optional.of(new CatalogItemId(UUID.randomUUID())), Optional.of("ITEM-1"),
                "Test item", "UN", PurchaseLineKind.STOCK, 3);
        private final CurrencySnapshot currency = new CurrencySnapshot(
                new CurrencyCode("PYG"), 0, "Guarani", "ISO-2026");

        private PurchaseRequest draftRequest() {
            return PurchaseRequest.draft(
                    companyId, requestId, "SC-1", requester, LocalDate.of(2026, 8, 11),
                    List.of(new PurchaseRequest.Line(
                            requestLineId, item, new BigDecimal("5"),
                            Optional.of(new PurchaseRequest.ExpectedPrice(
                                    new BigDecimal("100"), currency)))));
        }

        private PurchaseOrder draftOrder() {
            return PurchaseOrder.draft(
                    companyId, orderId, "OC-1",
                    new SupplierSnapshot(
                            new BusinessPartnerId(UUID.randomUUID()), "SUP-1", "Supplier", 2),
                    currency,
                    List.of(new PurchaseOrder.LineDraft(
                            orderLineId, item, new BigDecimal("5"), new BigDecimal("100"),
                            List.of(new PurchaseOrder.Allocation(
                                    requestId, requestLineId, new BigDecimal("5"))))),
                    Optional.empty());
        }

        private GoodsReceipt draftReceipt() {
            return GoodsReceipt.draft(
                    companyId, receiptId, "RC-1", orderId,
                    List.of(new GoodsReceipt.Line(
                            receiptLineId, orderLineId, PurchaseLineKind.STOCK,
                            new BigDecimal("2"), Optional.of(warehouseId), Optional.of(locationId))));
        }

        private SupplierReturn draftReturn() {
            return SupplierReturn.draft(
                    companyId, returnId, "DP-1", orderId, "Damaged",
                    List.of(new SupplierReturn.Line(
                            returnLineId, receiptId, receiptLineId, orderLineId,
                            PurchaseLineKind.STOCK, BigDecimal.ONE,
                            Optional.of(warehouseId), Optional.of(locationId))));
        }
    }
}
