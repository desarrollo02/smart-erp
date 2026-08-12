package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import java.math.BigDecimal;
import java.util.UUID;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.purchasing.api.PurchaseLineKind;
import py.com.logixone.plugins.purchasing.domain.PurchasedItemSnapshot;

@Embeddable
public class PurchasedItemEmbeddable {
    @Column(name = "catalog_item_id")
    private UUID catalogItemId;
    @Column(name = "catalog_code_snapshot", length = 64)
    private String catalogCode;
    @Column(name = "item_description_snapshot", nullable = false, length = 240)
    private String description;
    @Column(name = "presented_unit_code_snapshot", nullable = false, length = 16)
    private String presentedUnitCode;
    @Column(name = "base_unit_code_snapshot", nullable = false, length = 16)
    private String baseUnitCode;
    @Column(name = "conversion_factor", nullable = false, precision = 30, scale = 12)
    private BigDecimal conversionFactor;
    @Column(name = "line_kind", nullable = false, length = 24)
    private String kind;
    @Column(name = "catalog_source_version", nullable = false)
    private long sourceVersion;

    protected PurchasedItemEmbeddable() {
    }

    static PurchasedItemEmbeddable from(PurchasedItemSnapshot snapshot) {
        PurchasedItemEmbeddable value = new PurchasedItemEmbeddable();
        value.catalogItemId = snapshot.catalogItemId().map(CatalogItemId::value).orElse(null);
        value.catalogCode = snapshot.catalogCode().orElse(null);
        value.description = snapshot.description();
        value.presentedUnitCode = snapshot.presentedUnitCode();
        value.baseUnitCode = snapshot.baseUnitCode();
        value.conversionFactor = snapshot.conversionFactor();
        value.kind = snapshot.kind().name();
        value.sourceVersion = snapshot.sourceVersion();
        return value;
    }

    PurchasedItemSnapshot snapshot() {
        return new PurchasedItemSnapshot(
                Optional.ofNullable(catalogItemId).map(CatalogItemId::new),
                Optional.ofNullable(catalogCode), description, presentedUnitCode,
                baseUnitCode, conversionFactor,
                PurchaseLineKind.valueOf(kind), sourceVersion);
    }
}
