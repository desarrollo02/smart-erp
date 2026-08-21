package py.com.logixone.plugins.sales.infrastructure.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.CurrentCompanyAuthorization;
import py.com.logixone.plugins.sales.api.*;
import py.com.logixone.plugins.sales.application.*;

@ApplicationScoped @Transactional(rollbackOn=RuntimeException.class)
public class CdiSalesDirectory implements SalesDirectory {
    @Inject CurrentCompanyAuthorization authorization; @Inject SalesUseCases useCases;
    @Override @Transactional(TxType.SUPPORTS)
    public Optional<SalesQuoteReference> findQuote(CompanyId companyId,SalesQuoteId id){
        var result=useCases.quote(context(companyId),id); return result.code()==SalesResultCode.NOT_FOUND?Optional.empty():Optional.of(required(result));}
    @Override @Transactional(TxType.SUPPORTS)
    public Optional<SalesOrderReference> findOrder(CompanyId companyId,SalesOrderId id){
        var result=useCases.order(context(companyId),id); return result.code()==SalesResultCode.NOT_FOUND?Optional.empty():Optional.of(required(result));}
    private SalesOperationContext context(CompanyId requested){Objects.requireNonNull(requested);var value=authorization.require(SalesIdentity.PLUGIN_ID.value(),SalesPermissions.VIEW.value());if(!requested.equals(value.context().companyId()))throw new SecurityException("Requested sales company differs from authorized company");return SalesOperationContext.from(value);}
    private static <T>T required(SalesOperationResult<T> value){if(!value.successful())throw new IllegalStateException("Sales operation failed: "+value.code());return value.value().orElseThrow();}
}
