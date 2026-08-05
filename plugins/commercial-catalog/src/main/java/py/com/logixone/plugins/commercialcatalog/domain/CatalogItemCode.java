package py.com.logixone.plugins.commercialcatalog.domain;

/** Company-scoped mutable business code; it is not the technical identity. */
public record CatalogItemCode(String value) {

    public CatalogItemCode {
        value = DomainValues.code(value, "catalog item code", 64);
    }

    @Override
    public String toString() {
        return value;
    }
}
