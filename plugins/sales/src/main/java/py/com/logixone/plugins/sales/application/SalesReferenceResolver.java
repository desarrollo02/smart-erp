package py.com.logixone.plugins.sales.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.businesspartners.api.*;
import py.com.logixone.plugins.commercialcatalog.api.*;
import py.com.logixone.plugins.referencedata.api.*;
import py.com.logixone.plugins.sales.application.command.SalesCommands;
import py.com.logixone.plugins.sales.application.port.SalesTermRepository;
import py.com.logixone.plugins.sales.domain.*;

final class SalesReferenceResolver {
    private final BusinessPartnerDirectory partners; private final CatalogItemDirectory catalog;
    private final CatalogPricing pricing; private final ReferenceDataDirectory referenceData;
    private final SalesTermRepository terms; private final Clock clock;
    SalesReferenceResolver(BusinessPartnerDirectory partners, CatalogItemDirectory catalog,
            CatalogPricing pricing, ReferenceDataDirectory referenceData,
            SalesTermRepository terms, Clock clock) {
        this.partners=partners; this.catalog=catalog; this.pricing=pricing;
        this.referenceData=referenceData; this.terms=terms; this.clock=clock;
    }
    CustomerSnapshot customer(CompanyId company, BusinessPartnerId id, String taxId) {
        var value=partners.findById(company,id).orElseThrow(ReferenceFailure::new);
        if(value.state()!=BusinessPartnerState.ACTIVE || !value.roles().contains(BusinessPartnerRole.CLIENT)) throw new ReferenceFailure();
        return new CustomerSnapshot(value.id(),value.code(),value.displayName(),taxId,value.version());
    }
    CurrencySnapshot currency(CompanyId company, CurrencyCode code) {
        var value=referenceData.findCurrency(company,code).orElseThrow(ReferenceFailure::new);
        if(!value.enabled() || value.minorUnitIfDefined().isEmpty()) throw new ReferenceFailure();
        return new CurrencySnapshot(value.code(),value.minorUnit(),value.displayName(),value.releaseId());
    }
    PaymentTermSnapshot term(CompanyId company, java.util.UUID id) {
        var value=terms.findById(company,id).orElseThrow(ReferenceFailure::new).snapshot();
        if(!value.active()) throw new ReferenceFailure();
        return new PaymentTermSnapshot(value.id(),value.code(),value.displayName(),value.dueDays(),value.version());
    }
    SalesLineSnapshot line(CompanyId company, CurrencyCode currency,
            SalesCommands.LineInput input) {
        var item=catalog.findById(company,input.itemId()).orElseThrow(ReferenceFailure::new);
        if(item.state()!=CatalogItemState.ACTIVE || !item.scopes().contains(CatalogItemScope.SALE)
                || !item.baseUnitCode().equals(input.unitCode())) throw new ReferenceFailure();
        var quote=pricing.quote(company,new CatalogPriceQuoteRequest(input.priceListId(),
                input.itemId(),input.unitCode(),input.quantity(),clock.instant()))
                .orElseThrow(ReferenceFailure::new);
        if(!quote.currency().equals(currency.value()) || !quote.itemId().equals(input.itemId())
                || !quote.unitCode().equals(input.unitCode())) throw new ReferenceFailure();
        BigDecimal price=input.overrideUnitPrice().orElse(quote.unitAmount());
        return new SalesLineSnapshot(input.id(),item.id(),item.code(),item.displayName(),
                item.baseUnitCode(),item.type()==CatalogItemType.PRODUCT,input.quantity(),price,
                quote.taxMode().name(),Optional.of(quote.priceListId().toString()),
                input.overrideUnitPrice().isPresent(),input.overrideReason(),item.version());
    }
    static final class ReferenceFailure extends RuntimeException { }
}
