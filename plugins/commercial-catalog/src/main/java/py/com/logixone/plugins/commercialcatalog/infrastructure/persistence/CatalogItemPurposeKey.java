package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class CatalogItemPurposeKey implements Serializable {
    public UUID companyId;
    public UUID catalogItemId;
    public String unitCode;
    public String purposeCode;
    public CatalogItemPurposeKey() { }
    CatalogItemPurposeKey(UUID companyId, UUID catalogItemId, String unitCode, String purposeCode) {
        this.companyId = companyId; this.catalogItemId = catalogItemId; this.unitCode = unitCode; this.purposeCode = purposeCode;
    }
    @Override public boolean equals(Object other) { return this == other || other instanceof CatalogItemPurposeKey that && Objects.equals(companyId, that.companyId) && Objects.equals(catalogItemId, that.catalogItemId) && Objects.equals(unitCode, that.unitCode) && Objects.equals(purposeCode, that.purposeCode); }
    @Override public int hashCode() { return Objects.hash(companyId, catalogItemId, unitCode, purposeCode); }
}
