package py.com.logixone.plugins.commercialcatalog.application.port;

/** Stable persistence outcomes that application can translate without SQL knowledge. */
public enum CatalogPersistenceCode {
    ITEM_NOT_FOUND,
    PRICE_LIST_NOT_FOUND,
    DEFINITION_NOT_FOUND,
    VERSION_CONFLICT,
    CODE_CONFLICT,
    IDENTIFIER_CONFLICT,
    REFERENCE_CONFLICT,
    VALIDITY_CONFLICT,
    UNKNOWN_CONFLICT
}
