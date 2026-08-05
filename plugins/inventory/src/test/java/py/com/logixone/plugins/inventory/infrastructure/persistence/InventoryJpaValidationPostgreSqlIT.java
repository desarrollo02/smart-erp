package py.com.logixone.plugins.inventory.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemReference;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemScope;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemType;
import py.com.logixone.plugins.inventory.api.ExpiryPolicy;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.MovementQuantity;
import py.com.logixone.plugins.inventory.api.StockCondition;
import py.com.logixone.plugins.inventory.api.StockCountId;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.StockMovementDirection;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.inventory.api.StockMovementLine;
import py.com.logixone.plugins.inventory.api.StockMovementRequest;
import py.com.logixone.plugins.inventory.api.StockMovementType;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.inventory.api.StockReservationRequest;
import py.com.logixone.plugins.inventory.api.StockSourceReference;
import py.com.logixone.plugins.inventory.api.TrackingMode;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.application.port.InventoryPersistenceCode;
import py.com.logixone.plugins.inventory.application.port.InventoryPersistenceException;
import py.com.logixone.plugins.inventory.application.query.InventoryDirectoryQueries;
import py.com.logixone.plugins.inventory.domain.InventoryBalance;
import py.com.logixone.plugins.inventory.domain.InventoryItem;
import py.com.logixone.plugins.inventory.domain.ReservationOperation;
import py.com.logixone.plugins.inventory.domain.ReservationOperationType;
import py.com.logixone.plugins.inventory.domain.StockCount;
import py.com.logixone.plugins.inventory.domain.StockCountScope;
import py.com.logixone.plugins.inventory.domain.StockCountState;
import py.com.logixone.plugins.inventory.domain.StockLocationType;
import py.com.logixone.plugins.inventory.domain.StockMovement;
import py.com.logixone.plugins.inventory.domain.StockMovementSnapshot;
import py.com.logixone.plugins.inventory.domain.StockReservation;
import py.com.logixone.plugins.inventory.domain.Warehouse;

@Testcontainers
class InventoryJpaValidationPostgreSqlIT {
    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse(
                    "postgres:18.4-bookworm@sha256:16fa100a3a6e92c0556632870455e7f8c6f3df5cefddd67d6b95292732bd7ff0")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("logixone_inventory_jpa_validation")
            .withUsername("logixone_test")
            .withPassword("test-only-password");

    private static EntityManagerFactory entityManagerFactory;

    @BeforeAll
    static void migrateAndValidate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(InventoryPersistenceNames.SCHEMA)
                .defaultSchema(InventoryPersistenceNames.SCHEMA)
                .locations("classpath:db/migration/inventory")
                .createSchemas(true)
                .cleanDisabled(true)
                .load()
                .migrate();
        entityManagerFactory = Persistence.createEntityManagerFactory(
                InventoryPersistenceNames.TEST_UNIT_NAME,
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
    void persistsAndRebuildsEveryAggregateWithinItsCompanyBoundary() {
        Fixture fixture = new Fixture();
        Warehouse warehouse = fixture.warehouse();
        InventoryItem item = fixture.item();
        InventoryBalance balance = fixture.balance();
        StockMovementSnapshot movement = fixture.movement(item);
        StockReservation reservation = fixture.reservation();
        reservation.release(BigDecimal.ONE, reservation.version());
        ReservationOperation reservationOperation = ReservationOperation.capture(
                reservation, ReservationOperationType.RELEASE, BigDecimal.ONE,
                "release-so-1-line-1", Instant.parse("2026-07-31T13:00:00Z"));
        StockCount count = fixture.count();

        inTransaction(entityManager -> {
            new JpaWarehouseRepository(entityManager).insert(warehouse);
            new JpaInventoryItemRepository(entityManager).insert(item);
            new JpaInventoryBalanceRepository(entityManager).insert(balance);
            new JpaStockMovementRepository(entityManager).append(movement);
            new JpaStockReservationRepository(entityManager).insert(reservation);
            new JpaReservationOperationRepository(entityManager).append(reservationOperation);
            new JpaStockCountRepository(entityManager).insert(count);
            return null;
        });

        inTransaction(entityManager -> {
            assertEquals(warehouse.snapshot(), new JpaWarehouseRepository(entityManager)
                    .findById(fixture.companyId, fixture.warehouseId).orElseThrow().snapshot());
            assertEquals(item.snapshot(), new JpaInventoryItemRepository(entityManager)
                    .findById(fixture.companyId, fixture.itemId).orElseThrow().snapshot());
            assertEquals(balance.snapshot(), new JpaInventoryBalanceRepository(entityManager)
                    .find(fixture.companyId, fixture.key).orElseThrow().snapshot());
            assertEquals(movement, new JpaStockMovementRepository(entityManager)
                    .findById(fixture.companyId, fixture.movementId).orElseThrow());
            assertEquals(reservation.snapshot(), new JpaStockReservationRepository(entityManager)
                    .findById(fixture.companyId, fixture.reservationId).orElseThrow().snapshot());
            assertEquals(reservationOperation, new JpaReservationOperationRepository(entityManager)
                    .findByIdempotencyKey(fixture.companyId, "RELEASE-SO-1-LINE-1")
                    .orElseThrow());
            assertEquals(count.snapshot(), new JpaStockCountRepository(entityManager)
                    .findById(fixture.companyId, fixture.countId).orElseThrow().snapshot());
            return null;
        });

        boolean visibleFromAnotherCompany = inTransaction(entityManager ->
                new JpaWarehouseRepository(entityManager)
                        .findById(new CompanyId(UUID.randomUUID()), fixture.warehouseId).isPresent());
        assertFalse(visibleFromAnotherCompany);
    }

    @Test
    void readsCompanyScopedWarehouseItemAndCountDirectoriesFromPostgreSql() {
        Fixture fixture = new Fixture();
        inTransaction(entityManager -> {
            new JpaWarehouseRepository(entityManager).insert(fixture.warehouse());
            new JpaInventoryItemRepository(entityManager).insert(fixture.item());
            new JpaInventoryBalanceRepository(entityManager).insert(fixture.balance());
            new JpaStockCountRepository(entityManager).insert(fixture.count());
            return null;
        });

        inTransaction(entityManager -> {
            JpaInventoryDirectoryRepository directory =
                    new JpaInventoryDirectoryRepository(entityManager);

            var warehouses = directory.warehouses(
                    fixture.companyId,
                    new InventoryDirectoryQueries.Criteria(
                            Optional.of("main"), Optional.of(true), 0, 20));
            assertEquals(1, warehouses.total());
            assertEquals(2, warehouses.items().getFirst().locations().size());

            var items = directory.items(
                    fixture.companyId,
                    new InventoryDirectoryQueries.Criteria(
                            Optional.of("product"), Optional.of(true), 0, 20));
            assertEquals(1, items.total());
            assertEquals(0, new BigDecimal("5").compareTo(
                    items.items().getFirst().physicalQuantity()));
            assertEquals(0, BigDecimal.ZERO.compareTo(
                    items.items().getFirst().reservedQuantity()));
            assertEquals(items.items().getFirst(), directory.item(
                    fixture.companyId, fixture.itemId).orElseThrow());

            var counts = directory.counts(
                    fixture.companyId,
                    new InventoryDirectoryQueries.CountCriteria(
                            Optional.of(StockCountState.DRAFT), 0, 20));
            assertEquals(1, counts.total());
            assertEquals(1, counts.items().getFirst().lineCount());

            CompanyId otherCompany = new CompanyId(UUID.randomUUID());
            assertEquals(0, directory.warehouses(
                    otherCompany,
                    new InventoryDirectoryQueries.Criteria(
                            Optional.empty(), Optional.empty(), 0, 20)).total());
            assertEquals(0, directory.items(
                    otherCompany,
                    new InventoryDirectoryQueries.Criteria(
                            Optional.empty(), Optional.empty(), 0, 20)).total());
            assertEquals(0, directory.counts(
                    otherCompany,
                    new InventoryDirectoryQueries.CountCriteria(Optional.empty(), 0, 20)).total());
            return null;
        });
    }

    @Test
    void persistsOptimisticUpdatesForMutableRoots() {
        Fixture fixture = new Fixture();
        inTransaction(entityManager -> {
            new JpaWarehouseRepository(entityManager).insert(fixture.warehouse());
            new JpaInventoryItemRepository(entityManager).insert(fixture.item());
            new JpaInventoryBalanceRepository(entityManager).insert(fixture.balance());
            new JpaStockReservationRepository(entityManager).insert(fixture.reservation());
            new JpaStockCountRepository(entityManager).insert(fixture.count());
            return null;
        });

        inTransaction(entityManager -> {
            Warehouse warehouse = new JpaWarehouseRepository(entityManager)
                    .findById(fixture.companyId, fixture.warehouseId).orElseThrow();
            long warehouseVersion = warehouse.version();
            warehouse.rename("Updated warehouse", warehouseVersion);
            new JpaWarehouseRepository(entityManager).update(warehouse, warehouseVersion);

            InventoryBalance balance = new JpaInventoryBalanceRepository(entityManager)
                    .find(fixture.companyId, fixture.key).orElseThrow();
            long balanceVersion = balance.version();
            balance.receive(BigDecimal.ONE, balanceVersion);
            new JpaInventoryBalanceRepository(entityManager).update(balance, balanceVersion);

            StockReservation reservation = new JpaStockReservationRepository(entityManager)
                    .findById(fixture.companyId, fixture.reservationId).orElseThrow();
            long reservationVersion = reservation.version();
            reservation.consume(BigDecimal.ONE, reservationVersion);
            new JpaStockReservationRepository(entityManager).update(reservation, reservationVersion);

            StockCount count = new JpaStockCountRepository(entityManager)
                    .findById(fixture.companyId, fixture.countId).orElseThrow();
            long countVersion = count.version();
            count.start(countVersion);
            new JpaStockCountRepository(entityManager).update(count, countVersion);
            return null;
        });

        inTransaction(entityManager -> {
            assertEquals("Updated warehouse", new JpaWarehouseRepository(entityManager)
                    .findById(fixture.companyId, fixture.warehouseId).orElseThrow().name());
            assertEquals(0, new BigDecimal("6").compareTo(new JpaInventoryBalanceRepository(entityManager)
                    .find(fixture.companyId, fixture.key).orElseThrow().physicalQuantity()));
            assertEquals(0, BigDecimal.ONE.compareTo(new JpaStockReservationRepository(entityManager)
                    .findById(fixture.companyId, fixture.reservationId).orElseThrow().consumedQuantity()));
            assertEquals(py.com.logixone.plugins.inventory.domain.StockCountState.COUNTING,
                    new JpaStockCountRepository(entityManager)
                            .findById(fixture.companyId, fixture.countId).orElseThrow().state());
            return null;
        });
    }

    @Test
    void evaluatesPhysicalCountLocksWithStrictJpaCompliance() {
        Fixture fixture = new Fixture();
        inTransaction(entityManager -> {
            new JpaWarehouseRepository(entityManager).insert(fixture.warehouse());
            new JpaInventoryItemRepository(entityManager).insert(fixture.item());
            new JpaStockCountRepository(entityManager).insert(fixture.count());
            return null;
        });

        inTransaction(entityManager -> {
            JpaStockCountRepository counts = new JpaStockCountRepository(entityManager);
            assertFalse(counts.blocks(fixture.companyId, fixture.key));
            StockCount count = counts.findById(fixture.companyId, fixture.countId).orElseThrow();
            long previousVersion = count.version();
            count.start(previousVersion);
            counts.update(count, previousVersion);
            return null;
        });

        boolean blocked = inTransaction(entityManager -> new JpaStockCountRepository(entityManager)
                .blocks(fixture.companyId, fixture.key));
        assertTrue(blocked);
    }

    @Test
    void rejectsAStaleBalanceWriterWithAStableApplicationCode() {
        Fixture fixture = new Fixture();
        inTransaction(entityManager -> {
            new JpaWarehouseRepository(entityManager).insert(fixture.warehouse());
            new JpaInventoryItemRepository(entityManager).insert(fixture.item());
            new JpaInventoryBalanceRepository(entityManager).insert(fixture.balance());
            return null;
        });

        EntityManager firstManager = entityManagerFactory.createEntityManager();
        EntityManager staleManager = entityManagerFactory.createEntityManager();
        try {
            firstManager.getTransaction().begin();
            staleManager.getTransaction().begin();
            JpaInventoryBalanceRepository firstRepository = new JpaInventoryBalanceRepository(firstManager);
            JpaInventoryBalanceRepository staleRepository = new JpaInventoryBalanceRepository(staleManager);
            InventoryBalance first = firstRepository.find(fixture.companyId, fixture.key).orElseThrow();
            InventoryBalance stale = staleRepository.find(fixture.companyId, fixture.key).orElseThrow();
            long persistedVersion = first.version();
            first.receive(BigDecimal.ONE, persistedVersion);
            stale.receive(new BigDecimal("2"), persistedVersion);

            firstRepository.update(first, persistedVersion);
            firstManager.getTransaction().commit();

            InventoryPersistenceException failure = assertThrows(
                    InventoryPersistenceException.class,
                    () -> staleRepository.update(stale, persistedVersion));
            assertEquals(InventoryPersistenceCode.VERSION_CONFLICT, failure.code());
            staleManager.getTransaction().rollback();
        } finally {
            rollbackIfActive(firstManager);
            rollbackIfActive(staleManager);
            firstManager.close();
            staleManager.close();
        }
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

    private static void rollbackIfActive(EntityManager entityManager) {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
    }

    private static final class Fixture {
        private final CompanyId companyId = new CompanyId(UUID.randomUUID());
        private final WarehouseId warehouseId = new WarehouseId(UUID.randomUUID());
        private final StockLocationId locationId = new StockLocationId(UUID.randomUUID());
        private final InventoryItemId itemId = new InventoryItemId(UUID.randomUUID());
        private final StockMovementId movementId = new StockMovementId(UUID.randomUUID());
        private final StockReservationId reservationId = new StockReservationId(UUID.randomUUID());
        private final StockCountId countId = new StockCountId(UUID.randomUUID());
        private final StockKey key = new StockKey(
                itemId, warehouseId, locationId, Optional.of("LOT-A"), Optional.empty(),
                Optional.empty(), StockCondition.AVAILABLE);

        private Warehouse warehouse() {
            Warehouse warehouse = Warehouse.open(
                    companyId, warehouseId, locationId, "MAIN", "Main warehouse");
            warehouse.addLocation(
                    new StockLocationId(UUID.randomUUID()), "PICK", "Picking",
                    StockLocationType.STORAGE, warehouse.version());
            return warehouse;
        }

        private InventoryItem item() {
            CatalogItemReference catalog = new CatalogItemReference(
                    new CatalogItemId(UUID.randomUUID()), "SKU-1", "Product",
                    CatalogItemType.PRODUCT, CatalogItemState.ACTIVE,
                    Set.of(CatalogItemScope.PURCHASE), "EA", 3);
            return InventoryItem.enroll(
                    companyId, itemId, catalog, TrackingMode.LOT, ExpiryPolicy.NONE);
        }

        private InventoryBalance balance() {
            InventoryBalance balance = InventoryBalance.empty(companyId, key, "EA");
            balance.receive(new BigDecimal("5"), balance.version());
            return balance;
        }

        private StockMovementSnapshot movement(InventoryItem item) {
            StockMovementRequest request = new StockMovementRequest(
                    StockMovementType.RECEIPT, "PURCHASE",
                    new StockSourceReference("purchase_receipt", "PR-1"), "receipt-pr-1",
                    List.of(new StockMovementLine(
                            key, StockMovementDirection.INCREASE,
                            new MovementQuantity(
                                    "EA", new BigDecimal("5"), "EA", BigDecimal.ONE,
                                    new BigDecimal("5"), 3))), Optional.empty());
            return StockMovement.post(
                    companyId, movementId, request, Instant.parse("2026-07-31T12:00:00Z"))
                    .snapshot(Map.of(itemId, item));
        }

        private StockReservation reservation() {
            return StockReservation.create(
                    companyId, reservationId,
                    new StockReservationRequest(
                            key, new BigDecimal("2"),
                            new StockSourceReference("sales_order", "SO-1"),
                            Instant.parse("2026-08-01T12:00:00Z"), "sales-so-1-line-1"),
                    Instant.parse("2026-07-31T12:00:00Z"));
        }

        private StockCount count() {
            StockCount count = StockCount.draft(
                    companyId, countId, new StockCountScope(warehouseId, Optional.of(locationId)));
            count.addLine(key, new BigDecimal("5"), count.version());
            return count;
        }
    }
}
