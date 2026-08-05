package py.com.logixone.plugins.businesspartners.infrastructure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugin.api.ScreenElementType;
import py.com.logixone.plugins.businesspartners.BusinessPartnersScreenContract;

class BusinessPartnerSelectorSourcesTest {

    @Test
    void declaresEverySelectorAndMakesOperationalTypesCompanyManagedCatalogs() {
        Set<?> selectors = BusinessPartnersScreenContract.definition().elements().stream()
                .filter(element -> element.type() == ScreenElementType.SELECT)
                .map(element -> element.id())
                .collect(Collectors.toSet());
        var pending = new java.util.HashSet<>(selectors);
        pending.removeAll(BusinessPartnerSelectorSources.DIRECTORY.keySet());

        assertTrue(pending.isEmpty());
        Set.of(
                BusinessPartnersScreenContract.CHANNEL_KIND,
                BusinessPartnersScreenContract.IDENTIFICATION_TYPE,
                BusinessPartnersScreenContract.ADDRESS_TYPE,
                BusinessPartnersScreenContract.ADDRESS_PURPOSE).forEach(field -> {
                    var source = BusinessPartnerSelectorSources.DIRECTORY.get(field);
                    assertEquals(py.com.logixone.plugin.api.SelectorSourceKind.BUSINESS_CATALOG,
                            source.kind());
                    assertEquals(java.util.Optional.of(BusinessPartnersScreenContract.DEFINITIONS_ROUTE),
                            source.managementRoute());
                    assertEquals(java.util.Optional.of(
                            py.com.logixone.plugins.businesspartners.application.BusinessPartnerPermissions.MANAGE),
                            source.managementPermission());
                    assertEquals(Set.of(
                            py.com.logixone.plugin.api.SelectorManagementCapability.VIEW,
                            py.com.logixone.plugin.api.SelectorManagementCapability.CREATE,
                            py.com.logixone.plugin.api.SelectorManagementCapability.EDIT,
                            py.com.logixone.plugin.api.SelectorManagementCapability.INACTIVATE),
                            source.managementCapabilities());
                });
        var countrySource = BusinessPartnerSelectorSources.DIRECTORY.get(
                BusinessPartnersScreenContract.IDENTIFICATION_COUNTRY);
        assertEquals(py.com.logixone.plugin.api.SelectorSourceKind.NORMATIVE_CATALOG,
                countrySource.kind());
        assertEquals("reference_data", countrySource.ownerPluginId().value());
        assertEquals(java.util.Optional.of("/reference-data"), countrySource.managementRoute());
        assertEquals(Set.of(py.com.logixone.plugin.api.SelectorManagementCapability.VIEW),
                countrySource.managementCapabilities());
        assertEquals(py.com.logixone.plugin.api.SelectorLoadingStrategy.SEARCH_ON_DEMAND,
                countrySource.loadingStrategy());

        Set<?> definitionSelectors = BusinessPartnersScreenContract.definitions().elements().stream()
                .filter(element -> element.type() == ScreenElementType.SELECT)
                .map(element -> element.id())
                .collect(Collectors.toSet());
        var missingDefinitions = new java.util.HashSet<>(definitionSelectors);
        missingDefinitions.removeAll(BusinessPartnerSelectorSources.DEFINITIONS.keySet());
        assertTrue(missingDefinitions.isEmpty());
        assertTrue(definitionSelectors.contains(BusinessPartnersScreenContract.DEFINITION_KIND));
        assertTrue(definitionSelectors.contains(BusinessPartnersScreenContract.DEFINITION_NEW_KIND));
    }
}
