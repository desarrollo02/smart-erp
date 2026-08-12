package py.com.logixone.plugins.purchasing.application;

import java.math.BigDecimal;
import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerDirectory;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerRole;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemDirectory;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemScope;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemType;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversionRequest;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversions;
import py.com.logixone.plugins.purchasing.api.PurchaseLineKind;
import py.com.logixone.plugins.purchasing.application.command.PurchasingCommands.ItemInput;
import py.com.logixone.plugins.purchasing.domain.CurrencySnapshot;
import py.com.logixone.plugins.purchasing.domain.PurchasedItemSnapshot;
import py.com.logixone.plugins.purchasing.domain.SupplierSnapshot;
import py.com.logixone.plugins.referencedata.api.CurrencyCode;
import py.com.logixone.plugins.referencedata.api.ReferenceDataDirectory;

final class PurchasingReferenceResolver {
    private final BusinessPartnerDirectory partners;
    private final CatalogItemDirectory catalog;
    private final CatalogUnitConversions conversions;
    private final ReferenceDataDirectory referenceData;

    PurchasingReferenceResolver(
            BusinessPartnerDirectory partners,
            CatalogItemDirectory catalog,
            CatalogUnitConversions conversions,
            ReferenceDataDirectory referenceData) {
        this.partners = Objects.requireNonNull(partners, "partners");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.conversions = Objects.requireNonNull(conversions, "conversions");
        this.referenceData = Objects.requireNonNull(referenceData, "referenceData");
    }

    PurchasedItemSnapshot item(CompanyId companyId, ItemInput input) {
        Objects.requireNonNull(input, "input");
        if (input.catalogItemId().isEmpty()) {
            if (input.kind() == PurchaseLineKind.STOCK) {
                throw new ReferenceFailure();
            }
            return new PurchasedItemSnapshot(
                    input.catalogItemId(), java.util.Optional.empty(), input.description(),
                    input.presentedUnitCode(), input.presentedUnitCode(), BigDecimal.ONE,
                    input.kind(), 0);
        }
        var item = catalog.findById(companyId, input.catalogItemId().orElseThrow())
                .orElseThrow(ReferenceFailure::new);
        if (item.state() != CatalogItemState.ACTIVE
                || !item.scopes().contains(CatalogItemScope.PURCHASE)
                || (input.kind() == PurchaseLineKind.STOCK
                    && item.type() != CatalogItemType.PRODUCT)
                || (input.kind() == PurchaseLineKind.SERVICE
                    && item.type() != CatalogItemType.SERVICE)) {
            throw new ReferenceFailure();
        }
        var conversion = conversions.convert(companyId, new CatalogUnitConversionRequest(
                        item.id(), input.presentedUnitCode(), item.baseUnitCode(), BigDecimal.ONE))
                .orElseThrow(ReferenceFailure::new);
        if (!conversion.itemId().equals(item.id())
                || !conversion.targetUnitCode().equals(item.baseUnitCode())
                || conversion.itemVersion() != item.version()) {
            throw new ReferenceFailure();
        }
        return new PurchasedItemSnapshot(
                java.util.Optional.of(item.id()), java.util.Optional.of(item.code()),
                item.displayName(), conversion.sourceUnitCode(), conversion.targetUnitCode(),
                conversion.factor(), input.kind(), item.version());
    }

    SupplierSnapshot supplier(CompanyId companyId, BusinessPartnerId supplierId) {
        var partner = partners.findById(companyId, supplierId)
                .orElseThrow(ReferenceFailure::new);
        if (partner.state() != BusinessPartnerState.ACTIVE
                || !partner.roles().contains(BusinessPartnerRole.SUPPLIER)) {
            throw new ReferenceFailure();
        }
        return new SupplierSnapshot(
                partner.id(), partner.code(), partner.displayName(), partner.version());
    }

    CurrencySnapshot currency(CompanyId companyId, CurrencyCode code) {
        var currency = referenceData.findCurrency(companyId, code)
                .orElseThrow(ReferenceFailure::new);
        if (!currency.enabled() || currency.minorUnitIfDefined().isEmpty()) {
            throw new ReferenceFailure();
        }
        return new CurrencySnapshot(currency.code(), currency.minorUnit(),
                currency.displayName(), currency.releaseId());
    }

    static final class ReferenceFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
