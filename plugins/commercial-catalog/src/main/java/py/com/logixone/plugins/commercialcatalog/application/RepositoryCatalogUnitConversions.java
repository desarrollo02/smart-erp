package py.com.logixone.plugins.commercialcatalog.application;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversionRequest;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversionResult;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversions;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogItemRepository;
import py.com.logixone.plugins.commercialcatalog.domain.UnitCode;

/** Public deterministic unit conversion for already-authorized plugin use cases. */
public final class RepositoryCatalogUnitConversions implements CatalogUnitConversions {

    private final CatalogItemRepository items;

    public RepositoryCatalogUnitConversions(CatalogItemRepository items) {
        this.items = Objects.requireNonNull(items, "items");
    }

    @Override
    public Optional<CatalogUnitConversionResult> convert(
            CompanyId companyId, CatalogUnitConversionRequest request) {
        Objects.requireNonNull(request, "request");
        return items.findById(
                        Objects.requireNonNull(companyId, "companyId"), request.itemId())
                .map(item -> item.convert(
                        new UnitCode(request.sourceUnitCode()),
                        new UnitCode(request.targetUnitCode()), request.quantity()));
    }
}
