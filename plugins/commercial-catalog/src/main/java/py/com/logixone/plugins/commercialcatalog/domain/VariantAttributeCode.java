package py.com.logixone.plugins.commercialcatalog.domain;

public record VariantAttributeCode(String value) implements Comparable<VariantAttributeCode> {
    public VariantAttributeCode { value = DomainValues.code(value, "variant attribute code", 32); }
    @Override public int compareTo(VariantAttributeCode other) { return value.compareTo(java.util.Objects.requireNonNull(other, "other").value); }
}
