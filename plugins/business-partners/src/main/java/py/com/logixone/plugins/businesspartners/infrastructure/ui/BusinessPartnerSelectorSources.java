package py.com.logixone.plugins.businesspartners.infrastructure.ui;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.SelectorEmptyOptionPolicy;
import py.com.logixone.plugin.api.SelectorInactiveValuePolicy;
import py.com.logixone.plugin.api.SelectorLoadingStrategy;
import py.com.logixone.plugin.api.SelectorManagementCapability;
import py.com.logixone.plugin.api.SelectorSourceDefinition;
import py.com.logixone.plugin.api.SelectorSourceId;
import py.com.logixone.plugin.api.SelectorSourceKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugins.businesspartners.BusinessPartnersScreenContract;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnersIdentity;
import py.com.logixone.plugins.businesspartners.application.BusinessPartnerPermissions;

/** Selector sources already governed by the current business-partners domain. */
final class BusinessPartnerSelectorSources {

    static final Map<ScreenElementId, SelectorSourceDefinition> DIRECTORY = Map.of(
            BusinessPartnersScreenContract.SEARCH_ROLE,
            closed("business_partners.roles", SelectorEmptyOptionPolicy.MEANS_ALL),
            BusinessPartnersScreenContract.SEARCH_STATE,
            closed("business_partners.states", SelectorEmptyOptionPolicy.MEANS_ALL),
            BusinessPartnersScreenContract.NEW_KIND,
            closed("business_partners.kinds", SelectorEmptyOptionPolicy.NOT_ALLOWED),
            BusinessPartnersScreenContract.IDENTIFICATION_TYPE,
            businessCatalog(
                    "business_partners.identification_types",
                    BusinessPartnersScreenContract.DEFINITIONS_ROUTE,
                    BusinessPartnerPermissions.MANAGE,
                    managedCatalogCapabilities(),
                    SelectorEmptyOptionPolicy.NOT_ALLOWED),
            BusinessPartnersScreenContract.IDENTIFICATION_COUNTRY,
            normativeCatalog(
                    "reference_data.countries",
                    SelectorEmptyOptionPolicy.ALLOWED),
            BusinessPartnersScreenContract.ADDRESS_TYPE,
            businessCatalog(
                    "business_partners.address_types",
                    BusinessPartnersScreenContract.DEFINITIONS_ROUTE,
                    BusinessPartnerPermissions.MANAGE,
                    managedCatalogCapabilities(),
                    SelectorEmptyOptionPolicy.NOT_ALLOWED),
            BusinessPartnersScreenContract.ADDRESS_PURPOSE,
            businessCatalog(
                    "business_partners.address_purposes",
                    BusinessPartnersScreenContract.DEFINITIONS_ROUTE,
                    BusinessPartnerPermissions.MANAGE,
                    managedCatalogCapabilities(),
                    SelectorEmptyOptionPolicy.NOT_ALLOWED),
            BusinessPartnersScreenContract.CHANNEL_KIND,
            businessCatalog(
                    "business_partners.channel_kinds",
                    BusinessPartnersScreenContract.DEFINITIONS_ROUTE,
                    BusinessPartnerPermissions.MANAGE,
                    managedCatalogCapabilities(),
                    SelectorEmptyOptionPolicy.NOT_ALLOWED));

    static final Map<ScreenElementId, SelectorSourceDefinition> DEFINITIONS = Map.of(
            BusinessPartnersScreenContract.DEFINITION_KIND,
            closed("business_partners.definition_kinds", SelectorEmptyOptionPolicy.NOT_ALLOWED),
            BusinessPartnersScreenContract.DEFINITION_NEW_KIND,
            closed("business_partners.definition_kinds", SelectorEmptyOptionPolicy.NOT_ALLOWED),
            BusinessPartnersScreenContract.DEFINITION_SEARCH_STATE,
            closed("business_partners.definition_states", SelectorEmptyOptionPolicy.MEANS_ALL));

    private BusinessPartnerSelectorSources() {
    }

    private static SelectorSourceDefinition closed(
            String id, SelectorEmptyOptionPolicy emptyPolicy) {
        return new SelectorSourceDefinition(
                new SelectorSourceId(id),
                BusinessPartnersIdentity.PLUGIN_ID,
                SelectorSourceKind.CLOSED_STATE,
                SemanticVersion.parse("1.0.0"),
                Optional.empty(),
                Optional.empty(),
                Set.of(),
                emptyPolicy,
                SelectorInactiveValuePolicy.NOT_APPLICABLE,
                SelectorLoadingStrategy.INLINE);
    }

    private static SelectorSourceDefinition businessCatalog(
            String id,
            String route,
            ContributionId permission,
            Set<SelectorManagementCapability> capabilities,
            SelectorEmptyOptionPolicy emptyPolicy) {
        return new SelectorSourceDefinition(
                new SelectorSourceId(id),
                BusinessPartnersIdentity.PLUGIN_ID,
                SelectorSourceKind.BUSINESS_CATALOG,
                SemanticVersion.parse("1.0.0"),
                Optional.of(route),
                Optional.of(permission),
                capabilities,
                emptyPolicy,
                SelectorInactiveValuePolicy.EXCLUDE_FOR_NEW_KEEP_SELECTED,
                SelectorLoadingStrategy.INLINE);
    }

    private static SelectorSourceDefinition normativeCatalog(
            String id, SelectorEmptyOptionPolicy emptyPolicy) {
        return new SelectorSourceDefinition(
                new SelectorSourceId(id),
                new py.com.logixone.plugin.api.PluginId("reference_data"),
                SelectorSourceKind.NORMATIVE_CATALOG,
                SemanticVersion.parse("1.0.0"),
                Optional.of("/reference-data"),
                Optional.of(new ContributionId("reference_data.view")),
                Set.of(SelectorManagementCapability.VIEW),
                emptyPolicy,
                SelectorInactiveValuePolicy.EXCLUDE_FOR_NEW_KEEP_SELECTED,
                SelectorLoadingStrategy.INLINE);
    }

    private static Set<SelectorManagementCapability> managedCatalogCapabilities() {
        return Set.of(
                SelectorManagementCapability.VIEW,
                SelectorManagementCapability.CREATE,
                SelectorManagementCapability.EDIT,
                SelectorManagementCapability.INACTIVATE);
    }
}
