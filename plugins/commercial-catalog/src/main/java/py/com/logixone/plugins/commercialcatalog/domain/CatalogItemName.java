package py.com.logixone.plugins.commercialcatalog.domain;

/** Normalized display name of a commercial item. */
public record CatalogItemName(String value) {

    public CatalogItemName {
        value = DomainValues.text(value, "catalog item name", 200);
    }

    @Override
    public String toString() {
        return value;
    }
}
