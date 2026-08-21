package py.com.logixone.plugins.sales.application;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.audit.TechnicalAuditEvent;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.*;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.businesspartners.api.*;
import py.com.logixone.plugins.commercialcatalog.api.*;
import py.com.logixone.plugins.inventory.api.*;
import py.com.logixone.plugins.referencedata.api.*;
import py.com.logixone.plugins.sales.api.*;
import py.com.logixone.plugins.sales.application.command.SalesCommands;
import py.com.logixone.plugins.sales.application.port.*;
import py.com.logixone.plugins.sales.domain.*;

class SalesCommandServiceTest {
    private static final CompanyId COMPANY=new CompanyId(uuid(1));
    private static final BusinessPartnerId CUSTOMER=new BusinessPartnerId(uuid(2));
    private static final CatalogItemId ITEM=new CatalogItemId(uuid(3));
    private static final PriceListId PRICE_LIST=new PriceListId(uuid(4));
    private static final UUID TERM=uuid(5),LINE=uuid(6);
    private static final WarehouseId WAREHOUSE=new WarehouseId(uuid(7));
    private static final StockLocationId LOCATION=new StockLocationId(uuid(8));
    private static final Clock CLOCK=Clock.fixed(Instant.parse("2026-08-21T12:00:00Z"),ZoneOffset.UTC);
    private MemoryTerms terms; private MemoryOrders orders; private MemoryOperations operations;
    private FakeInventory inventory; private CountingIds ids; private int partnerLookups;
    private SalesCommandService service;

    @BeforeEach void setUp(){
        terms=new MemoryTerms(); terms.insert(SalesTerm.active(COMPANY,TERM,"CONTADO","Contado",0));
        orders=new MemoryOrders(); operations=new MemoryOperations(); inventory=new FakeInventory(); ids=new CountingIds();
        var quotes=new MemoryQuotes(); var history=new ArrayList<SalesTransitionRecord>(); var audits=new ArrayList<TechnicalAuditEvent>();
        BusinessPartnerDirectory partners=new BusinessPartnerDirectory(){
            public Optional<BusinessPartnerReference> findById(CompanyId c,BusinessPartnerId id){partnerLookups++;return c.equals(COMPANY)&&id.equals(CUSTOMER)?Optional.of(new BusinessPartnerReference(id,"CLI-1","Cliente",BusinessPartnerKind.ORGANIZATION,BusinessPartnerState.ACTIVE,Set.of(BusinessPartnerRole.CLIENT),2)):Optional.empty();}
            public BusinessPartnerSearchPage search(CompanyId c,BusinessPartnerSearchQuery q){throw new UnsupportedOperationException();}
        };
        CatalogItemDirectory catalog=new CatalogItemDirectory(){
            public Optional<CatalogItemReference> findById(CompanyId c,CatalogItemId id){return Optional.of(new CatalogItemReference(id,"ITEM-1","Artículo",CatalogItemType.PRODUCT,CatalogItemState.ACTIVE,Set.of(CatalogItemScope.SALE),"EA",3));}
            public CatalogSearchPage search(CompanyId c,CatalogSearchCriteria q){throw new UnsupportedOperationException();}
        };
        CatalogPricing pricing=(c,q)->Optional.of(new CatalogPriceQuote(q.priceListId(),new PriceEntryId(uuid(9)),q.itemId(),"PYG",CatalogTaxMode.TAX_INCLUDED,q.unitCode(),q.quantity(),new BigDecimal("100"),q.quantity().multiply(new BigDecimal("100")),CLOCK.instant(),Optional.empty(),4));
        ReferenceDataDirectory referenceData=new ReferenceDataDirectory(){
            public ReferenceDataRelease currentRelease(CompanyId c,ReferenceDataCatalog x){throw new UnsupportedOperationException();}
            public List<CountryReference> countries(CompanyId c){return List.of();}
            public Optional<CountryReference> findCountry(CompanyId c,CountryCode x){return Optional.empty();}
            public List<CurrencyReference> currencies(CompanyId c){return List.of(currency());}
            public Optional<CurrencyReference> findCurrency(CompanyId c,CurrencyCode x){return x.value().equals("PYG")?Optional.of(currency()):Optional.empty();}
        };
        service=new SalesCommandService(terms,quotes,orders,operations,history::add,ids,partners,catalog,pricing,referenceData,inventory,audits::add,CLOCK);
    }

    @Test void deniesBeforeResolvingReferencesOrGeneratingIdentity(){
        var result=service.createOrder(context(SalesPermissions.VIEW),create("create-denied"));
        assertEquals(SalesResultCode.ACCESS_DENIED,result.code()); assertEquals(0,partnerLookups); assertEquals(0,ids.orderCalls);
    }

    @Test void createsOnceAndReturnsCurrentAggregateOnIdempotentRetry(){
        var first=service.createOrder(context(SalesPermissions.ORDERS_CREATE),create("create-1"));
        var retry=service.createOrder(context(SalesPermissions.ORDERS_CREATE),create("create-1"));
        assertTrue(first.successful()); assertEquals(first.value().orElseThrow().reference(),retry.value().orElseThrow().reference());
        assertEquals(1,ids.orderCalls); assertEquals(1,orders.values.size()); assertEquals(1,operations.values.size());
    }

    @Test void confirmsWithCatalogReservationAndReleasesRemainingQuantityOnCancel(){
        var order=service.createOrder(context(SalesPermissions.ORDERS_CREATE),create("create-2")).value().orElseThrow();
        var confirm=new SalesCommands.ConfirmOrder(order.reference().id(),0,List.of(new SalesCommands.ReservationInput(
                LINE,WAREHOUSE,LOCATION,Optional.empty(),Optional.empty(),Optional.empty(),StockCondition.AVAILABLE,
                Instant.parse("2026-08-22T12:00:00Z"))),"confirm-1");
        var confirmed=service.confirmOrder(context(SalesPermissions.ORDERS_CONFIRM),confirm).value().orElseThrow();
        assertEquals(SalesOrderState.CONFIRMED,confirmed.state()); assertEquals(ITEM.value(),inventory.lastRequest.catalogItemId());
        var cancelled=service.cancelOrder(context(SalesPermissions.ORDERS_CANCEL),new SalesCommands.CancelOrder(
                confirmed.reference().id(),1,"Cliente desistió","cancel-1")).value().orElseThrow();
        assertEquals(SalesOrderState.CANCELLED,cancelled.state()); assertEquals(1,inventory.releaseCalls);
    }

    @Test void reportsInventoryFailureWithoutConfirmingTheOrder(){
        var order=service.createOrder(context(SalesPermissions.ORDERS_CREATE),create("create-3")).value().orElseThrow();
        inventory.failReserve=true;
        var result=service.confirmOrder(context(SalesPermissions.ORDERS_CONFIRM),new SalesCommands.ConfirmOrder(
                order.reference().id(),0,List.of(new SalesCommands.ReservationInput(LINE,WAREHOUSE,LOCATION,
                Optional.empty(),Optional.empty(),Optional.empty(),StockCondition.AVAILABLE,
                Instant.parse("2026-08-22T12:00:00Z"))),"confirm-failure"));
        assertEquals(SalesResultCode.INVENTORY_FAILURE,result.code());
        assertEquals(SalesOrderState.DRAFT,order.state());
    }

    private static SalesCommands.CreateOrder create(String key){return new SalesCommands.CreateOrder("PED-1",CUSTOMER,"80000000-0",new CurrencyCode("PYG"),TERM,List.of(new SalesCommands.LineInput(LINE,ITEM,"EA",new BigDecimal("2"),PRICE_LIST,Optional.empty(),Optional.empty())),key);}
    private static SalesOperationContext context(ContributionId permission){return new SalesOperationContext(new AuthenticatedCompanyContext(new AuthenticatedActor(new AppUserId(uuid(99))),COMPANY),SalesIdentity.PLUGIN_ID,permission,"sales:test");}
    private static CurrencyReference currency(){return new CurrencyReference(new CurrencyCode("PYG"),"600",0,"Guaraní","2026-1",true);}
    private static UUID uuid(long value){return new UUID(0,value);}

    private static final class CountingIds implements SalesIdGenerator {int orderCalls;public SalesQuoteId nextQuoteId(){return new SalesQuoteId(uuid(20));}public SalesOrderId nextOrderId(){orderCalls++;return new SalesOrderId(uuid(21));}public UUID nextTermId(){return uuid(22);}public UUID nextTransitionId(){return uuid(23+orderCalls);}}
    private static final class MemoryOperations implements SalesOperationRepository {final Map<String,SalesOperationRecord> values=new HashMap<>();public Optional<SalesOperationRecord> find(CompanyId c,String k){return Optional.ofNullable(values.get(k)).filter(v->v.companyId().equals(c));}public void append(SalesOperationRecord v){values.put(v.idempotencyKey(),v);}}
    private static final class MemoryTerms implements SalesTermRepository {final Map<UUID,SalesTerm> values=new HashMap<>();public Optional<SalesTerm> findById(CompanyId c,UUID id){return Optional.ofNullable(values.get(id)).filter(v->v.snapshot().companyId().equals(c));}public Optional<SalesTerm> findByCode(CompanyId c,String code){return values.values().stream().filter(v->v.snapshot().code().equals(code)).findFirst();}public SalesTerm insert(SalesTerm v){values.put(v.snapshot().id(),v);return v;}public SalesTerm update(SalesTerm v,long x){return insert(v);}}
    private static final class MemoryOrders implements SalesOrderRepository {final Map<SalesOrderId,SalesOrder> values=new HashMap<>();public Optional<SalesOrder> findById(CompanyId c,SalesOrderId id){return Optional.ofNullable(values.get(id)).filter(v->v.snapshot().companyId().equals(c));}public Optional<SalesOrder> findByNumber(CompanyId c,String n){return values.values().stream().filter(v->v.reference().number().equals(n)).findFirst();}public SalesOrder insert(SalesOrder v){values.put(v.reference().id(),v);return v;}public SalesOrder update(SalesOrder v,long x){return insert(v);}}
    private static final class MemoryQuotes implements SalesQuoteRepository {final Map<SalesQuoteId,SalesQuote> values=new HashMap<>();public Optional<SalesQuote> findById(CompanyId c,SalesQuoteId id){return Optional.ofNullable(values.get(id));}public Optional<SalesQuote> findByNumber(CompanyId c,String n){return Optional.empty();}public SalesQuote insert(SalesQuote v){values.put(v.reference().id(),v);return v;}public SalesQuote update(SalesQuote v,long x){return insert(v);}}
    private static final class FakeInventory implements InventoryReservations {CatalogStockReservationRequest lastRequest;int releaseCalls;boolean failReserve;StockReservationReference value;
        public Optional<StockReservationReference> find(CompanyId c,StockReservationId id){return Optional.ofNullable(value);}
        public StockReservationReference reserve(CompanyId c,StockReservationRequest r){throw new UnsupportedOperationException();}
        public StockReservationReference reserveCatalogItem(CompanyId c,CatalogStockReservationRequest r){if(failReserve)throw new IllegalStateException("insufficient stock");lastRequest=r;value=reference(r);return value;}
        public StockReservationReference consume(CompanyId c,StockReservationId id,long v,BigDecimal q,String k){throw new UnsupportedOperationException();}
        public StockReservationReference release(CompanyId c,StockReservationId id,long v,BigDecimal q,String k){releaseCalls++;value=new StockReservationReference(value.id(),value.key(),value.originalQuantity(),BigDecimal.ZERO,q,BigDecimal.ZERO,StockReservationState.RELEASED,value.source(),value.expiresAt(),v+1);return value;}
        public StockReservationReference expire(CompanyId c,StockReservationId id,long v,String k){throw new UnsupportedOperationException();}
        private static StockReservationReference reference(CatalogStockReservationRequest r){return new StockReservationReference(new StockReservationId(uuid(30)),new StockKey(new InventoryItemId(uuid(31)),r.warehouseId(),r.locationId(),r.lotCode(),r.serialNumber(),r.expiryDate(),r.condition()),r.quantity(),BigDecimal.ZERO,BigDecimal.ZERO,r.quantity(),StockReservationState.ACTIVE,r.source(),r.expiresAt(),0);}
    }
}
