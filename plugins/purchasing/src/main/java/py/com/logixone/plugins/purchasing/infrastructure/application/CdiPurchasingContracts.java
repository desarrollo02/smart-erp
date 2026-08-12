package py.com.logixone.plugins.purchasing.infrastructure.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.CurrentCompanyAuthorization;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.purchasing.api.OpenPurchaseOrderImport;
import py.com.logixone.plugins.purchasing.api.OpenPurchaseRequestImport;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderReference;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestReference;
import py.com.logixone.plugins.purchasing.api.PurchasingDirectory;
import py.com.logixone.plugins.purchasing.api.PurchasingImports;
import py.com.logixone.plugins.purchasing.application.PurchasingIdentity;
import py.com.logixone.plugins.purchasing.application.PurchasingOperationContext;
import py.com.logixone.plugins.purchasing.application.PurchasingOperationResult;
import py.com.logixone.plugins.purchasing.application.PurchasingPermissions;
import py.com.logixone.plugins.purchasing.application.PurchasingResultCode;
import py.com.logixone.plugins.purchasing.application.PurchasingUseCases;

@ApplicationScoped
@Transactional(rollbackOn = RuntimeException.class)
public class CdiPurchasingContracts implements PurchasingDirectory, PurchasingImports {
    @Inject CurrentCompanyAuthorization authorization;
    @Inject PurchasingUseCases useCases;

    @Override
    @Transactional(TxType.SUPPORTS)
    public Optional<PurchaseRequestReference> findRequest(
            CompanyId companyId, PurchaseRequestId requestId) {
        var result = useCases.request(context(companyId, PurchasingPermissions.VIEW), requestId);
        if (result.code() == PurchasingResultCode.NOT_FOUND) {
            return Optional.empty();
        }
        return Optional.of(required(result));
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public Optional<PurchaseOrderReference> findOrder(
            CompanyId companyId, PurchaseOrderId orderId) {
        var result = useCases.order(context(companyId, PurchasingPermissions.VIEW), orderId);
        if (result.code() == PurchasingResultCode.NOT_FOUND) {
            return Optional.empty();
        }
        return Optional.of(required(result));
    }

    @Override
    public PurchaseRequestReference importOpenRequest(
            CompanyId companyId, OpenPurchaseRequestImport request) {
        return required(useCases.importRequest(
                context(companyId, PurchasingPermissions.IMPORTS_EXECUTE), request));
    }

    @Override
    public PurchaseOrderReference importOpenOrder(
            CompanyId companyId, OpenPurchaseOrderImport request) {
        return required(useCases.importOrder(
                context(companyId, PurchasingPermissions.IMPORTS_EXECUTE), request));
    }

    private PurchasingOperationContext context(
            CompanyId requestedCompany, ContributionId permission) {
        Objects.requireNonNull(requestedCompany, "companyId");
        var authorized = authorization.require(
                PurchasingIdentity.PLUGIN_ID.value(), permission.value());
        if (!requestedCompany.equals(authorized.context().companyId())) {
            throw new SecurityException(
                    "Requested purchasing company differs from the authorized company");
        }
        return PurchasingOperationContext.from(authorized);
    }

    private static <T> T required(PurchasingOperationResult<T> result) {
        if (!result.successful()) {
            throw new IllegalStateException(
                    "Purchasing operation failed: " + result.code().name());
        }
        return result.value().orElseThrow();
    }
}
