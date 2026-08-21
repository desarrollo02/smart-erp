package py.com.logixone.plugins.sales.application;

import py.com.logixone.plugins.sales.application.port.SalesPersistenceCode;

final class SalesApplicationSupport {
    private SalesApplicationSupport() { }
    static SalesResultCode map(SalesPersistenceCode code) {
        return switch(code) {
            case NOT_FOUND -> SalesResultCode.NOT_FOUND;
            case VERSION_CONFLICT -> SalesResultCode.VERSION_CONFLICT;
            case DUPLICATE_NUMBER, DUPLICATE_SOURCE_QUOTE -> SalesResultCode.DUPLICATE;
            case IMMUTABLE_DOCUMENT, CONSTRAINT_VIOLATION -> SalesResultCode.STORAGE_FAILURE;
        };
    }
}
