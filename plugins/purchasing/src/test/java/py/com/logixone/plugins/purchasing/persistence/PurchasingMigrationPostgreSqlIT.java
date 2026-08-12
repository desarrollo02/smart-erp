package py.com.logixone.plugins.purchasing.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class PurchasingMigrationPostgreSqlIT {
    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse(
                    "postgres:18.4-bookworm@sha256:16fa100a3a6e92c0556632870455e7f8c6f3df5cefddd67d6b95292732bd7ff0")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("logixone_purchasing_test")
            .withUsername("logixone_test")
            .withPassword("test-only-password");

    @BeforeAll
    static void migrateSchema() {
        MigrateResult first = flyway().migrate();
        MigrateResult second = flyway().migrate();
        assertEquals(2, first.migrationsExecuted);
        assertEquals(0, second.migrationsExecuted);
        flyway().validate();
    }

    @Test
    void createsElevenPrivateTablesAndOnlyPrivateForeignKeys() throws SQLException {
        assertEquals(11, queryInt("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema = 'plg_purchasing'
                  AND table_name <> 'flyway_schema_history'
                """));
        assertEquals(0, queryInt("""
                SELECT count(*)
                  FROM information_schema.referential_constraints reference
                 WHERE reference.constraint_schema = 'plg_purchasing'
                   AND reference.unique_constraint_schema <> 'plg_purchasing'
                """));
    }

    @Test
    void scopesDocumentReferencesByCompanyAndRejectsImpossibleQuantities() throws SQLException {
        Fixture first = seedOrder(UUID.randomUUID(), "A");
        Fixture second = seedOrder(UUID.randomUUID(), "B");
        Fixture anotherOrder = seedOrder(first.company(), "A2");

        assertSqlState("23503", () -> execute("""
                INSERT INTO plg_purchasing.purchase_order_allocation
                    (company_id, purchase_order_id, purchase_order_line_id,
                     purchase_request_id, purchase_request_line_id,
                     allocation_position, allocated_quantity)
                VALUES ('%s', '%s', '%s', '%s', '%s', 1, 1)
                """.formatted(
                        second.company(), second.order(), second.orderLine(),
                        first.request(), first.requestLine())));
        insertAllocation(first, first, 5);
        assertSqlState("23514", () -> insertAllocation(
                anotherOrder, first, 1));
        assertSqlState("23514", () -> execute("""
                UPDATE plg_purchasing.purchase_order_line
                   SET returned_quantity = 2
                 WHERE company_id = '%s' AND purchase_order_id = '%s'
                   AND purchase_order_line_id = '%s'
                """.formatted(first.company(), first.order(), first.orderLine())));
    }

    @Test
    void requiresInventoryMovementBeforeReceiptConfirmationAndThenFreezesIt() throws SQLException {
        Fixture fixture = seedOrder(UUID.randomUUID(), "RECEIPT");
        UUID receipt = UUID.randomUUID();
        UUID receiptLine = UUID.randomUUID();
        execute("""
                INSERT INTO plg_purchasing.goods_receipt
                    (company_id, goods_receipt_id, receipt_number, purchase_order_id,
                     receipt_state, entity_version)
                VALUES ('%s', '%s', 'RC-1', '%s', 'DRAFT', 0)
                """.formatted(fixture.company(), receipt, fixture.order()));
        execute("""
                INSERT INTO plg_purchasing.goods_receipt_line
                    (company_id, goods_receipt_id, goods_receipt_line_id, line_position,
                     purchase_order_id, purchase_order_line_id, line_kind,
                     received_quantity, warehouse_id, stock_location_id, stock_condition)
                VALUES ('%s', '%s', '%s', 1, '%s', '%s', 'STOCK', 2, '%s', '%s', 'AVAILABLE')
                """.formatted(
                        fixture.company(), receipt, receiptLine, fixture.order(), fixture.orderLine(),
                        UUID.randomUUID(), UUID.randomUUID()));

        assertSqlState("23514", () -> confirmReceipt(fixture.company(), receipt));
        execute("""
                UPDATE plg_purchasing.goods_receipt_line SET stock_movement_id = '%s'
                 WHERE company_id = '%s' AND goods_receipt_id = '%s'
                """.formatted(UUID.randomUUID(), fixture.company(), receipt));
        confirmReceipt(fixture.company(), receipt);
        assertSqlState("P2001", () -> execute("""
                UPDATE plg_purchasing.goods_receipt_line SET received_quantity = 1
                 WHERE company_id = '%s' AND goods_receipt_id = '%s'
                """.formatted(fixture.company(), receipt)));
    }

    @Test
    void requiresInventoryMovementBeforeReturnConfirmationAndKeepsCompensationImmutable()
            throws SQLException {
        Fixture fixture = seedOrder(UUID.randomUUID(), "RETURN");
        Receipt receipt = seedConfirmedReceipt(fixture);
        UUID supplierReturn = UUID.randomUUID();
        execute("""
                INSERT INTO plg_purchasing.supplier_return
                    (company_id, supplier_return_id, return_number, purchase_order_id,
                     return_reason, return_state, entity_version)
                VALUES ('%s', '%s', 'DP-1', '%s', 'Damaged', 'DRAFT', 0)
                """.formatted(fixture.company(), supplierReturn, fixture.order()));
        execute("""
                INSERT INTO plg_purchasing.supplier_return_line
                    (company_id, supplier_return_id, supplier_return_line_id, line_position,
                     purchase_order_id, purchase_order_line_id, goods_receipt_id,
                     goods_receipt_line_id, line_kind, returned_quantity,
                     warehouse_id, stock_location_id, stock_condition)
                VALUES ('%s', '%s', '%s', 1, '%s', '%s', '%s', '%s',
                        'STOCK', 1, '%s', '%s', 'AVAILABLE')
                """.formatted(
                        fixture.company(), supplierReturn, UUID.randomUUID(), fixture.order(),
                        fixture.orderLine(), receipt.id(), receipt.line(),
                        UUID.randomUUID(), UUID.randomUUID()));

        assertSqlState("23514", () -> confirmReturn(fixture.company(), supplierReturn));
        execute("""
                UPDATE plg_purchasing.supplier_return_line SET stock_movement_id = '%s'
                 WHERE company_id = '%s' AND supplier_return_id = '%s'
                """.formatted(UUID.randomUUID(), fixture.company(), supplierReturn));
        confirmReturn(fixture.company(), supplierReturn);
        assertSqlState("P2001", () -> execute("""
                UPDATE plg_purchasing.supplier_return SET return_reason = 'Changed'
                 WHERE company_id = '%s' AND supplier_return_id = '%s'
                """.formatted(fixture.company(), supplierReturn)));
    }

    private static Fixture seedOrder(UUID company, String suffix) throws SQLException {
        UUID request = UUID.randomUUID();
        UUID requestLine = UUID.randomUUID();
        UUID order = UUID.randomUUID();
        UUID orderLine = UUID.randomUUID();
        UUID catalogItem = UUID.randomUUID();
        execute("""
                INSERT INTO plg_purchasing.purchase_request
                    (company_id, purchase_request_id, request_number, requester_id,
                     requested_on, request_state, entity_version)
                VALUES ('%s', '%s', 'SC-%s', '%s', current_date, 'DRAFT', 0)
                """.formatted(company, request, suffix, UUID.randomUUID()));
        execute("""
                INSERT INTO plg_purchasing.purchase_request_line
                    (company_id, purchase_request_id, purchase_request_line_id, line_position,
                     catalog_item_id, catalog_code_snapshot, item_description_snapshot,
                     presented_unit_code_snapshot, base_unit_code_snapshot, conversion_factor,
                     line_kind, catalog_source_version, requested_quantity)
                VALUES ('%s', '%s', '%s', 1, '%s', 'ITEM-%s', 'Item', 'UN', 'UN', 1,
                        'STOCK', 1, 5)
                """.formatted(company, request, requestLine, catalogItem, suffix));
        execute("""
                INSERT INTO plg_purchasing.purchase_order
                    (company_id, purchase_order_id, order_number, supplier_id,
                     supplier_code_snapshot, supplier_name_snapshot, supplier_source_version,
                     currency_code, currency_minor_unit, currency_name_snapshot,
                     currency_release_id, direct_order_justification, order_state,
                     issued_by, issued_at, entity_version)
                VALUES ('%s', '%s', 'OC-%s', '%s', 'SUP-%s', 'Supplier', 1,
                        'PYG', 0, 'Guarani', 'ISO-2026', 'Direct test quantity',
                        'ISSUED', '%s', now(), 0)
                """.formatted(company, order, suffix, UUID.randomUUID(), suffix, UUID.randomUUID()));
        execute("""
                INSERT INTO plg_purchasing.purchase_order_line
                    (company_id, purchase_order_id, purchase_order_line_id, line_position,
                     catalog_item_id, catalog_code_snapshot, item_description_snapshot,
                     presented_unit_code_snapshot, base_unit_code_snapshot, conversion_factor,
                     line_kind, catalog_source_version, ordered_quantity,
                     unit_price, received_quantity, returned_quantity, short_closed_quantity)
                VALUES ('%s', '%s', '%s', 1, '%s', 'ITEM-%s', 'Item', 'UN', 'UN', 1, 'STOCK', 1,
                        5, 100, 0, 0, 0)
                """.formatted(company, order, orderLine, catalogItem, suffix));
        return new Fixture(company, request, requestLine, order, orderLine);
    }

    private static void insertAllocation(
            Fixture orderOwner, Fixture requestOwner, int quantity) throws SQLException {
        execute("""
                INSERT INTO plg_purchasing.purchase_order_allocation
                    (company_id, purchase_order_id, purchase_order_line_id,
                     purchase_request_id, purchase_request_line_id,
                     allocation_position, allocated_quantity)
                VALUES ('%s', '%s', '%s', '%s', '%s', 1, %s)
                """.formatted(
                        orderOwner.company(), orderOwner.order(), orderOwner.orderLine(),
                        requestOwner.request(), requestOwner.requestLine(), quantity));
    }

    private static Receipt seedConfirmedReceipt(Fixture fixture) throws SQLException {
        UUID receipt = UUID.randomUUID();
        UUID line = UUID.randomUUID();
        execute("""
                INSERT INTO plg_purchasing.goods_receipt
                    (company_id, goods_receipt_id, receipt_number, purchase_order_id,
                     receipt_state, entity_version)
                VALUES ('%s', '%s', 'RC-%s', '%s', 'DRAFT', 0)
                """.formatted(fixture.company(), receipt, receipt.toString().substring(0, 8), fixture.order()));
        execute("""
                INSERT INTO plg_purchasing.goods_receipt_line
                    (company_id, goods_receipt_id, goods_receipt_line_id, line_position,
                     purchase_order_id, purchase_order_line_id, line_kind, received_quantity,
                     warehouse_id, stock_location_id, stock_condition, stock_movement_id)
                VALUES ('%s', '%s', '%s', 1, '%s', '%s', 'STOCK', 2, '%s', '%s',
                        'AVAILABLE', '%s')
                """.formatted(
                        fixture.company(), receipt, line, fixture.order(), fixture.orderLine(),
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
        confirmReceipt(fixture.company(), receipt);
        return new Receipt(receipt, line);
    }

    private static void confirmReceipt(UUID company, UUID receipt) throws SQLException {
        execute("""
                UPDATE plg_purchasing.goods_receipt
                   SET receipt_state = 'CONFIRMED', confirmed_by = '%s',
                       confirmed_at = now(), entity_version = entity_version + 1
                 WHERE company_id = '%s' AND goods_receipt_id = '%s'
                """.formatted(UUID.randomUUID(), company, receipt));
    }

    private static void confirmReturn(UUID company, UUID supplierReturn) throws SQLException {
        execute("""
                UPDATE plg_purchasing.supplier_return
                   SET return_state = 'CONFIRMED', confirmed_by = '%s',
                       confirmed_at = now(), entity_version = entity_version + 1
                 WHERE company_id = '%s' AND supplier_return_id = '%s'
                """.formatted(UUID.randomUUID(), company, supplierReturn));
    }

    private static Flyway flyway() {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas("plg_purchasing")
                .defaultSchema("plg_purchasing")
                .table("flyway_schema_history")
                .locations("classpath:db/migration/purchasing")
                .createSchemas(true)
                .cleanDisabled(true)
                .validateMigrationNaming(true)
                .validateOnMigrate(true)
                .failOnMissingLocations(true)
                .outOfOrder(false)
                .load();
    }

    private static void assertSqlState(String expected, SqlOperation operation) {
        SQLException failure = assertThrows(SQLException.class, operation::execute);
        assertEquals(expected, failure.getSQLState());
    }

    private static void execute(String sql) throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static int queryInt(String sql) throws SQLException {
        try (Connection connection = connection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private record Fixture(
            UUID company, UUID request, UUID requestLine, UUID order, UUID orderLine) {
    }

    private record Receipt(UUID id, UUID line) {
    }

    @FunctionalInterface
    private interface SqlOperation {
        void execute() throws SQLException;
    }
}
