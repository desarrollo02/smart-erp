package py.com.logixone.plugins.commercialcatalog.infrastructure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugin.api.ScreenDefinition;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenElementType;
import py.com.logixone.plugin.api.SelectorSourceKind;
import py.com.logixone.plugins.commercialcatalog.CommercialCatalogScreenContract;

class CommercialCatalogSelectorSourcesTest {

    @Test
    void declaresEveryCurrentSelectorAndRoutesManagedDefinitionsToTheirOwner() {
        Set<ScreenElementId> pending = new HashSet<>();
        pending.addAll(pending(
                CommercialCatalogScreenContract.itemsDefinition(),
                CommercialCatalogSelectorSources.ITEMS.keySet()));
        pending.addAll(pending(
                CommercialCatalogScreenContract.priceListsDefinition(),
                CommercialCatalogSelectorSources.PRICE_LISTS.keySet()));
        pending.addAll(pending(
                CommercialCatalogScreenContract.definitionsDefinition(),
                CommercialCatalogSelectorSources.DEFINITIONS.keySet()));
        pending.addAll(pending(
                CommercialCatalogScreenContract.taxProfilesDefinition(),
                CommercialCatalogSelectorSources.TAX_PROFILES.keySet()));
        pending.addAll(pending(
                CommercialCatalogScreenContract.variantFamiliesDefinition(),
                CommercialCatalogSelectorSources.VARIANT_FAMILIES.keySet()));

        assertEquals(Set.of(), pending);
        assertEquals(
                SelectorSourceKind.BUSINESS_CATALOG,
                CommercialCatalogSelectorSources.ITEMS
                        .get(CommercialCatalogScreenContract.ITEM_NEW_TAX_PROFILE).kind());
        assertEquals(
                CommercialCatalogScreenContract.DEFINITIONS_ROUTE,
                CommercialCatalogSelectorSources.ITEMS
                        .get(CommercialCatalogScreenContract.ITEM_NEW_BASE_UNIT)
                        .managementRoute().orElseThrow());
        assertEquals(
                SelectorSourceKind.NORMATIVE_CATALOG,
                CommercialCatalogSelectorSources.PRICE_LISTS
                        .get(CommercialCatalogScreenContract.PRICE_CURRENCY).kind());
        assertEquals(
                "reference_data",
                CommercialCatalogSelectorSources.PRICE_LISTS
                        .get(CommercialCatalogScreenContract.PRICE_CURRENCY)
                        .ownerPluginId().value());
        assertEquals(
                py.com.logixone.plugin.api.SelectorLoadingStrategy.SEARCH_ON_DEMAND,
                CommercialCatalogSelectorSources.PRICE_LISTS
                        .get(CommercialCatalogScreenContract.PRICE_CURRENCY)
                        .loadingStrategy());
        assertEquals(
                CommercialCatalogScreenContract.DEFINITIONS_ROUTE,
                CommercialCatalogSelectorSources.PRICE_LISTS
                        .get(CommercialCatalogScreenContract.PRICE_ENTRY_UNIT)
                        .managementRoute().orElseThrow());
    }

    private static Set<ScreenElementId> pending(
            ScreenDefinition screen, Set<ScreenElementId> declared) {
        Set<ScreenElementId> result = screen.elements().stream()
                .filter(element -> element.type() == ScreenElementType.SELECT)
                .map(element -> element.id())
                .collect(Collectors.toCollection(HashSet::new));
        result.removeAll(declared);
        return result;
    }
}
