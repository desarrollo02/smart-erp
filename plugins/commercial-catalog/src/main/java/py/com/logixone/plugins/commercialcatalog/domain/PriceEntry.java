package py.com.logixone.plugins.commercialcatalog.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.PriceEntryId;

/** One immutable price condition; lifecycle changes preserve its identity and values. */
public record PriceEntry(
        PriceEntryId id,
        CatalogItemId itemId,
        UnitCode unit,
        BigDecimal minimumQuantity,
        BigDecimal amount,
        Instant validFrom,
        Optional<Instant> validUntil,
        boolean active) {

    public PriceEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(unit, "unit");
        minimumQuantity = DomainValues.positive(minimumQuantity, "minimumQuantity");
        amount = DomainValues.nonNegative(amount, "amount");
        Objects.requireNonNull(validFrom, "validFrom");
        validUntil = Objects.requireNonNull(validUntil, "validUntil");
        validUntil.ifPresent(end -> {
            if (!end.isAfter(validFrom)) {
                throw new IllegalArgumentException("validUntil must be after validFrom");
            }
        });
    }

    public static PriceEntry active(
            PriceEntryId id,
            CatalogItemId itemId,
            UnitCode unit,
            BigDecimal minimumQuantity,
            BigDecimal amount,
            Instant validFrom,
            Optional<Instant> validUntil) {
        return new PriceEntry(id, itemId, unit, minimumQuantity, amount, validFrom, validUntil, true);
    }

    public boolean appliesAt(Instant moment) {
        Objects.requireNonNull(moment, "moment");
        return active && !moment.isBefore(validFrom) && validUntil.map(end -> moment.isBefore(end)).orElse(true);
    }

    public boolean hasSameScope(PriceEntry other) {
        Objects.requireNonNull(other, "other");
        return itemId.equals(other.itemId)
                && unit.equals(other.unit)
                && minimumQuantity.compareTo(other.minimumQuantity) == 0;
    }

    public boolean overlaps(PriceEntry other) {
        Objects.requireNonNull(other, "other");
        boolean startsBeforeOtherEnds = other.validUntil.map(end -> validFrom.isBefore(end)).orElse(true);
        boolean otherStartsBeforeThisEnds = validUntil.map(end -> other.validFrom.isBefore(end)).orElse(true);
        return startsBeforeOtherEnds && otherStartsBeforeThisEnds;
    }

    public PriceEntry inactivate() {
        return active ? new PriceEntry(id, itemId, unit, minimumQuantity, amount, validFrom, validUntil, false) : this;
    }
}
