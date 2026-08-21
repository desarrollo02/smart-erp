package py.com.logixone.plugins.sales.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerDirectory;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemDirectory;
import py.com.logixone.plugins.commercialcatalog.api.CatalogPricing;
import py.com.logixone.plugins.inventory.api.*;
import py.com.logixone.plugins.referencedata.api.ReferenceDataDirectory;
import py.com.logixone.plugins.sales.api.*;
import py.com.logixone.plugins.sales.application.command.SalesCommands;
import py.com.logixone.plugins.sales.application.port.*;
import py.com.logixone.plugins.sales.domain.*;

public final class SalesCommandService extends SalesApplicationService implements SalesUseCases {
    private static final String TERM="sales_term",QUOTE="sales_quote",ORDER="sales_order";
    private final SalesTermRepository terms; private final SalesQuoteRepository quotes;
    private final SalesOrderRepository orders; private final SalesTransitionRepository transitions;
    private final SalesIdGenerator ids; private final InventoryReservations inventory;
    private final SalesReferenceResolver references;

    public SalesCommandService(SalesTermRepository terms, SalesQuoteRepository quotes,
            SalesOrderRepository orders, SalesOperationRepository operations,
            SalesTransitionRepository transitions, SalesIdGenerator ids,
            BusinessPartnerDirectory partners, CatalogItemDirectory catalog,
            CatalogPricing pricing, ReferenceDataDirectory referenceData,
            InventoryReservations inventory, TechnicalAudit audit, Clock clock) {
        super(operations,audit,clock); this.terms=terms; this.quotes=quotes; this.orders=orders;
        this.transitions=transitions; this.ids=ids; this.inventory=inventory;
        this.references=new SalesReferenceResolver(partners,catalog,pricing,referenceData,terms,clock);
    }

    @Override public SalesOperationResult<SalesTerm> createTerm(SalesOperationContext c, SalesCommands.CreateTerm x) {
        return mutate(c,SalesPermissions.TERMS_MANAGE,"CREATE_SALES_TERM",TERM,x.idempotencyKey(),x,
                Optional.empty(),id->terms.findById(company(c),id),v->v.snapshot().version(),()->{
                    var value=SalesTerm.active(company(c),ids.nextTermId(),x.code(),x.displayName(),x.dueDays()); terms.insert(value); return value;});
    }
    @Override public SalesOperationResult<SalesTerm> reviseTerm(SalesOperationContext c, SalesCommands.ReviseTerm x) {
        return mutate(c,SalesPermissions.TERMS_MANAGE,"REVISE_SALES_TERM",TERM,x.idempotencyKey(),x,
                Optional.of(x.termId()),id->terms.findById(company(c),id),v->v.snapshot().version(),()->{
                    var value=terms.findById(company(c),x.termId()).orElseThrow(SalesReferenceResolver.ReferenceFailure::new);
                    long previous=value.snapshot().version(); value.revise(x.displayName(),x.dueDays(),x.expectedVersion()); return terms.update(value,previous);});
    }
    @Override public SalesOperationResult<SalesTerm> deactivateTerm(SalesOperationContext c, SalesCommands.DeactivateTerm x) {
        return mutate(c,SalesPermissions.TERMS_MANAGE,"DEACTIVATE_SALES_TERM",TERM,x.idempotencyKey(),x,
                Optional.of(x.termId()),id->terms.findById(company(c),id),v->v.snapshot().version(),()->{
                    var value=terms.findById(company(c),x.termId()).orElseThrow(SalesReferenceResolver.ReferenceFailure::new);
                    long previous=value.snapshot().version(); value.deactivate(x.expectedVersion()); return terms.update(value,previous);});
    }
    @Override public SalesOperationResult<SalesQuote> createQuote(SalesOperationContext c, SalesCommands.CreateQuote x) {
        ContributionId permission=x.overridesPrice()?SalesPermissions.PRICE_OVERRIDE:SalesPermissions.QUOTES_CREATE;
        return mutate(c,permission,"CREATE_SALES_QUOTE",QUOTE,x.idempotencyKey(),x,Optional.empty(),
                id->quotes.findById(company(c),new SalesQuoteId(id)),SalesQuote::version,()->{
                    var currency=references.currency(company(c),x.currencyCode());
                    var lines=x.lines().stream().map(v->references.line(company(c),x.currencyCode(),v)).toList();
                    var value=SalesQuote.draft(company(c),ids.nextQuoteId(),x.number(),
                            references.customer(company(c),x.customerId(),x.customerTaxId()),currency,
                            references.term(company(c),x.termId()),x.validUntil(),lines);
                    return quotes.insert(value);});
    }
    @Override public SalesOperationResult<SalesQuote> issueQuote(SalesOperationContext c, SalesCommands.QuoteTransition x) {
        return quoteTransition(c,SalesPermissions.QUOTES_ISSUE,"ISSUE_SALES_QUOTE",x,
                q->q.issue(actor(c),clock.instant(),x.expectedVersion()));
    }
    @Override public SalesOperationResult<SalesQuote> rejectQuote(SalesOperationContext c, SalesCommands.QuoteTransition x) {
        return quoteTransition(c,SalesPermissions.QUOTES_ACCEPT,"REJECT_SALES_QUOTE",x,
                q->q.reject(actor(c),clock.instant(),x.expectedVersion()));
    }
    @Override public SalesOperationResult<SalesQuote> expireQuote(SalesOperationContext c, SalesCommands.QuoteTransition x) {
        return quoteTransition(c,SalesPermissions.QUOTES_ISSUE,"EXPIRE_SALES_QUOTE",x,
                q->q.expire(actor(c),clock.instant(),x.expectedVersion()));
    }
    @Override public SalesOperationResult<SalesQuote> cancelQuote(SalesOperationContext c, SalesCommands.QuoteTransition x) {
        return quoteTransition(c,SalesPermissions.QUOTES_CANCEL,"CANCEL_SALES_QUOTE",x,
                q->q.cancel(actor(c),x.expectedVersion()));
    }
    @Override public SalesOperationResult<SalesOrder> acceptQuote(SalesOperationContext c, SalesCommands.AcceptQuote x) {
        return mutate(c,SalesPermissions.QUOTES_ACCEPT,"ACCEPT_SALES_QUOTE",ORDER,x.idempotencyKey(),x,
                Optional.empty(),id->orders.findById(company(c),new SalesOrderId(id)),SalesOrder::version,()->{
                    var quote=quotes.findById(company(c),x.quoteId()).orElseThrow(SalesReferenceResolver.ReferenceFailure::new);
                    String from=quote.state().name(); long previous=quote.version();
                    quote.accept(actor(c),clock.instant(),x.expectedVersion());
                    var s=quote.snapshot(); var order=SalesOrder.fromQuote(company(c),ids.nextOrderId(),x.orderNumber(),quote,
                            s.customer(),s.currency(),s.term());
                    quotes.update(quote,previous); orders.insert(order);
                    history(c,QUOTE,s.id().value(),from,quote.state().name(),Optional.empty(),x.idempotencyKey()); return order;});
    }
    @Override public SalesOperationResult<SalesOrder> createOrder(SalesOperationContext c, SalesCommands.CreateOrder x) {
        ContributionId permission=x.overridesPrice()?SalesPermissions.PRICE_OVERRIDE:SalesPermissions.ORDERS_CREATE;
        return mutate(c,permission,"CREATE_SALES_ORDER",ORDER,x.idempotencyKey(),x,Optional.empty(),
                id->orders.findById(company(c),new SalesOrderId(id)),SalesOrder::version,()->{
                    var currency=references.currency(company(c),x.currencyCode());
                    var lines=x.lines().stream().map(v->references.line(company(c),x.currencyCode(),v)).toList();
                    var value=SalesOrder.direct(company(c),ids.nextOrderId(),x.number(),
                            references.customer(company(c),x.customerId(),x.customerTaxId()),currency,
                            references.term(company(c),x.termId()),lines); return orders.insert(value);});
    }
    @Override public SalesOperationResult<SalesOrder> confirmOrder(SalesOperationContext c, SalesCommands.ConfirmOrder x) {
        return mutate(c,SalesPermissions.ORDERS_CONFIRM,"CONFIRM_SALES_ORDER",ORDER,x.idempotencyKey(),x,
                Optional.of(x.orderId().value()),id->orders.findById(company(c),new SalesOrderId(id)),SalesOrder::version,()->{
                    var order=requiredOrder(c,x.orderId()); var s=order.snapshot();
                    var required=s.lines().stream().filter(SalesLineSnapshot::stockManaged).map(SalesLineSnapshot::id).collect(Collectors.toSet());
                    var inputs=x.reservations().stream().collect(Collectors.toMap(SalesCommands.ReservationInput::lineId,v->v));
                    if(!inputs.keySet().equals(required)) throw new IllegalArgumentException("Reservations must match stock lines");
                    Map<UUID,StockReservationId> reserved=new LinkedHashMap<>();
                    for(var line:s.lines()) if(line.stockManaged()) {
                        var input=inputs.get(line.id()); var request=new CatalogStockReservationRequest(
                                line.catalogItemId().value(),input.warehouseId(),input.locationId(),input.lotCode(),input.serialNumber(),
                                input.expiryDate(),input.condition(),line.quantity(),new StockSourceReference("SALES_ORDER",s.id().toString()),
                                input.expiresAt(),x.idempotencyKey()+":line:"+line.id());
                        reserved.put(line.id(),reserve(company(c),request).id());
                    }
                    String from=order.state().name(); long previous=order.version(); order.confirm(reserved,actor(c),clock.instant(),x.expectedVersion());
                    orders.update(order,previous); history(c,ORDER,s.id().value(),from,order.state().name(),Optional.empty(),x.idempotencyKey()); return order;});
    }
    @Override public SalesOperationResult<SalesOrder> cancelOrder(SalesOperationContext c, SalesCommands.CancelOrder x) {
        return mutate(c,SalesPermissions.ORDERS_CANCEL,"CANCEL_SALES_ORDER",ORDER,x.idempotencyKey(),x,
                Optional.of(x.orderId().value()),id->orders.findById(company(c),new SalesOrderId(id)),SalesOrder::version,()->{
                    var order=requiredOrder(c,x.orderId()); var s=order.snapshot();
                    for(var reservationId:s.reservations().values()) {
                        var reservation=findReservation(company(c),reservationId);
                        if(reservation.remainingQuantity().signum()>0) release(company(c),reservationId,
                                reservation.version(),reservation.remainingQuantity(),x.idempotencyKey()+":release:"+reservationId);
                    }
                    String from=order.state().name(); long previous=order.version(); order.cancel(actor(c),clock.instant(),x.expectedVersion());
                    orders.update(order,previous); history(c,ORDER,s.id().value(),from,order.state().name(),Optional.of(x.reason()),x.idempotencyKey()); return order;});
    }
    @Override public SalesOperationResult<SalesOrder> closeOrder(SalesOperationContext c, SalesCommands.CloseOrder x) {
        return mutate(c,SalesPermissions.ORDERS_CLOSE,"CLOSE_SALES_ORDER",ORDER,x.idempotencyKey(),x,
                Optional.of(x.orderId().value()),id->orders.findById(company(c),new SalesOrderId(id)),SalesOrder::version,()->{
                    var order=requiredOrder(c,x.orderId()); var s=order.snapshot(); String from=order.state().name(); long previous=order.version();
                    order.close(actor(c),clock.instant(),x.expectedVersion()); orders.update(order,previous);
                    history(c,ORDER,s.id().value(),from,order.state().name(),Optional.empty(),x.idempotencyKey()); return order;});
    }
    @Override public SalesOperationResult<SalesQuoteReference> quote(SalesOperationContext c, SalesQuoteId id) {
        if(!c.authorizes(SalesPermissions.VIEW)) return audit.rejected(c,SalesPermissions.VIEW,"VIEW_SALES_QUOTE",QUOTE,Optional.of(id.toString()),Optional.empty(),SalesResultCode.ACCESS_DENIED);
        return quotes.findById(company(c),id).map(v->SalesOperationResult.success(v.reference())).orElseGet(()->SalesOperationResult.failure(SalesResultCode.NOT_FOUND));
    }
    @Override public SalesOperationResult<SalesOrderReference> order(SalesOperationContext c, SalesOrderId id) {
        if(!c.authorizes(SalesPermissions.VIEW)) return audit.rejected(c,SalesPermissions.VIEW,"VIEW_SALES_ORDER",ORDER,Optional.of(id.toString()),Optional.empty(),SalesResultCode.ACCESS_DENIED);
        return orders.findById(company(c),id).map(v->SalesOperationResult.success(v.reference())).orElseGet(()->SalesOperationResult.failure(SalesResultCode.NOT_FOUND));
    }

    private SalesOperationResult<SalesQuote> quoteTransition(SalesOperationContext c,ContributionId permission,String operation,
            SalesCommands.QuoteTransition x,Consumer<SalesQuote> action) {
        return mutate(c,permission,operation,QUOTE,x.idempotencyKey(),x,Optional.of(x.quoteId().value()),
                id->quotes.findById(company(c),new SalesQuoteId(id)),SalesQuote::version,()->{
                    var quote=quotes.findById(company(c),x.quoteId()).orElseThrow(SalesReferenceResolver.ReferenceFailure::new);
                    String from=quote.state().name(); long previous=quote.version(); action.accept(quote); quotes.update(quote,previous);
                    history(c,QUOTE,x.quoteId().value(),from,quote.state().name(),x.reason(),x.idempotencyKey()); return quote;});
    }
    private <T> SalesOperationResult<T> mutate(SalesOperationContext c,ContributionId permission,String operation,String type,
            String key,Object command,Optional<UUID> requested,java.util.function.Function<UUID,Optional<T>> loader,
            java.util.function.ToLongFunction<T> version,java.util.function.Supplier<T> action) {
        if(!c.authorizes(permission)) return audit.rejected(c,permission,operation,type,requested.map(Object::toString),Optional.empty(),SalesResultCode.ACCESS_DENIED);
        var replay=replay(c,permission,operation,type,key,command,loader,version); if(replay.isPresent()) return replay.orElseThrow();
        try { T value=action.get(); UUID id=aggregateId(value); remember(c,key,operation,command,type,id);
            audit.changed(c,permission,operation,type,id.toString(),Optional.empty(),version.applyAsLong(value)); return SalesOperationResult.success(value);
        } catch(RuntimeException failure) {
            if(failure instanceof InventoryFailure) return audit.rejected(c,permission,operation,type,requested.map(Object::toString),Optional.empty(),SalesResultCode.INVENTORY_FAILURE);
            return failure(c,permission,operation,type,requested.map(Object::toString),Optional.empty(),failure); }
    }
    private UUID aggregateId(Object value) { return switch(value) { case SalesTerm v->v.snapshot().id(); case SalesQuote v->v.reference().id().value(); case SalesOrder v->v.reference().id().value(); default->throw new IllegalArgumentException("Unsupported aggregate");}; }
    private SalesOrder requiredOrder(SalesOperationContext c,SalesOrderId id){return orders.findById(company(c),id).orElseThrow(SalesReferenceResolver.ReferenceFailure::new);}
    private StockReservationReference reserve(CompanyId company,CatalogStockReservationRequest request){try{return inventory.reserveCatalogItem(company,request);}catch(RuntimeException failure){throw new InventoryFailure(failure);}}
    private StockReservationReference findReservation(CompanyId company,StockReservationId id){try{return inventory.find(company,id).orElseThrow(InventoryFailure::new);}catch(InventoryFailure failure){throw failure;}catch(RuntimeException failure){throw new InventoryFailure(failure);}}
    private void release(CompanyId company,StockReservationId id,long version,BigDecimal quantity,String key){try{inventory.release(company,id,version,quantity,key);}catch(RuntimeException failure){throw new InventoryFailure(failure);}}
    private void history(SalesOperationContext c,String type,UUID id,String from,String to,Optional<String> reason,String key){transitions.append(new SalesTransitionRecord(company(c),ids.nextTransitionId(),type,id,from,to,actor(c),reason,clock.instant(),key));}
    private static CompanyId company(SalesOperationContext c){return c.companyContext().companyId();}
    private static py.com.logixone.kernel.api.security.AppUserId actor(SalesOperationContext c){return c.companyContext().actor().userId();}
    private static final class InventoryFailure extends RuntimeException { InventoryFailure(){} InventoryFailure(Throwable cause){super(cause);} }
}
