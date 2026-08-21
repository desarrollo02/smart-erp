package py.com.logixone.plugins.sales.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.plugins.sales.api.SalesQuoteId;
import py.com.logixone.plugins.sales.api.SalesQuoteReference;
import py.com.logixone.plugins.sales.api.SalesQuoteState;

public final class SalesQuote {
    private final CompanyId companyId; private final SalesQuoteId id; private final String number;
    private final CustomerSnapshot customer; private final CurrencySnapshot currency; private final PaymentTermSnapshot term;
    private final LocalDate validUntil; private final List<SalesLineSnapshot> lines;
    private SalesQuoteState state = SalesQuoteState.DRAFT; private long version;
    private Optional<Instant> issuedAt = Optional.empty(); private Optional<AppUserId> transitionActor = Optional.empty();

    private SalesQuote(CompanyId companyId, SalesQuoteId id, String number, CustomerSnapshot customer, CurrencySnapshot currency,
            PaymentTermSnapshot term, LocalDate validUntil, List<SalesLineSnapshot> lines) {
        this.companyId=Objects.requireNonNull(companyId); this.id=Objects.requireNonNull(id); this.number=SalesValues.text(number,"number",64);
        this.customer=Objects.requireNonNull(customer); this.currency=Objects.requireNonNull(currency); this.term=Objects.requireNonNull(term);
        this.validUntil=Objects.requireNonNull(validUntil); this.lines=validate(lines);
    }
    public static SalesQuote draft(CompanyId companyId, SalesQuoteId id, String number, CustomerSnapshot customer,
            CurrencySnapshot currency, PaymentTermSnapshot term, LocalDate validUntil, List<SalesLineSnapshot> lines) {
        return new SalesQuote(companyId,id,number,customer,currency,term,validUntil,lines);
    }
    public static SalesQuote restore(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        SalesQuote quote = new SalesQuote(snapshot.companyId(), snapshot.id(), snapshot.number(),
                snapshot.customer(), snapshot.currency(), snapshot.term(), snapshot.validUntil(), snapshot.lines());
        quote.state = Objects.requireNonNull(snapshot.state(), "state");
        quote.issuedAt = Objects.requireNonNull(snapshot.issuedAt(), "issuedAt");
        quote.transitionActor = Objects.requireNonNull(snapshot.transitionActor(), "transitionActor");
        if (snapshot.version() < 0 || (quote.state == SalesQuoteState.DRAFT && quote.issuedAt.isPresent())
                || (quote.state != SalesQuoteState.DRAFT && quote.issuedAt.isEmpty())) {
            throw new IllegalArgumentException("Invalid quote snapshot");
        }
        quote.version = snapshot.version();
        return quote;
    }
    public void issue(AppUserId actor, Instant at, long expectedVersion) { change(expectedVersion, SalesQuoteState.DRAFT); state=SalesQuoteState.ISSUED; transitionActor=Optional.of(actor); issuedAt=Optional.of(at); version++; }
    public void accept(AppUserId actor, Instant at, long expectedVersion) { change(expectedVersion, SalesQuoteState.ISSUED); if (at.isAfter(validUntil.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant())) throw new IllegalStateException("Expired quote cannot be accepted"); state=SalesQuoteState.ACCEPTED; transitionActor=Optional.of(actor); version++; }
    public void reject(AppUserId actor, Instant at, long expectedVersion) { change(expectedVersion, SalesQuoteState.ISSUED); Objects.requireNonNull(at); state=SalesQuoteState.REJECTED; transitionActor=Optional.of(actor); version++; }
    public void expire(AppUserId actor, Instant at, long expectedVersion) { change(expectedVersion, SalesQuoteState.ISSUED); Objects.requireNonNull(actor); if (at.isBefore(validUntil.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant())) throw new IllegalStateException("Quote is still valid"); state=SalesQuoteState.EXPIRED; transitionActor=Optional.of(actor); version++; }
    public void cancel(AppUserId actor, long expectedVersion) { change(expectedVersion, state); if (state!=SalesQuoteState.DRAFT && state!=SalesQuoteState.ISSUED) throw new IllegalStateException("Quote cannot be cancelled"); state=SalesQuoteState.CANCELLED; transitionActor=Optional.of(actor); version++; }
    private void change(long expected, SalesQuoteState required) { if(expected!=version) throw new ConcurrentSalesChangeException(expected,version); if(state!=required) throw new IllegalStateException("Quote must be "+required); }
    private static List<SalesLineSnapshot> validate(List<SalesLineSnapshot> values) { values=List.copyOf(Objects.requireNonNull(values)); if(values.isEmpty()) throw new IllegalArgumentException("Quote requires lines"); var ids=new HashSet<>(); if(values.stream().anyMatch(v->!ids.add(v.id()))) throw new IllegalArgumentException("Duplicate line id"); return values; }
    public SalesQuoteReference reference() { return new SalesQuoteReference(id,number,customer.id().toString(),state,currency.code().value(),lines.stream().map(SalesLineSnapshot::total).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add),version); }
    public Snapshot snapshot() { return new Snapshot(companyId,id,number,customer,currency,term,validUntil,lines,state,issuedAt,transitionActor,version); }
    public SalesQuoteState state(){return state;} public long version(){return version;} public List<SalesLineSnapshot> lines(){return lines;} public CompanyId companyId(){return companyId;} public PaymentTermSnapshot term(){return term;}
    public record Snapshot(CompanyId companyId, SalesQuoteId id, String number,
            CustomerSnapshot customer, CurrencySnapshot currency, PaymentTermSnapshot term,
            LocalDate validUntil, List<SalesLineSnapshot> lines, SalesQuoteState state,
            Optional<Instant> issuedAt, Optional<AppUserId> transitionActor, long version) {
        public Snapshot { lines = List.copyOf(Objects.requireNonNull(lines, "lines")); }
    }
}
