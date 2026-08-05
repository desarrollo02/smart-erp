package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import jakarta.persistence.PersistenceException;
import java.sql.SQLException;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogPersistenceCode;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogPersistenceException;

final class PostgreSqlCatalogConflictMapper {
    private PostgreSqlCatalogConflictMapper() { }

    static CatalogPersistenceException map(PersistenceException failure) {
        SQLException sql = findSqlException(failure);
        if (sql == null) {
            return new CatalogPersistenceException(CatalogPersistenceCode.UNKNOWN_CONFLICT, failure);
        }
        String message = sql.getMessage() == null ? "" : sql.getMessage();
        if ("23505".equals(sql.getSQLState())) {
            if (message.contains("uq_catalog_item_code")
                    || message.contains("uq_price_list_code")
                    || message.contains("uq_category_definition_code")
                    || message.contains("uq_brand_definition_code")
                    || message.contains("uq_tag_definition_code")
                    || message.contains("uq_tax_profile_code")
                    || message.contains("uq_variant_family_code")
                    || message.contains("pk_unit_definition")) {
                return new CatalogPersistenceException(CatalogPersistenceCode.CODE_CONFLICT, failure);
            }
            if (message.contains("uq_catalog_item_identifier_active")) {
                return new CatalogPersistenceException(CatalogPersistenceCode.IDENTIFIER_CONFLICT, failure);
            }
            if (message.contains("uq_price_entry_validity") || message.contains("uq_tax_profile_revision_validity")) {
                return new CatalogPersistenceException(CatalogPersistenceCode.VALIDITY_CONFLICT, failure);
            }
            return new CatalogPersistenceException(CatalogPersistenceCode.UNKNOWN_CONFLICT, failure);
        }
        if ("23503".equals(sql.getSQLState()) || "23514".equals(sql.getSQLState())) {
            return new CatalogPersistenceException(CatalogPersistenceCode.REFERENCE_CONFLICT, failure);
        }
        return new CatalogPersistenceException(CatalogPersistenceCode.UNKNOWN_CONFLICT, failure);
    }

    private static SQLException findSqlException(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SQLException sql) { return sql; }
            current = current.getCause();
        }
        return null;
    }
}
