package py.com.logixone.plugins.commercialcatalog.domain;

public record PriceListName(String value) {
    public PriceListName { value = DomainValues.text(value, "price list name", 200); }
    @Override public String toString() { return value; }
}
