package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class CatalogItemUuidKey implements Serializable {
    public UUID companyId;
    public UUID catalogItemId;
    public UUID detailId;
    public CatalogItemUuidKey() { }
    CatalogItemUuidKey(UUID companyId, UUID catalogItemId, UUID detailId) {
        this.companyId = companyId; this.catalogItemId = catalogItemId; this.detailId = detailId;
    }
    @Override public boolean equals(Object other) { return this == other || other instanceof CatalogItemUuidKey that && Objects.equals(companyId, that.companyId) && Objects.equals(catalogItemId, that.catalogItemId) && Objects.equals(detailId, that.detailId); }
    @Override public int hashCode() { return Objects.hash(companyId, catalogItemId, detailId); }
}
