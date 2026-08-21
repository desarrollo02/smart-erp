package py.com.logixone.plugins.sales.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.sales.api.SalesOrderId;
import py.com.logixone.plugins.sales.api.SalesOrderReference;
import py.com.logixone.plugins.sales.api.SalesOrderState;
import py.com.logixone.plugins.sales.api.SalesQuoteId;

public final class SalesOrder {
    private final CompanyId companyId; private final SalesOrderId id; private final String number; private final CustomerSnapshot customer;
    private final CurrencySnapshot currency; private final PaymentTermSnapshot term; private final Optional<SalesQuoteId> sourceQuoteId;
    private final List<SalesLineSnapshot> lines; private Map<UUID,StockReservationId> reservations=Map.of();
    private SalesOrderState state=SalesOrderState.DRAFT; private long version;
    private SalesOrder(CompanyId companyId, SalesOrderId id, String number, CustomerSnapshot customer, CurrencySnapshot currency,
            PaymentTermSnapshot term, Optional<SalesQuoteId> sourceQuoteId, List<SalesLineSnapshot> lines) {
        this.companyId=Objects.requireNonNull(companyId); this.id=Objects.requireNonNull(id); this.number=SalesValues.text(number,"number",64);
        this.customer=Objects.requireNonNull(customer); this.currency=Objects.requireNonNull(currency); this.term=Objects.requireNonNull(term);
        this.sourceQuoteId=Objects.requireNonNull(sourceQuoteId); this.lines=List.copyOf(Objects.requireNonNull(lines));
        if(this.lines.isEmpty()) throw new IllegalArgumentException("Order requires lines"); var ids=new HashSet<>(); if(this.lines.stream().anyMatch(v->!ids.add(v.id()))) throw new IllegalArgumentException("Duplicate line id");
    }
    public static SalesOrder direct(CompanyId c, SalesOrderId id, String n, CustomerSnapshot customer, CurrencySnapshot currency, PaymentTermSnapshot term, List<SalesLineSnapshot> lines){return new SalesOrder(c,id,n,customer,currency,term,Optional.empty(),lines);}
    public static SalesOrder fromQuote(CompanyId c, SalesOrderId id, String n, SalesQuote quote, CustomerSnapshot customer, CurrencySnapshot currency, PaymentTermSnapshot term){ if(quote.state()!=py.com.logixone.plugins.sales.api.SalesQuoteState.ACCEPTED) throw new IllegalStateException("Source quote must be accepted"); return new SalesOrder(c,id,n,customer,currency,term,Optional.of(quote.reference().id()),quote.lines()); }
    public static SalesOrder restore(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        SalesOrder order = new SalesOrder(snapshot.companyId(), snapshot.id(), snapshot.number(),
                snapshot.customer(), snapshot.currency(), snapshot.term(), snapshot.sourceQuoteId(), snapshot.lines());
        order.state = Objects.requireNonNull(snapshot.state(), "state");
        order.reservations = Map.copyOf(Objects.requireNonNull(snapshot.reservations(), "reservations"));
        var required = order.lines.stream().filter(SalesLineSnapshot::stockManaged)
                .map(SalesLineSnapshot::id).collect(java.util.stream.Collectors.toSet());
        if (snapshot.version() < 0 || (order.state == SalesOrderState.DRAFT && !order.reservations.isEmpty())
                || ((order.state == SalesOrderState.CONFIRMED || order.state == SalesOrderState.CLOSED)
                    && !order.reservations.keySet().equals(required))
                || !required.containsAll(order.reservations.keySet())) {
            throw new IllegalArgumentException("Invalid order snapshot");
        }
        order.version = snapshot.version();
        return order;
    }
    public void confirm(Map<UUID,StockReservationId> provided, AppUserId actor, Instant at, long expectedVersion){ verify(expectedVersion); if(state!=SalesOrderState.DRAFT) throw new IllegalStateException("Order must be DRAFT"); Objects.requireNonNull(actor); Objects.requireNonNull(at); provided=Map.copyOf(Objects.requireNonNull(provided)); var required=lines.stream().filter(SalesLineSnapshot::stockManaged).map(SalesLineSnapshot::id).collect(java.util.stream.Collectors.toSet()); if(!provided.keySet().equals(required)) throw new IllegalArgumentException("Reservations must match stock-managed lines exactly"); reservations=provided; state=SalesOrderState.CONFIRMED; version++; }
    public List<StockReservationId> cancel(AppUserId actor, Instant at, long expectedVersion){ verify(expectedVersion); if(state!=SalesOrderState.DRAFT && state!=SalesOrderState.CONFIRMED) throw new IllegalStateException("Order cannot be cancelled"); Objects.requireNonNull(actor); Objects.requireNonNull(at); state=SalesOrderState.CANCELLED; version++; return List.copyOf(reservations.values()); }
    public void close(AppUserId actor, Instant at, long expectedVersion){ verify(expectedVersion); if(state!=SalesOrderState.CONFIRMED) throw new IllegalStateException("Order must be CONFIRMED"); Objects.requireNonNull(actor); Objects.requireNonNull(at); state=SalesOrderState.CLOSED; version++; }
    private void verify(long expected){if(expected!=version) throw new ConcurrentSalesChangeException(expected,version);}
    public SalesOrderReference reference(){return new SalesOrderReference(id,number,customer.id().toString(),state,currency.code().value(),lines.stream().map(SalesLineSnapshot::total).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add),sourceQuoteId,version);}
    public Snapshot snapshot(){return new Snapshot(companyId,id,number,customer,currency,term,sourceQuoteId,lines,state,reservations,version);}
    public SalesOrderState state(){return state;} public long version(){return version;} public Map<UUID,StockReservationId> reservations(){return reservations;}
    public record Snapshot(CompanyId companyId, SalesOrderId id, String number,
            CustomerSnapshot customer, CurrencySnapshot currency, PaymentTermSnapshot term,
            Optional<SalesQuoteId> sourceQuoteId, List<SalesLineSnapshot> lines,
            SalesOrderState state, Map<UUID,StockReservationId> reservations, long version) {
        public Snapshot { lines=List.copyOf(Objects.requireNonNull(lines,"lines")); reservations=Map.copyOf(Objects.requireNonNull(reservations,"reservations")); }
    }
}
