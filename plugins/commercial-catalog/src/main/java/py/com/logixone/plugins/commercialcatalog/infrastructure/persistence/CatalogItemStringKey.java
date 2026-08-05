package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class CatalogItemStringKey implements Serializable {
    public UUID companyId;
    public UUID catalogItemId;
    public String detailCode;
    public CatalogItemStringKey() { }
    CatalogItemStringKey(UUID companyId, UUID catalogItemId, String detailCode) {
        this.companyId = companyId; this.catalogItemId = catalogItemId; this.detailCode = detailCode;
    }
    @Override public boolean equals(Object other) { return this == other || other instanceof CatalogItemStringKey that && Objects.equals(companyId, that.companyId) && Objects.equals(catalogItemId, that.catalogItemId) && Objects.equals(detailCode, that.detailCode); }
    @Override public int hashCode() { return Objects.hash(companyId, catalogItemId, detailCode); }
}
