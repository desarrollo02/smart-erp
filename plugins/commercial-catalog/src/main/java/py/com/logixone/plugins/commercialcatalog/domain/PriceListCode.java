package py.com.logixone.plugins.commercialcatalog.domain;

public record PriceListCode(String value) {
    public PriceListCode { value = DomainValues.code(value, "price list code", 64); }
    @Override public String toString() { return value; }
}
