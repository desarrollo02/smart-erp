package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class PriceEntryKey implements Serializable {
    public UUID companyId;
    public UUID priceListId;
    public UUID priceEntryId;
    public PriceEntryKey() { }
    PriceEntryKey(UUID companyId, UUID priceListId, UUID priceEntryId) {
        this.companyId = companyId; this.priceListId = priceListId; this.priceEntryId = priceEntryId;
    }
    @Override public boolean equals(Object other) { return this == other || other instanceof PriceEntryKey that && Objects.equals(companyId, that.companyId) && Objects.equals(priceListId, that.priceListId) && Objects.equals(priceEntryId, that.priceEntryId); }
    @Override public int hashCode() { return Objects.hash(companyId, priceListId, priceEntryId); }
}
