package py.com.logixone.migrator;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class CoreMigrationResourceTest {

    @Test
    void initialMigrationRemainsByteForByteImmutableAndOwnsOnlyCore() throws IOException {
        byte[] bytes = resource("/db/migration/core/V1__initialize_core_schema.sql");
        String sql = new String(bytes, StandardCharsets.UTF_8).toLowerCase();

        assertEquals(
                "07a375f06f9ebb9d6e6ec162e113ada35397348bfcd03486870faf28cc424da6",
                sha256(bytes));
        assertTrue(sql.contains("create table core.system_metadata"));
        assertTrue(sql.contains("'schema_owner', 'core'"));
        assertTrue(sql.lines()
                .filter(line -> !line.isBlank())
                .noneMatch(line -> line.contains("plg_")));
    }

    @Test
    void secondMigrationAddsOnlyTheApprovedCoreStructures() throws IOException {
        byte[] bytes = resource("/db/migration/core/V2__add_companies_and_plugin_activation.sql");
        String sql = new String(bytes, StandardCharsets.UTF_8)
                .toLowerCase();

        assertEquals(
                "f5186a3817f7a31569c58551a9339911b29b44f7409e47ae470fc999afa5cc11",
                sha256(bytes));
        assertTrue(sql.contains("create table core.company ("));
        assertTrue(sql.contains("create table core.company_plugin_activation ("));
        assertTrue(sql.contains("company_id uuid"));
        assertTrue(sql.contains("customization_plugin_id varchar(59) not null"));
        assertTrue(sql.contains("unique (customization_plugin_id)"));
        assertTrue(sql.contains("primary key (company_id, plugin_id)"));
        assertTrue(sql.contains("references core.company (company_id) on delete restrict"));
        assertTrue(sql.contains("desired_state in ('disabled', 'enabled')"));
        assertTrue(sql.contains("version >= 0"));
        assertTrue(sql.lines()
                .filter(line -> !line.isBlank())
                .noneMatch(line -> line.contains("plg_")));
    }

    @Test
    void thirdAppliedMigrationRemainsImmutableAndOwnsOnlyCoreSecurity() throws IOException {
        byte[] bytes = resource(
                "/db/migration/core/V3__add_identity_membership_and_authorization.sql");
        String sql = new String(bytes, StandardCharsets.UTF_8).toLowerCase();

        assertEquals(
                "6c34c64c0739f4988287c7b9dba5a0dff2808c976b30a0b2c066f382f7961170",
                sha256(bytes));
        assertTrue(sql.contains("create table core.app_user ("));
        assertTrue(sql.contains("create table core.company_membership ("));
        assertTrue(sql.contains("create table core.security_role ("));
        assertTrue(sql.lines().noneMatch(line -> line.contains("plg_")));
    }

    @Test
    void fourthMigrationAddsOnlyGlobalAuthorityStructures() throws IOException {
        byte[] bytes = resource("/db/migration/core/V4__add_system_authority.sql");
        String sql = new String(bytes, StandardCharsets.UTF_8).toLowerCase();

        assertEquals(
                "8c35ef550ffc0949915758389781b25f9243a1e49aec8ac2afc16f26cb46b67a",
                sha256(bytes));
        assertTrue(sql.contains("create table core.system_role ("));
        assertTrue(sql.contains("create table core.system_role_permission ("));
        assertTrue(sql.contains("create table core.app_user_system_role ("));
        assertFalse(sql.contains("kernel.system_administration.manage"));
        assertTrue(sql.lines().noneMatch(line -> line.contains("plg_")));
    }

    @Test
    void fifthMigrationAddsAppendOnlyTechnicalAuditWithoutBackfill() throws IOException {
        byte[] bytes = resource("/db/migration/core/V5__add_technical_audit_event.sql");
        String sql = new String(bytes, StandardCharsets.UTF_8).toLowerCase();

        assertEquals(
                "0aacba3999424dbb00337d7df39936e9d702e1e2df8d413a80817e5c8a52d625",
                sha256(bytes));
        assertTrue(sql.contains("create table core.audit_event ("));
        assertTrue(sql.contains("before update or delete on core.audit_event"));
        assertTrue(sql.contains("create index audit_event_occurred_idx"));
        assertTrue(sql.lines().noneMatch(line -> line.stripLeading().startsWith("insert ")));
        assertTrue(sql.lines().noneMatch(line -> line.contains("plg_")));
    }

    @Test
    void sixthMigrationAddsPluginAuditResourcesWithoutCommercialData() throws IOException {
        byte[] bytes = resource(
                "/db/migration/core/V6__extend_audit_for_plugin_operations.sql");
        String sql = new String(bytes, StandardCharsets.UTF_8).toLowerCase();

        assertEquals(
                "ac4f1128e6ed31618376d213bc801b29c77b0dba99f3aec7c49b1dd10b4bee35",
                sha256(bytes));
        assertTrue(sql.contains("'plugin_operation'"));
        assertTrue(sql.contains("add column resource_type varchar(96)"));
        assertTrue(sql.contains("add column resource_id varchar(160)"));
        assertTrue(sql.contains("create index audit_event_resource_idx"));
        assertTrue(sql.lines().noneMatch(line -> line.stripLeading().startsWith("insert ")));
        assertTrue(sql.lines().noneMatch(line -> line.contains("plg_")));
    }

    private byte[] resource(String path) throws IOException {
        try (InputStream resource = getClass().getResourceAsStream(path)) {
            assertNotNull(resource);
            return resource.readAllBytes();
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }
}
