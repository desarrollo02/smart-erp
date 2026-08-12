package py.com.logixone.plugins.purchasing.application;

import java.util.Objects;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.purchasing.application.port.PurchasingPersistenceCode;

final class PurchasingApplicationSupport {
    private PurchasingApplicationSupport() {
    }

    static boolean authorized(PurchasingOperationContext context, ContributionId permission) {
        return Objects.requireNonNull(context, "context").authorizes(permission);
    }

    static PurchasingResultCode map(PurchasingPersistenceCode code) {
        return switch (Objects.requireNonNull(code, "code")) {
            case DUPLICATE -> PurchasingResultCode.DUPLICATE;
            case REFERENCE_CONFLICT -> PurchasingResultCode.REFERENCE_CONFLICT;
            case VERSION_CONFLICT -> PurchasingResultCode.VERSION_CONFLICT;
            case IMMUTABLE_DOCUMENT -> PurchasingResultCode.IMMUTABLE_DOCUMENT;
            case NOT_FOUND -> PurchasingResultCode.NOT_FOUND;
            case STORAGE_FAILURE -> PurchasingResultCode.STORAGE_FAILURE;
        };
    }
}
