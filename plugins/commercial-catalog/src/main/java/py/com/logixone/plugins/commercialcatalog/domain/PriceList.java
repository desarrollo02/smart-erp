package py.com.logixone.plugins.commercialcatalog.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogPriceQuote;
import py.com.logixone.plugins.commercialcatalog.api.CatalogPriceQuoteRequest;
import py.com.logixone.plugins.commercialcatalog.api.CatalogTaxMode;
import py.com.logixone.plugins.commercialcatalog.api.PriceEntryId;
import py.com.logixone.plugins.commercialcatalog.api.PriceListId;

/** Company-owned price list with fixed currency, tax interpretation and rounding. */
public final class PriceList {
    private final CompanyId companyId;
    private final PriceListId id;
    private final PriceListCode code;
    private PriceListName name;
    private final String currency;
    private final CatalogTaxMode taxMode;
    private final int scale;
    private final RoundingMode roundingMode;
    private PriceListState state;
    private final Map<PriceEntryId, PriceEntry> entries = new LinkedHashMap<>();
    private long version;

    private PriceList(
            CompanyId companyId,
            PriceListId id,
            PriceListCode code,
            PriceListName name,
            String currency,
            CatalogTaxMode taxMode,
            int scale,
            RoundingMode roundingMode) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.id = Objects.requireNonNull(id, "id");
        this.code = Objects.requireNonNull(code, "code");
        this.name = Objects.requireNonNull(name, "name");
        this.currency = DomainValues.currency(currency);
        this.taxMode = Objects.requireNonNull(taxMode, "taxMode");
        if (scale < 0 || scale > 6) {
            throw new IllegalArgumentException("Price list scale must be between 0 and 6");
        }
        this.scale = scale;
        this.roundingMode = Objects.requireNonNull(roundingMode, "roundingMode");
        if (roundingMode == RoundingMode.UNNECESSARY) {
            throw new IllegalArgumentException("Price list rounding mode must resolve non-exact values");
        }
        this.state = PriceListState.ACTIVE;
    }

    public static PriceList create(
            CompanyId companyId,
            PriceListId id,
            PriceListCode code,
            PriceListName name,
            String currency,
            CatalogTaxMode taxMode,
            int scale,
            RoundingMode roundingMode) {
        return new PriceList(companyId, id, code, name, currency, taxMode, scale, roundingMode);
    }

    public static PriceList restore(PriceListSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        PriceList list = new PriceList(
                snapshot.companyId(),
                snapshot.id(),
                snapshot.code(),
                snapshot.name(),
                snapshot.currency(),
                snapshot.taxMode(),
                snapshot.scale(),
                snapshot.roundingMode());
        snapshot.entries().forEach(entry -> {
            if (list.entries.putIfAbsent(entry.id(), entry) != null) {
                throw new IllegalArgumentException("Price entry id is duplicated in snapshot");
            }
            boolean ambiguous = list.entries.values().stream()
                    .filter(existing -> !existing.id().equals(entry.id()))
                    .filter(PriceEntry::active)
                    .anyMatch(existing -> entry.active()
                            && existing.hasSameScope(entry)
                            && existing.overlaps(entry));
            if (ambiguous) {
                throw new IllegalArgumentException("Snapshot contains overlapping active price entries");
            }
        });
        list.state = snapshot.state();
        list.version = snapshot.version();
        return list;
    }

    public void rename(PriceListName name, long expectedVersion) {
        verifyVersion(expectedVersion);
        requireActive();
        this.name = Objects.requireNonNull(name, "name");
        version++;
    }

    public void addEntry(PriceEntry entry, long expectedVersion) {
        verifyVersion(expectedVersion);
        requireActive();
        Objects.requireNonNull(entry, "entry");
        if (!entry.active()) {
            throw new IllegalArgumentException("A new price entry must be active");
        }
        if (entries.containsKey(entry.id())) {
            throw new IllegalArgumentException("Price entry id already exists in this list");
        }
        boolean ambiguous = entries.values().stream()
                .filter(PriceEntry::active)
                .anyMatch(existing -> existing.hasSameScope(entry) && existing.overlaps(entry));
        if (ambiguous) {
            throw new IllegalArgumentException("Active price-entry validity overlaps the same scope");
        }
        entries.put(entry.id(), entry);
        version++;
    }

    public void inactivateEntry(PriceEntryId entryId, long expectedVersion) {
        verifyVersion(expectedVersion);
        PriceEntry entry = entries.get(Objects.requireNonNull(entryId, "entryId"));
        if (entry == null) {
            throw new IllegalArgumentException("Price entry does not belong to this list");
        }
        if (entry.active()) {
            entries.put(entryId, entry.inactivate());
            version++;
        }
    }

    public Optional<CatalogPriceQuote> quote(CatalogPriceQuoteRequest request) {
        Objects.requireNonNull(request, "request");
        if (!id.equals(request.priceListId())) {
            throw new IllegalArgumentException("Quote request belongs to another price list");
        }
        if (state != PriceListState.ACTIVE) {
            return Optional.empty();
        }
        PriceEntry selected = entries.values().stream()
                .filter(entry -> entry.itemId().equals(request.itemId()))
                .filter(entry -> entry.unit().value().equals(request.unitCode()))
                .filter(entry -> entry.appliesAt(request.effectiveAt()))
                .filter(entry -> entry.minimumQuantity().compareTo(request.quantity()) <= 0)
                .max(Comparator.comparing(PriceEntry::minimumQuantity)
                        .thenComparing(PriceEntry::validFrom))
                .orElse(null);
        if (selected == null) {
            return Optional.empty();
        }
        BigDecimal unitAmount = selected.amount().setScale(scale, roundingMode);
        BigDecimal total = unitAmount.multiply(request.quantity()).setScale(scale, roundingMode);
        return Optional.of(new CatalogPriceQuote(
                id,
                selected.id(),
                selected.itemId(),
                currency,
                taxMode,
                selected.unit().value(),
                request.quantity(),
                unitAmount,
                total,
                selected.validFrom(),
                selected.validUntil(),
                version));
    }

    public void inactivate(long expectedVersion) {
        verifyVersion(expectedVersion);
        if (state == PriceListState.ACTIVE) {
            state = PriceListState.INACTIVE;
            version++;
        }
    }

    public void reactivate(long expectedVersion) {
        verifyVersion(expectedVersion);
        if (state == PriceListState.INACTIVE) {
            state = PriceListState.ACTIVE;
            version++;
        }
    }

    public PriceListSnapshot snapshot() {
        return new PriceListSnapshot(
                companyId,
                id,
                code,
                name,
                currency,
                taxMode,
                scale,
                roundingMode,
                state,
                entries.values().stream().toList(),
                version);
    }

    private void verifyVersion(long expectedVersion) {
        if (expectedVersion != version) {
            throw new ConcurrentCatalogChangeException(expectedVersion, version);
        }
    }

    private void requireActive() {
        if (state != PriceListState.ACTIVE) {
            throw new IllegalStateException("Inactive price list cannot be modified");
        }
    }

    public CompanyId companyId() { return companyId; }
    public PriceListId id() { return id; }
    public PriceListCode code() { return code; }
    public PriceListName name() { return name; }
    public String currency() { return currency; }
    public CatalogTaxMode taxMode() { return taxMode; }
    public int scale() { return scale; }
    public RoundingMode roundingMode() { return roundingMode; }
    public PriceListState state() { return state; }
    public Map<PriceEntryId, PriceEntry> entries() { return Map.copyOf(entries); }
    public long version() { return version; }
}
