package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.PersistenceException;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogPersistenceCode;

class PostgreSqlCatalogConflictMapperTest {

    @Test
    void mapsCatalogAndPriceListCodeUniqueness() {
        assertMapped("23505", "constraint uq_catalog_item_code", CatalogPersistenceCode.CODE_CONFLICT);
        assertMapped("23505", "constraint uq_price_list_code", CatalogPersistenceCode.CODE_CONFLICT);
        assertMapped("23505", "constraint uq_category_definition_code",
                CatalogPersistenceCode.CODE_CONFLICT);
        assertMapped("23505", "constraint uq_tax_profile_code",
                CatalogPersistenceCode.CODE_CONFLICT);
        assertMapped("23505", "constraint pk_unit_definition",
                CatalogPersistenceCode.CODE_CONFLICT);
    }

    @Test
    void mapsActiveIdentifierUniqueness() {
        assertMapped("23505", "constraint uq_catalog_item_identifier_active",
                CatalogPersistenceCode.IDENTIFIER_CONFLICT);
    }

    @Test
    void mapsProtectedValidityWindows() {
        assertMapped("23505", "constraint uq_price_entry_validity", CatalogPersistenceCode.VALIDITY_CONFLICT);
        assertMapped("23505", "constraint uq_tax_profile_revision_validity",
                CatalogPersistenceCode.VALIDITY_CONFLICT);
    }

    @Test
    void mapsForeignKeysAndChecksAsReferenceConflicts() {
        assertMapped("23503", "constraint fk_catalog_item_base_unit", CatalogPersistenceCode.REFERENCE_CONFLICT);
        assertMapped("23514", "constraint ck_catalog_item_state", CatalogPersistenceCode.REFERENCE_CONFLICT);
    }

    @Test
    void keepsUnknownSqlStatesClosedAndStable() {
        assertMapped("22001", "value too long", CatalogPersistenceCode.UNKNOWN_CONFLICT);
        assertMapped("23505", "unclassified unique constraint", CatalogPersistenceCode.UNKNOWN_CONFLICT);
    }

    @Test
    void keepsFrameworkFailuresWithoutSqlDetailsClosedAndStable() {
        var mapped = PostgreSqlCatalogConflictMapper.map(new PersistenceException("provider failure"));
        assertEquals(CatalogPersistenceCode.UNKNOWN_CONFLICT, mapped.code());
    }

    private static void assertMapped(String sqlState, String message, CatalogPersistenceCode expected) {
        var failure = new PersistenceException("persistence failure", new SQLException(message, sqlState));
        assertEquals(expected, PostgreSqlCatalogConflictMapper.map(failure).code());
    }
}
