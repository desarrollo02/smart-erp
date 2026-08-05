package py.com.logixone.plugins.commercialcatalog.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

/** Item-specific factor that converts one alternative unit into the base unit. */
public record ItemUnitConversion(
        UnitCode unit,
        BigDecimal toBaseFactor,
        Set<UnitPurpose> purposes,
        Set<UnitPurpose> defaultFor,
        boolean active) {

    public ItemUnitConversion {
        Objects.requireNonNull(unit, "unit");
        toBaseFactor = DomainValues.positive(toBaseFactor, "toBaseFactor");
        purposes = Set.copyOf(Objects.requireNonNull(purposes, "purposes"));
        defaultFor = Set.copyOf(Objects.requireNonNull(defaultFor, "defaultFor"));
        if (purposes.isEmpty()) {
            throw new IllegalArgumentException("purposes must not be empty");
        }
        if (!purposes.containsAll(defaultFor)) {
            throw new IllegalArgumentException("A default unit must support the same purpose");
        }
    }

    public ItemUnitConversion inactivate() {
        return active ? new ItemUnitConversion(unit, toBaseFactor, purposes, Set.of(), false) : this;
    }
}
