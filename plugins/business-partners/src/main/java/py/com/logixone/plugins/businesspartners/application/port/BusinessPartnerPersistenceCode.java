package py.com.logixone.plugins.businesspartners.application.port;

/** Stable persistence outcomes interpreted later by the application layer. */
public enum BusinessPartnerPersistenceCode {
    PARTNER_NOT_FOUND,
    DEFINITION_NOT_FOUND,
    PARTNER_ALREADY_EXISTS,
    GENERAL_CODE_ALREADY_EXISTS,
    ROLE_CODE_ALREADY_EXISTS,
    VERSION_CONFLICT,
    INVALID_PERSISTED_STATE
}
