package py.com.logixone.plugins.sales.infrastructure.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.Transactional;
import java.time.Clock;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerDirectory;
import py.com.logixone.plugins.commercialcatalog.api.*;
import py.com.logixone.plugins.inventory.api.InventoryReservations;
import py.com.logixone.plugins.referencedata.api.ReferenceDataDirectory;
import py.com.logixone.plugins.sales.api.*;
import py.com.logixone.plugins.sales.application.*;
import py.com.logixone.plugins.sales.application.command.SalesCommands;
import py.com.logixone.plugins.sales.application.port.*;
import py.com.logixone.plugins.sales.domain.*;

@ApplicationScoped @Transactional(rollbackOn=RuntimeException.class)
public class TransactionalSalesUseCases implements SalesUseCases {
    @Inject SalesTermRepository terms; @Inject SalesQuoteRepository quotes; @Inject SalesOrderRepository orders;
    @Inject SalesOperationRepository operations; @Inject SalesTransitionRepository transitions; @Inject SalesIdGenerator ids;
    @Inject BusinessPartnerDirectory partners; @Inject CatalogItemDirectory catalog; @Inject CatalogPricing pricing;
    @Inject ReferenceDataDirectory referenceData; @Inject InventoryReservations inventory; @Inject TechnicalAudit audit;
    @Inject TransactionSynchronizationRegistry transactions;
    private SalesCommandService service(){return new SalesCommandService(terms,quotes,orders,operations,transitions,ids,partners,catalog,pricing,referenceData,inventory,audit,Clock.systemUTC());}
    private <T> SalesOperationResult<T> mutation(SalesOperationResult<T> value){if(!value.successful())transactions.setRollbackOnly();return value;}
    public SalesOperationResult<SalesTerm> createTerm(SalesOperationContext c,SalesCommands.CreateTerm x){return mutation(service().createTerm(c,x));}
    public SalesOperationResult<SalesTerm> reviseTerm(SalesOperationContext c,SalesCommands.ReviseTerm x){return mutation(service().reviseTerm(c,x));}
    public SalesOperationResult<SalesTerm> deactivateTerm(SalesOperationContext c,SalesCommands.DeactivateTerm x){return mutation(service().deactivateTerm(c,x));}
    public SalesOperationResult<SalesQuote> createQuote(SalesOperationContext c,SalesCommands.CreateQuote x){return mutation(service().createQuote(c,x));}
    public SalesOperationResult<SalesQuote> issueQuote(SalesOperationContext c,SalesCommands.QuoteTransition x){return mutation(service().issueQuote(c,x));}
    public SalesOperationResult<SalesQuote> rejectQuote(SalesOperationContext c,SalesCommands.QuoteTransition x){return mutation(service().rejectQuote(c,x));}
    public SalesOperationResult<SalesQuote> expireQuote(SalesOperationContext c,SalesCommands.QuoteTransition x){return mutation(service().expireQuote(c,x));}
    public SalesOperationResult<SalesQuote> cancelQuote(SalesOperationContext c,SalesCommands.QuoteTransition x){return mutation(service().cancelQuote(c,x));}
    public SalesOperationResult<SalesOrder> acceptQuote(SalesOperationContext c,SalesCommands.AcceptQuote x){return mutation(service().acceptQuote(c,x));}
    public SalesOperationResult<SalesOrder> createOrder(SalesOperationContext c,SalesCommands.CreateOrder x){return mutation(service().createOrder(c,x));}
    public SalesOperationResult<SalesOrder> confirmOrder(SalesOperationContext c,SalesCommands.ConfirmOrder x){return mutation(service().confirmOrder(c,x));}
    public SalesOperationResult<SalesOrder> cancelOrder(SalesOperationContext c,SalesCommands.CancelOrder x){return mutation(service().cancelOrder(c,x));}
    public SalesOperationResult<SalesOrder> closeOrder(SalesOperationContext c,SalesCommands.CloseOrder x){return mutation(service().closeOrder(c,x));}
    public SalesOperationResult<SalesQuoteReference> quote(SalesOperationContext c,SalesQuoteId id){return service().quote(c,id);}
    public SalesOperationResult<SalesOrderReference> order(SalesOperationContext c,SalesOrderId id){return service().order(c,id);}
}
