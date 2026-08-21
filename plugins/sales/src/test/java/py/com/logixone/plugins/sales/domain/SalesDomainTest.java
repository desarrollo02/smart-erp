package py.com.logixone.plugins.sales.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.referencedata.api.CurrencyCode;
import py.com.logixone.plugins.sales.api.*;

class SalesDomainTest {
    private static final UUID LINE=UUID.fromString("00000000-0000-0000-0000-000000000010");
    @Test void quoteNeverReservesAndConfirmedOrderRequiresExactStockReservations(){
        var quote=SalesQuote.draft(company(),new SalesQuoteId(UUID.randomUUID()),"COT-1",customer(),currency(),term(),LocalDate.of(2026,8,31),List.of(line(true)));
        quote.issue(user(),Instant.parse("2026-08-20T10:00:00Z"),0); quote.accept(user(),Instant.parse("2026-08-21T10:00:00Z"),1);
        var order=SalesOrder.fromQuote(company(),new SalesOrderId(UUID.randomUUID()),"PED-1",quote,customer(),currency(),term());
        assertTrue(order.reservations().isEmpty()); assertThrows(IllegalArgumentException.class,()->order.confirm(Map.of(),user(),Instant.now(),0));
        var reservation=new StockReservationId(UUID.randomUUID()); order.confirm(Map.of(LINE,reservation),user(),Instant.now(),0);
        assertEquals(SalesOrderState.CONFIRMED,order.state()); assertEquals(List.of(reservation),order.cancel(user(),Instant.now(),1));
        assertEquals(order.snapshot(), SalesOrder.restore(order.snapshot()).snapshot());
        assertEquals(quote.snapshot(), SalesQuote.restore(quote.snapshot()).snapshot());
    }
    @Test void manualPriceRequiresReasonAndConcurrencyIsChecked(){
        assertThrows(IllegalArgumentException.class,()->line(true,true,Optional.empty()));
        var order=SalesOrder.direct(company(),new SalesOrderId(UUID.randomUUID()),"PED-2",customer(),currency(),term(),List.of(line(false)));
        assertThrows(ConcurrentSalesChangeException.class,()->order.confirm(Map.of(),user(),Instant.now(),9));
        order.confirm(Map.of(),user(),Instant.now(),0); assertEquals(SalesOrderState.CONFIRMED,order.state());
    }
    @Test void commercialTermsAreInactivatedWithoutDeletingHistory(){var value=SalesTerm.active(company(),UUID.randomUUID(),"30D","Treinta días",30); var historical=value.documentSnapshot(); value.deactivate(0); assertEquals(30,historical.dueDays()); assertFalse(value.snapshot().active()); assertEquals(value.snapshot(),SalesTerm.restore(value.snapshot()).snapshot());}
    @Test void explicitTerminalTransitionsDoNotInventFiscalFulfillment(){
        var rejected=SalesQuote.draft(company(),new SalesQuoteId(UUID.randomUUID()),"COT-2",customer(),currency(),term(),LocalDate.of(2026,8,31),List.of(line(false)));
        rejected.issue(user(),Instant.parse("2026-08-20T10:00:00Z"),0);
        rejected.reject(user(),Instant.parse("2026-08-21T10:00:00Z"),1);
        assertEquals(SalesQuoteState.REJECTED,rejected.state());
        var expired=SalesQuote.draft(company(),new SalesQuoteId(UUID.randomUUID()),"COT-3",customer(),currency(),term(),LocalDate.of(2026,8,20),List.of(line(false)));
        expired.issue(user(),Instant.parse("2026-08-20T10:00:00Z"),0);
        expired.expire(user(),Instant.parse("2026-08-21T00:00:00Z"),1);
        assertEquals(SalesQuoteState.EXPIRED,expired.state());
        var order=SalesOrder.direct(company(),new SalesOrderId(UUID.randomUUID()),"PED-3",customer(),currency(),term(),List.of(line(false)));
        order.confirm(Map.of(),user(),Instant.now(),0); order.close(user(),Instant.now(),1);
        assertEquals(SalesOrderState.CLOSED,order.state());
    }
    private static SalesLineSnapshot line(boolean stock){return line(stock,false,Optional.empty());}
    private static SalesLineSnapshot line(boolean stock,boolean manual,Optional<String> reason){return new SalesLineSnapshot(LINE,new CatalogItemId(UUID.fromString("00000000-0000-0000-0000-000000000020")),"ITEM-1","Artículo","UN",stock,new BigDecimal("2"),new BigDecimal("100"),"IVA10",Optional.of("LP-1"),manual,reason,1);}
    private static CompanyId company(){return new CompanyId(UUID.fromString("00000000-0000-0000-0000-000000000001"));}
    private static AppUserId user(){return new AppUserId(UUID.fromString("00000000-0000-0000-0000-000000000002"));}
    private static CustomerSnapshot customer(){return new CustomerSnapshot(new BusinessPartnerId(UUID.fromString("00000000-0000-0000-0000-000000000003")),"CLI-1","Cliente","80000000-0",1);}
    private static CurrencySnapshot currency(){return new CurrencySnapshot(new CurrencyCode("PYG"),0,"Guaraní","2026-1");}
    private static PaymentTermSnapshot term(){return new PaymentTermSnapshot(UUID.randomUUID(),"CONTADO","Contado",0,1);}
}
