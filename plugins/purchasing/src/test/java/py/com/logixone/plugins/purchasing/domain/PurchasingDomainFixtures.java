package py.com.logixone.plugins.purchasing.domain;

import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.purchasing.api.PurchaseLineKind;
import py.com.logixone.plugins.referencedata.api.CurrencyCode;

final class PurchasingDomainFixtures {
    static final CompanyId COMPANY = new CompanyId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    static final AppUserId REQUESTER = new AppUserId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    static final AppUserId APPROVER = new AppUserId(UUID.fromString("00000000-0000-0000-0000-000000000003"));

    private PurchasingDomainFixtures() {
    }

    static SupplierSnapshot supplier() {
        return new SupplierSnapshot(
                new BusinessPartnerId(UUID.fromString("00000000-0000-0000-0000-000000000010")),
                "PROV-1", "Proveedor Uno", 4);
    }

    static PurchasedItemSnapshot stockItem() {
        return new PurchasedItemSnapshot(
                Optional.of(new CatalogItemId(UUID.fromString("00000000-0000-0000-0000-000000000020"))),
                Optional.of("ITEM-1"), "Artículo uno", "UN", PurchaseLineKind.STOCK, 7);
    }

    static PurchasedItemSnapshot service() {
        return new PurchasedItemSnapshot(
                Optional.empty(), Optional.empty(), "Servicio técnico", "UN",
                PurchaseLineKind.SERVICE, 0);
    }

    static CurrencySnapshot pyg() {
        return new CurrencySnapshot(new CurrencyCode("PYG"), 0, "Guaraní", "ISO-2026");
    }
}
