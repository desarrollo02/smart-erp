package py.com.logixone.plugins.inventory.persistence;

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
class InventoryMigrationPostgreSqlIT {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse(
                    "postgres:18.4-bookworm@sha256:16fa100a3a6e92c0556632870455e7f8c6f3df5cefddd67d6b95292732bd7ff0")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("logixone_inventory_test")
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
    void createsTenPrivateTablesAndIndependentHistory() throws SQLException {
        assertEquals(10, queryInt("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema = 'plg_inventory'
                  AND table_name <> 'flyway_schema_history'
                """));
        assertEquals(1, queryInt("""
                SELECT count(*) FROM plg_inventory.flyway_schema_history
                WHERE success AND version = '1'
                """));
        assertEquals(1, queryInt("""
                SELECT count(*) FROM plg_inventory.flyway_schema_history
                WHERE success AND version = '2'
                """));
    }

    @Test
    void scopesKeysAndForeignReferencesByCompany() throws SQLException {
        Fixture a = seedFixture(UUID.randomUUID(), "MAIN");
        Fixture b = seedFixture(UUID.randomUUID(), "MAIN");

        assertSqlState("23505", () -> insertWarehouse(a.company(), UUID.randomUUID(), "MAIN"));
        assertSqlState("23503", () -> execute("""
                INSERT INTO plg_inventory.inventory_balance
                    (company_id, inventory_balance_id, inventory_item_id, warehouse_id,
                     stock_location_id, condition_code, base_unit_code,
                     physical_quantity, reserved_quantity, entity_version)
                VALUES ('%s', '%s', '%s', '%s', '%s', 'AVAILABLE', 'EA', 1, 0, 0)
                """.formatted(b.company(), UUID.randomUUID(), a.item(), b.warehouse(), b.location())));
    }

    @Test
    void rejectsImpossibleBalancesAndDuplicatePositiveSerials() throws SQLException {
        Fixture fixture = seedFixture(UUID.randomUUID(), "BALANCE");

        assertSqlState("23514", () -> insertBalance(
                fixture, UUID.randomUUID(), null, "AVAILABLE", "2", "3"));
        insertBalance(fixture, UUID.randomUUID(), "SER-1", "AVAILABLE", "1", "0");
        assertSqlState("23505", () -> insertBalance(
                fixture, UUID.randomUUID(), "SER-1", "DAMAGED", "1", "0"));
        insertBalance(fixture, UUID.randomUUID(), "SER-1", "DAMAGED", "0", "0");
    }

    @Test
    void enforcesMovementIdempotencyAndSingleReversal() throws SQLException {
        Fixture fixture = seedFixture(UUID.randomUUID(), "LEDGER");
        UUID original = insertMovement(fixture.company(), "RECEIPT", "source", "idem-1", null);
        assertSqlState("23505", () -> insertMovement(
                fixture.company(), "RECEIPT", "source", "idem-1", null));
        insertMovement(fixture.company(), "REVERSAL", "source", "idem-r1", original);
        assertSqlState("23505", () -> insertMovement(
                fixture.company(), "REVERSAL", "other-source", "idem-r2", original));
        assertSqlState("23514", () -> insertMovement(
                fixture.company(), "RECEIPT", "source", "invalid-shape", original));
    }

    @Test
    void keepsOneImmutableReservationOperationPerCompanyAndKey() throws SQLException {
        Fixture fixture = seedFixture(UUID.randomUUID(), "RES-OPS");
        UUID reservation = insertReservation(fixture, "reserve-1");
        insertReservationOperation(fixture.company(), reservation, "consume-1");

        assertSqlState("23505", () ->
                insertReservationOperation(fixture.company(), reservation, "consume-1"));
        assertSqlState("23503", () ->
                insertReservationOperation(UUID.randomUUID(), reservation, "consume-other-company"));
    }

    @Test
    void preventsOverlappingActiveCountScopesButAllowsOtherWarehouses() throws SQLException {
        UUID company = UUID.randomUUID();
        Fixture first = seedFixture(company, "COUNT-A");
        Fixture second = seedFixture(company, "COUNT-B");
        insertCount(first, null, "COUNTING");

        assertSqlState("23P01", () -> insertCount(first, first.location(), "REVIEW"));
        insertCount(second, null, "COUNTING");
        insertCount(first, first.location(), "DRAFT");
    }

    private static Flyway flyway() {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas("plg_inventory")
                .defaultSchema("plg_inventory")
                .table("flyway_schema_history")
                .locations("classpath:db/migration/inventory")
                .createSchemas(true)
                .cleanDisabled(true)
                .validateMigrationNaming(true)
                .validateOnMigrate(true)
                .failOnMissingLocations(true)
                .outOfOrder(false)
                .load();
    }

    private static Fixture seedFixture(UUID company, String code) throws SQLException {
        UUID warehouse = UUID.randomUUID();
        UUID location = UUID.randomUUID();
        UUID item = UUID.randomUUID();
        insertWarehouse(company, warehouse, code);
        execute("""
                INSERT INTO plg_inventory.stock_location
                    (company_id, stock_location_id, warehouse_id, location_code,
                     location_name, location_type, active, entity_version)
                VALUES ('%s', '%s', '%s', 'GENERAL', 'General', 'GENERAL', true, 0)
                """.formatted(company, location, warehouse));
        execute("""
                INSERT INTO plg_inventory.inventory_item
                    (company_id, inventory_item_id, catalog_item_id, catalog_code_snapshot,
                     catalog_name_snapshot, base_unit_code_snapshot, catalog_item_version,
                     tracking_mode, expiry_policy, active, entity_version)
                VALUES ('%s', '%s', '%s', 'SKU-%s', 'Test product', 'EA', 0,
                        'SERIAL', 'NONE', true, 0)
                """.formatted(company, item, UUID.randomUUID(), code));
        return new Fixture(company, warehouse, location, item);
    }

    private static void insertWarehouse(UUID company, UUID warehouse, String code) throws SQLException {
        execute("""
                INSERT INTO plg_inventory.warehouse
                    (company_id, warehouse_id, warehouse_code, warehouse_name, active, entity_version)
                VALUES ('%s', '%s', '%s', 'Test warehouse', true, 0)
                """.formatted(company, warehouse, code));
    }

    private static void insertBalance(
            Fixture fixture, UUID balance, String serial, String condition,
            String physical, String reserved) throws SQLException {
        String serialValue = serial == null ? "NULL" : "'%s'".formatted(serial);
        execute("""
                INSERT INTO plg_inventory.inventory_balance
                    (company_id, inventory_balance_id, inventory_item_id, warehouse_id,
                     stock_location_id, serial_number, condition_code, base_unit_code,
                     physical_quantity, reserved_quantity, entity_version)
                VALUES ('%s', '%s', '%s', '%s', '%s', %s, '%s', 'EA', %s, %s, 0)
                """.formatted(fixture.company(), balance, fixture.item(), fixture.warehouse(),
                        fixture.location(), serialValue, condition, physical, reserved));
    }

    private static UUID insertMovement(
            UUID company, String type, String source, String idempotency, UUID reversal) throws SQLException {
        UUID movement = UUID.randomUUID();
        String reversalValue = reversal == null ? "NULL" : "'%s'".formatted(reversal);
        execute("""
                INSERT INTO plg_inventory.stock_movement
                    (company_id, stock_movement_id, movement_type, reason_code, source_type,
                     source_id, idempotency_key, posted_at, reversal_of_movement_id)
                VALUES ('%s', '%s', '%s', 'TEST', '%s', 'external-1', '%s', now(), %s)
                """.formatted(company, movement, type, source, idempotency, reversalValue));
        return movement;
    }

    private static UUID insertReservation(Fixture fixture, String idempotency) throws SQLException {
        UUID reservation = UUID.randomUUID();
        execute("""
                INSERT INTO plg_inventory.stock_reservation
                    (company_id, stock_reservation_id, inventory_item_id, warehouse_id,
                     stock_location_id, serial_number, condition_code, original_quantity,
                     consumed_quantity, released_quantity, source_type, source_id,
                     idempotency_key, reservation_state, created_at, expires_at, entity_version)
                VALUES ('%s', '%s', '%s', '%s', '%s', 'SER-RES', 'AVAILABLE', 2,
                        0, 0, 'ORDER', 'order-1', '%s', 'ACTIVE', now(),
                        now() + interval '1 day', 0)
                """.formatted(fixture.company(), reservation, fixture.item(),
                        fixture.warehouse(), fixture.location(), idempotency));
        return reservation;
    }

    private static void insertReservationOperation(
            UUID company, UUID reservation, String idempotency) throws SQLException {
        execute("""
                INSERT INTO plg_inventory.stock_reservation_operation
                    (company_id, idempotency_key, stock_reservation_id, operation_type,
                     operation_quantity, resulting_consumed_quantity,
                     resulting_released_quantity, resulting_remaining_quantity,
                     resulting_state, resulting_version, occurred_at)
                VALUES ('%s', '%s', '%s', 'CONSUME', 1, 1, 0, 1,
                        'PARTIALLY_CONSUMED', 1, now())
                """.formatted(company, idempotency, reservation));
    }

    private static void insertCount(Fixture fixture, UUID location, String state) throws SQLException {
        String locationValue = location == null ? "NULL" : "'%s'".formatted(location);
        execute("""
                INSERT INTO plg_inventory.stock_count
                    (company_id, stock_count_id, warehouse_id, stock_location_id,
                     count_state, entity_version)
                VALUES ('%s', '%s', '%s', %s, '%s', 0)
                """.formatted(fixture.company(), UUID.randomUUID(), fixture.warehouse(), locationValue, state));
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

    private record Fixture(UUID company, UUID warehouse, UUID location, UUID item) {}

    @FunctionalInterface
    private interface SqlOperation {
        void execute() throws SQLException;
    }
}
