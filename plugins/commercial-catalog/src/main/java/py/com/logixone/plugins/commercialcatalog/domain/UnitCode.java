package py.com.logixone.plugins.commercialcatalog.domain;

/** Stable unit code interpreted inside the commercial catalog. */
public record UnitCode(String value) implements Comparable<UnitCode> {

    public UnitCode {
        value = DomainValues.code(value, "unit code", 16);
    }

    @Override
    public int compareTo(UnitCode other) {
        return value.compareTo(java.util.Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
