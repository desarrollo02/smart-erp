package py.com.logixone.plugins.commercialcatalog.infrastructure.ui;

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
import py.com.logixone.plugins.commercialcatalog.CommercialCatalogScreenContract;
import py.com.logixone.plugins.commercialcatalog.application.CommercialCatalogIdentity;
import py.com.logixone.plugins.commercialcatalog.application.CommercialCatalogPermissions;

/** Selector governance declared by the commercial-catalog interaction adapters. */
final class CommercialCatalogSelectorSources {

    private static final Set<SelectorManagementCapability> FULL_CATALOG = Set.of(
            SelectorManagementCapability.VIEW,
            SelectorManagementCapability.CREATE,
            SelectorManagementCapability.EDIT,
            SelectorManagementCapability.INACTIVATE);
    private static final Set<SelectorManagementCapability> CREATABLE_CATALOG = Set.of(
            SelectorManagementCapability.VIEW,
            SelectorManagementCapability.CREATE);

    static final Map<ScreenElementId, SelectorSourceDefinition> ITEMS = Map.ofEntries(
            entry(CommercialCatalogScreenContract.ITEM_SEARCH_TYPE,
                    closed("commercial_catalog.item_types", SelectorEmptyOptionPolicy.MEANS_ALL)),
            entry(CommercialCatalogScreenContract.ITEM_SEARCH_STATE,
                    closed("commercial_catalog.item_states", SelectorEmptyOptionPolicy.MEANS_ALL)),
            entry(CommercialCatalogScreenContract.ITEM_NEW_TYPE,
                    closed("commercial_catalog.item_types", SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(CommercialCatalogScreenContract.ITEM_NEW_SCOPE,
                    closed("commercial_catalog.sale_scopes", SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(CommercialCatalogScreenContract.ITEM_NEW_BASE_UNIT,
                    businessCatalog(
                            "commercial_catalog.units",
                            CommercialCatalogScreenContract.DEFINITIONS_ROUTE,
                            CommercialCatalogPermissions.DEFINITIONS_MANAGE,
                            CREATABLE_CATALOG,
                            SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(CommercialCatalogScreenContract.ITEM_EDIT_SCOPE,
                    closed("commercial_catalog.sale_scopes", SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(CommercialCatalogScreenContract.MAIN_CATEGORY,
                    businessCatalog(
                            "commercial_catalog.categories",
                            CommercialCatalogScreenContract.DEFINITIONS_ROUTE,
                            CommercialCatalogPermissions.DEFINITIONS_MANAGE,
                            CREATABLE_CATALOG,
                            SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(CommercialCatalogScreenContract.BRAND,
                    businessCatalog(
                            "commercial_catalog.brands",
                            CommercialCatalogScreenContract.DEFINITIONS_ROUTE,
                            CommercialCatalogPermissions.DEFINITIONS_MANAGE,
                            CREATABLE_CATALOG,
                            SelectorEmptyOptionPolicy.ALLOWED)),
            entry(CommercialCatalogScreenContract.CONVERSION_UNIT,
                    businessCatalog(
                            "commercial_catalog.units",
                            CommercialCatalogScreenContract.DEFINITIONS_ROUTE,
                            CommercialCatalogPermissions.DEFINITIONS_MANAGE,
                            CREATABLE_CATALOG,
                            SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(CommercialCatalogScreenContract.CONVERSION_PURPOSE,
                    closed("commercial_catalog.sale_scopes", SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(CommercialCatalogScreenContract.ITEM_NEW_TAX_PROFILE,
                    businessCatalog(
                            "commercial_catalog.tax_profiles",
                            CommercialCatalogScreenContract.TAX_PROFILES_ROUTE,
                            CommercialCatalogPermissions.DEFINITIONS_MANAGE,
                            Set.of(
                                    SelectorManagementCapability.VIEW,
                                    SelectorManagementCapability.CREATE),
                            SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(CommercialCatalogScreenContract.ITEM_TAX_PROFILE,
                    businessCatalog(
                            "commercial_catalog.tax_profiles",
                            CommercialCatalogScreenContract.TAX_PROFILES_ROUTE,
                            CommercialCatalogPermissions.DEFINITIONS_MANAGE,
                            Set.of(
                                    SelectorManagementCapability.VIEW,
                                    SelectorManagementCapability.CREATE),
                            SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(CommercialCatalogScreenContract.ITEM_VARIANT_FAMILY,
                    businessCatalog(
                            "commercial_catalog.variant_families",
                            CommercialCatalogScreenContract.VARIANT_FAMILIES_ROUTE,
                            CommercialCatalogPermissions.DEFINITIONS_MANAGE,
                            FULL_CATALOG,
                            SelectorEmptyOptionPolicy.NOT_ALLOWED)));

    static final Map<ScreenElementId, SelectorSourceDefinition> PRICE_LISTS = Map.ofEntries(
            entry(CommercialCatalogScreenContract.PRICE_SEARCH_STATE,
                    closed("commercial_catalog.price_list_states", SelectorEmptyOptionPolicy.MEANS_ALL)),
            entry(CommercialCatalogScreenContract.PRICE_CURRENCY,
                    normativeCatalog("reference_data.currencies")),
            entry(CommercialCatalogScreenContract.PRICE_TAX_MODE,
                    closed("commercial_catalog.tax_modes", SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(CommercialCatalogScreenContract.PRICE_SCALE,
                    closed("commercial_catalog.price_scales", SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(CommercialCatalogScreenContract.PRICE_ROUNDING_MODE,
                    closed("commercial_catalog.rounding_modes", SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(CommercialCatalogScreenContract.PRICE_ENTRY_ITEM,
                    managed(
                            "commercial_catalog.items",
                            CommercialCatalogScreenContract.ITEMS_ROUTE,
                            CommercialCatalogPermissions.ITEMS_MANAGE,
                            FULL_CATALOG,
                            SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(CommercialCatalogScreenContract.PRICE_ENTRY_UNIT,
                    businessCatalog(
                            "commercial_catalog.units",
                            CommercialCatalogScreenContract.DEFINITIONS_ROUTE,
                            CommercialCatalogPermissions.DEFINITIONS_MANAGE,
                            CREATABLE_CATALOG,
                            SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(CommercialCatalogScreenContract.PRICE_ENTRY_TO_INACTIVATE,
                    managed(
                            "commercial_catalog.price_entries",
                            CommercialCatalogScreenContract.PRICE_LISTS_ROUTE,
                            CommercialCatalogPermissions.PRICES_MANAGE,
                            Set.of(
                                    SelectorManagementCapability.VIEW,
                                    SelectorManagementCapability.CREATE,
                                    SelectorManagementCapability.INACTIVATE),
                            SelectorEmptyOptionPolicy.ALLOWED)));

    static final Map<ScreenElementId, SelectorSourceDefinition> TAX_PROFILES = Map.of(
            CommercialCatalogScreenContract.TAX_PROFILE_SEARCH_STATE,
            closed("commercial_catalog.tax_profile_states", SelectorEmptyOptionPolicy.MEANS_ALL));

    static final Map<ScreenElementId, SelectorSourceDefinition> DEFINITIONS = Map.ofEntries(
            entry(CommercialCatalogScreenContract.DEFINITION_SEARCH_KIND,
                    closed("commercial_catalog.definition_kinds",
                            SelectorEmptyOptionPolicy.MEANS_ALL)),
            entry(CommercialCatalogScreenContract.DEFINITION_SEARCH_STATE,
                    closed("commercial_catalog.definition_states",
                            SelectorEmptyOptionPolicy.MEANS_ALL)),
            entry(CommercialCatalogScreenContract.DEFINITION_NEW_KIND,
                    closed("commercial_catalog.definition_kinds",
                            SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(CommercialCatalogScreenContract.DEFINITION_UNIT_SCALE,
                    closed("commercial_catalog.unit_scales",
                            SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(CommercialCatalogScreenContract.DEFINITION_REVISION_UNIT_SCALE,
                    closed("commercial_catalog.unit_scales",
                            SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(CommercialCatalogScreenContract.DEFINITION_REPLACEMENT_UNIT_SCALE,
                    closed("commercial_catalog.unit_scales",
                            SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(CommercialCatalogScreenContract.DEFINITION_CATEGORY_PARENT,
                    businessCatalog(
                            "commercial_catalog.categories",
                            CommercialCatalogScreenContract.DEFINITIONS_ROUTE,
                            CommercialCatalogPermissions.DEFINITIONS_MANAGE,
                            CREATABLE_CATALOG,
                            SelectorEmptyOptionPolicy.ALLOWED)),
            entry(CommercialCatalogScreenContract.DEFINITION_REVISION_CATEGORY_PARENT,
                    businessCatalog(
                            "commercial_catalog.categories",
                            CommercialCatalogScreenContract.DEFINITIONS_ROUTE,
                            CommercialCatalogPermissions.DEFINITIONS_MANAGE,
                            CREATABLE_CATALOG,
                            SelectorEmptyOptionPolicy.ALLOWED)),
            entry(CommercialCatalogScreenContract.DEFINITION_REPLACEMENT_CATEGORY_PARENT,
                    businessCatalog(
                            "commercial_catalog.categories",
                            CommercialCatalogScreenContract.DEFINITIONS_ROUTE,
                            CommercialCatalogPermissions.DEFINITIONS_MANAGE,
                            CREATABLE_CATALOG,
                            SelectorEmptyOptionPolicy.ALLOWED)));

    static final Map<ScreenElementId, SelectorSourceDefinition> VARIANT_FAMILIES = Map.ofEntries(
            entry(CommercialCatalogScreenContract.VARIANT_FAMILY_SEARCH_STATE,
                    closed("commercial_catalog.variant_family_states",
                            SelectorEmptyOptionPolicy.MEANS_ALL)),
            entry(CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_TYPE,
                    closed("commercial_catalog.variant_attribute_types",
                            SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(CommercialCatalogScreenContract.VARIANT_ATTRIBUTE_REQUIRED,
                    closed("commercial_catalog.variant_attribute_requirements",
                            SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_TYPE,
                    closed("commercial_catalog.variant_attribute_types",
                            SelectorEmptyOptionPolicy.NOT_ALLOWED)),
            entry(CommercialCatalogScreenContract.VARIANT_REVISION_ATTRIBUTE_REQUIRED,
                    closed("commercial_catalog.variant_attribute_requirements",
                            SelectorEmptyOptionPolicy.NOT_ALLOWED)));

    private CommercialCatalogSelectorSources() {
    }

    private static Map.Entry<ScreenElementId, SelectorSourceDefinition> entry(
            ScreenElementId field, SelectorSourceDefinition source) {
        return Map.entry(field, source);
    }

    private static SelectorSourceDefinition closed(
            String id, SelectorEmptyOptionPolicy emptyPolicy) {
        return new SelectorSourceDefinition(
                new SelectorSourceId(id),
                CommercialCatalogIdentity.PLUGIN_ID,
                SelectorSourceKind.CLOSED_STATE,
                SemanticVersion.parse("1.0.0"),
                Optional.empty(),
                Optional.empty(),
                Set.of(),
                emptyPolicy,
                SelectorInactiveValuePolicy.NOT_APPLICABLE,
                SelectorLoadingStrategy.INLINE);
    }

    private static SelectorSourceDefinition managed(
            String id,
            String route,
            ContributionId permission,
            Set<SelectorManagementCapability> capabilities,
            SelectorEmptyOptionPolicy emptyPolicy) {
        return source(
                id,
                SelectorSourceKind.OPERATIONAL_REFERENCE,
                route,
                permission,
                capabilities,
                emptyPolicy);
    }

    private static SelectorSourceDefinition businessCatalog(
            String id,
            String route,
            ContributionId permission,
            Set<SelectorManagementCapability> capabilities,
            SelectorEmptyOptionPolicy emptyPolicy) {
        return source(
                id,
                SelectorSourceKind.BUSINESS_CATALOG,
                route,
                permission,
                capabilities,
                emptyPolicy);
    }

    private static SelectorSourceDefinition normativeCatalog(String id) {
        return new SelectorSourceDefinition(
                new SelectorSourceId(id),
                new py.com.logixone.plugin.api.PluginId("reference_data"),
                SelectorSourceKind.NORMATIVE_CATALOG,
                SemanticVersion.parse("1.0.0"),
                Optional.of("/reference-data"),
                Optional.of(new ContributionId("reference_data.view")),
                Set.of(SelectorManagementCapability.VIEW),
                SelectorEmptyOptionPolicy.NOT_ALLOWED,
                SelectorInactiveValuePolicy.EXCLUDE_FOR_NEW_KEEP_SELECTED,
                SelectorLoadingStrategy.INLINE);
    }

    private static SelectorSourceDefinition source(
            String id,
            SelectorSourceKind kind,
            String route,
            ContributionId permission,
            Set<SelectorManagementCapability> capabilities,
            SelectorEmptyOptionPolicy emptyPolicy) {
        return new SelectorSourceDefinition(
                new SelectorSourceId(id),
                CommercialCatalogIdentity.PLUGIN_ID,
                kind,
                SemanticVersion.parse("1.0.0"),
                Optional.of(route),
                Optional.of(permission),
                capabilities,
                emptyPolicy,
                SelectorInactiveValuePolicy.EXCLUDE_FOR_NEW_KEEP_SELECTED,
                SelectorLoadingStrategy.INLINE);
    }
}
