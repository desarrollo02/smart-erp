package py.com.logixone.plugins.commercialcatalog;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import py.com.logixone.plugin.api.ScreenCustomizationOperation;
import py.com.logixone.plugin.api.ScreenDefinition;
import py.com.logixone.plugin.api.ScreenElementDefinition;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenElementType;
import py.com.logixone.plugin.api.ScreenId;
import py.com.logixone.plugin.api.ScreenRegionId;
import py.com.logixone.plugin.api.ScreenSlotDefinition;
import py.com.logixone.plugin.api.ScreenSlotId;
import py.com.logixone.plugin.api.ScreenTextKey;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugins.commercialcatalog.application.CommercialCatalogIdentity;

/** Stable UI contracts rendered by the shell without accepting plugin markup. */
public final class CommercialCatalogScreenContract {

    public static final String ITEMS_ROUTE = "/catalog";
    public static final String PRICE_LISTS_ROUTE = "/catalog/price-lists";
    public static final String DEFINITIONS_ROUTE = "/catalog/definitions";
    public static final String VARIANT_FAMILIES_ROUTE = "/catalog/variant-families";
    public static final String TAX_PROFILES_ROUTE = "/catalog/tax-profiles";
    public static final ScreenId ITEMS = new ScreenId(
            CommercialCatalogIdentity.PLUGIN_ID, "items");
    public static final ScreenId PRICE_LISTS = new ScreenId(
            CommercialCatalogIdentity.PLUGIN_ID, "price_lists");
    public static final ScreenId DEFINITIONS = new ScreenId(
            CommercialCatalogIdentity.PLUGIN_ID, "definitions");
    public static final ScreenId VARIANT_FAMILIES = new ScreenId(
            CommercialCatalogIdentity.PLUGIN_ID, "variant_families");
    public static final ScreenId TAX_PROFILES = new ScreenId(
            CommercialCatalogIdentity.PLUGIN_ID, "tax_profiles");

    public static final ScreenElementId ITEM_SEARCH_TEXT = id("item_search_text");
    public static final ScreenElementId ITEM_SEARCH_TYPE = id("item_search_type");
    public static final ScreenElementId ITEM_SEARCH_STATE = id("item_search_state");
    public static final ScreenElementId ITEM_SEARCH = id("item_search");
    public static final ScreenElementId ITEM_RESULTS = id("item_results");
    public static final ScreenElementId SELECT_ITEM = id("select_item");
    public static final ScreenElementId ITEM_NEW_CODE = id("item_new_code");
    public static final ScreenElementId ITEM_NEW_NAME = id("item_new_name");
    public static final ScreenElementId ITEM_NEW_DESCRIPTION = id("item_new_description");
    public static final ScreenElementId ITEM_NEW_TYPE = id("item_new_type");
    public static final ScreenElementId ITEM_NEW_SCOPE = id("item_new_scope");
    public static final ScreenElementId ITEM_NEW_BASE_UNIT = id("item_new_base_unit");
    public static final ScreenElementId ITEM_NEW_TAX_PROFILE = id("item_new_tax_profile");
    public static final ScreenElementId REGISTER_ITEM = id("register_item");
    public static final ScreenElementId ITEM_EDIT_CODE = id("item_edit_code");
    public static final ScreenElementId ITEM_EDIT_NAME = id("item_edit_name");
    public static final ScreenElementId ITEM_EDIT_DESCRIPTION = id("item_edit_description");
    public static final ScreenElementId ITEM_EDIT_SCOPE = id("item_edit_scope");
    public static final ScreenElementId REVISE_ITEM = id("revise_item");
    public static final ScreenElementId IDENTIFIER_TYPE = id("identifier_type");
    public static final ScreenElementId IDENTIFIER_VALUE = id("identifier_value");
    public static final ScreenElementId ADD_IDENTIFIER = id("add_identifier");
    public static final ScreenElementId MAIN_CATEGORY = id("main_category");
    public static final ScreenElementId BRAND = id("brand");
    public static final ScreenElementId CLASSIFY_ITEM = id("classify_item");
    public static final ScreenElementId CONVERSION_UNIT = id("conversion_unit");
    public static final ScreenElementId CONVERSION_FACTOR = id("conversion_factor");
    public static final ScreenElementId CONVERSION_PURPOSE = id("conversion_purpose");
    public static final ScreenElementId ADD_CONVERSION = id("add_conversion");
    public static final ScreenElementId ITEM_TAX_PROFILE = id("item_tax_profile");
    public static final ScreenElementId ASSIGN_TAX_PROFILE = id("assign_tax_profile");
    public static final ScreenElementId ITEM_VARIANT_FAMILY = id("item_variant_family");
    public static final ScreenElementId ITEM_VARIANT_STRUCTURE = id("item_variant_structure");
    public static final ScreenElementId ITEM_VARIANT_VALUES = id("item_variant_values");
    public static final ScreenElementId PREPARE_ITEM_VARIANT = id("prepare_item_variant");
    public static final ScreenElementId ASSIGN_ITEM_VARIANT = id("assign_item_variant");
    public static final ScreenElementId ACTIVATE_ITEM = id("activate_item");
    public static final ScreenElementId INACTIVATE_ITEM = id("inactivate_item");

    public static final ScreenElementId PRICE_SEARCH_TEXT = id("price_search_text");
    public static final ScreenElementId PRICE_SEARCH_STATE = id("price_search_state");
    public static final ScreenElementId PRICE_SEARCH = id("price_search");
    public static final ScreenElementId PRICE_RESULTS = id("price_results");
    public static final ScreenElementId SELECT_PRICE_LIST = id("select_price_list");
    public static final ScreenElementId PRICE_NEW_CODE = id("price_new_code");
    public static final ScreenElementId PRICE_NEW_NAME = id("price_new_name");
    public static final ScreenElementId PRICE_CURRENCY = id("price_currency");
    public static final ScreenElementId PRICE_TAX_MODE = id("price_tax_mode");
    public static final ScreenElementId PRICE_SCALE = id("price_scale");
    public static final ScreenElementId PRICE_ROUNDING_MODE = id("price_rounding_mode");
    public static final ScreenElementId REGISTER_PRICE_LIST = id("register_price_list");
    public static final ScreenElementId PRICE_EDIT_NAME = id("price_edit_name");
    public static final ScreenElementId RENAME_PRICE_LIST = id("rename_price_list");
    public static final ScreenElementId PRICE_ENTRY_ITEM = id("price_entry_item");
    public static final ScreenElementId PRICE_ENTRY_UNIT = id("price_entry_unit");
    public static final ScreenElementId PRICE_ENTRY_MINIMUM = id("price_entry_minimum");
    public static final ScreenElementId PRICE_ENTRY_AMOUNT = id("price_entry_amount");
    public static final ScreenElementId PRICE_ENTRY_VALID_FROM = id("price_entry_valid_from");
    public static final ScreenElementId PRICE_ENTRY_VALID_UNTIL = id("price_entry_valid_until");
    public static final ScreenElementId ADD_PRICE_ENTRY = id("add_price_entry");
    public static final ScreenElementId PRICE_ENTRY_TO_INACTIVATE = id("price_entry_to_inactivate");
    public static final ScreenElementId INACTIVATE_PRICE_ENTRY = id("inactivate_price_entry");
    public static final ScreenElementId ACTIVATE_PRICE_LIST = id("activate_price_list");
    public static final ScreenElementId INACTIVATE_PRICE_LIST = id("inactivate_price_list");

    public static final ScreenElementId DEFINITION_SEARCH_TEXT = id("definition_search_text");
    public static final ScreenElementId DEFINITION_SEARCH_KIND = id("definition_search_kind");
    public static final ScreenElementId DEFINITION_SEARCH_STATE = id("definition_search_state");
    public static final ScreenElementId DEFINITION_SEARCH = id("definition_search");
    public static final ScreenElementId DEFINITION_RESULTS = id("definition_results");
    public static final ScreenElementId SELECT_DEFINITION = id("select_definition");
    public static final ScreenElementId DEFINITION_NEW_KIND = id("definition_new_kind");
    public static final ScreenElementId DEFINITION_NEW_CODE = id("definition_new_code");
    public static final ScreenElementId DEFINITION_NEW_NAME = id("definition_new_name");
    public static final ScreenElementId DEFINITION_UNIT_SCALE = id("definition_unit_scale");
    public static final ScreenElementId DEFINITION_CATEGORY_PARENT =
            id("definition_category_parent");
    public static final ScreenElementId REGISTER_DEFINITION = id("register_definition");
    public static final ScreenElementId DEFINITION_REVISION_NAME =
            id("definition_revision_name");
    public static final ScreenElementId DEFINITION_REVISION_UNIT_SCALE =
            id("definition_revision_unit_scale");
    public static final ScreenElementId DEFINITION_REVISION_CATEGORY_PARENT =
            id("definition_revision_category_parent");
    public static final ScreenElementId REVISE_DEFINITION = id("revise_definition");
    public static final ScreenElementId DEFINITION_HISTORY = id("definition_history");
    public static final ScreenElementId DEFINITION_REPLACEMENT_CODE =
            id("definition_replacement_code");
    public static final ScreenElementId DEFINITION_REPLACEMENT_NAME =
            id("definition_replacement_name");
    public static final ScreenElementId DEFINITION_REPLACEMENT_UNIT_SCALE =
            id("definition_replacement_unit_scale");
    public static final ScreenElementId DEFINITION_REPLACEMENT_CATEGORY_PARENT =
            id("definition_replacement_category_parent");
    public static final ScreenElementId REPLACE_DEFINITION = id("replace_definition");
    public static final ScreenElementId ACTIVATE_DEFINITION = id("activate_definition");
    public static final ScreenElementId INACTIVATE_DEFINITION = id("inactivate_definition");

    public static final ScreenElementId VARIANT_FAMILY_SEARCH_TEXT =
            id("variant_family_search_text");
    public static final ScreenElementId VARIANT_FAMILY_SEARCH_STATE =
            id("variant_family_search_state");
    public static final ScreenElementId VARIANT_FAMILY_SEARCH = id("variant_family_search");
    public static final ScreenElementId VARIANT_FAMILY_RESULTS = id("variant_family_results");
    public static final ScreenElementId SELECT_VARIANT_FAMILY = id("select_variant_family");
    public static final ScreenElementId VARIANT_FAMILY_NEW_CODE = id("variant_family_new_code");
    public static final ScreenElementId VARIANT_FAMILY_NEW_NAME = id("variant_family_new_name");
    public static final ScreenElementId VARIANT_ATTRIBUTE_CODE = id("variant_attribute_code");
    public static final ScreenElementId VARIANT_ATTRIBUTE_NAME = id("variant_attribute_name");
    public static final ScreenElementId VARIANT_ATTRIBUTE_TYPE = id("variant_attribute_type");
    public static final ScreenElementId VARIANT_ATTRIBUTE_REQUIRED =
            id("variant_attribute_required");
    public static final ScreenElementId VARIANT_ATTRIBUTE_DRAFT = id("variant_attribute_draft");
    public static final ScreenElementId ADD_VARIANT_ATTRIBUTE = id("add_variant_attribute");
    public static final ScreenElementId REMOVE_VARIANT_ATTRIBUTE = id("remove_variant_attribute");
    public static final ScreenElementId REGISTER_VARIANT_FAMILY = id("register_variant_family");
    public static final ScreenElementId VARIANT_FAMILY_REVISION_NAME =
            id("variant_family_revision_name");
    public static final ScreenElementId VARIANT_REVISION_ATTRIBUTE_CODE =
            id("variant_revision_attribute_code");
    public static final ScreenElementId VARIANT_REVISION_ATTRIBUTE_NAME =
            id("variant_revision_attribute_name");
    public static final ScreenElementId VARIANT_REVISION_ATTRIBUTE_TYPE =
            id("variant_revision_attribute_type");
    public static final ScreenElementId VARIANT_REVISION_ATTRIBUTE_REQUIRED =
            id("variant_revision_attribute_required");
    public static final ScreenElementId VARIANT_REVISION_ATTRIBUTE_DRAFT =
            id("variant_revision_attribute_draft");
    public static final ScreenElementId ADD_VARIANT_REVISION_ATTRIBUTE =
            id("add_variant_revision_attribute");
    public static final ScreenElementId REMOVE_VARIANT_REVISION_ATTRIBUTE =
            id("remove_variant_revision_attribute");
    public static final ScreenElementId REVISE_VARIANT_FAMILY =
            id("revise_variant_family");
    public static final ScreenElementId VARIANT_FAMILY_HISTORY =
            id("variant_family_history");
    public static final ScreenElementId ACTIVATE_VARIANT_FAMILY =
            id("activate_variant_family");
    public static final ScreenElementId INACTIVATE_VARIANT_FAMILY =
            id("inactivate_variant_family");

    public static final ScreenElementId TAX_PROFILE_SEARCH_TEXT = id("tax_profile_search_text");
    public static final ScreenElementId TAX_PROFILE_SEARCH_STATE = id("tax_profile_search_state");
    public static final ScreenElementId TAX_PROFILE_SEARCH = id("tax_profile_search");
    public static final ScreenElementId TAX_PROFILE_RESULTS = id("tax_profile_results");
    public static final ScreenElementId SELECT_TAX_PROFILE = id("select_tax_profile");
    public static final ScreenElementId TAX_PROFILE_NEW_CODE = id("tax_profile_new_code");
    public static final ScreenElementId TAX_PROFILE_NEW_NAME = id("tax_profile_new_name");
    public static final ScreenElementId TAX_PROFILE_NEW_KIND = id("tax_profile_new_kind");
    public static final ScreenElementId TAX_PROFILE_NEW_DESCRIPTION =
            id("tax_profile_new_description");
    public static final ScreenElementId TAX_PROFILE_NEW_VALID_FROM =
            id("tax_profile_new_valid_from");
    public static final ScreenElementId TAX_PROFILE_NEW_VALID_UNTIL =
            id("tax_profile_new_valid_until");
    public static final ScreenElementId REGISTER_TAX_PROFILE = id("register_tax_profile");
    public static final ScreenElementId TAX_PROFILE_REVISION_KIND =
            id("tax_profile_revision_kind");
    public static final ScreenElementId TAX_PROFILE_REVISION_DESCRIPTION =
            id("tax_profile_revision_description");
    public static final ScreenElementId TAX_PROFILE_REVISION_VALID_FROM =
            id("tax_profile_revision_valid_from");
    public static final ScreenElementId TAX_PROFILE_REVISION_VALID_UNTIL =
            id("tax_profile_revision_valid_until");
    public static final ScreenElementId REVISE_TAX_PROFILE = id("revise_tax_profile");
    public static final ScreenElementId TAX_PROFILE_HISTORY = id("tax_profile_history");
    public static final ScreenElementId ACTIVATE_TAX_PROFILE = id("activate_tax_profile");
    public static final ScreenElementId INACTIVATE_TAX_PROFILE = id("inactivate_tax_profile");

    public static final ScreenSlotId DIRECTORY_EXTENSIONS =
            new ScreenSlotId("directory_extensions");
    public static final ScreenSlotId DETAIL_EXTENSIONS =
            new ScreenSlotId("detail_extensions");

    private static final Set<ScreenCustomizationOperation> FIELD_CHANGES = EnumSet.of(
            ScreenCustomizationOperation.CHANGE_LABEL,
            ScreenCustomizationOperation.CHANGE_HELP,
            ScreenCustomizationOperation.HIDE,
            ScreenCustomizationOperation.DISABLE,
            ScreenCustomizationOperation.REQUIRE,
            ScreenCustomizationOperation.REORDER);
    private static final Set<ScreenCustomizationOperation> ACTION_CHANGES = EnumSet.of(
            ScreenCustomizationOperation.CHANGE_LABEL,
            ScreenCustomizationOperation.CHANGE_HELP,
            ScreenCustomizationOperation.HIDE,
            ScreenCustomizationOperation.DISABLE,
            ScreenCustomizationOperation.REORDER);
    private static final Set<ScreenCustomizationOperation> CONTENT_CHANGES = EnumSet.of(
            ScreenCustomizationOperation.CHANGE_LABEL,
            ScreenCustomizationOperation.CHANGE_HELP,
            ScreenCustomizationOperation.HIDE,
            ScreenCustomizationOperation.REORDER);

    private CommercialCatalogScreenContract() {
    }

    public static ScreenDefinition itemsDefinition() {
        return definition(ITEMS, "items", List.of(
                field(ITEM_SEARCH_TEXT, ScreenElementType.TEXT_INPUT, "search", 10, false, "items"),
                field(ITEM_SEARCH_TYPE, ScreenElementType.SELECT, "search", 20, false, "items"),
                field(ITEM_SEARCH_STATE, ScreenElementType.SELECT, "search", 30, false, "items"),
                action(ITEM_SEARCH, "search_actions", 10, "items"),
                content(ITEM_RESULTS, ScreenElementType.DATA_TABLE, "results", 10, "items"),
                action(SELECT_ITEM, "row_actions", 10, "items"),
                field(ITEM_NEW_CODE, ScreenElementType.TEXT_INPUT, "create", 10, false, "items"),
                field(ITEM_NEW_NAME, ScreenElementType.TEXT_INPUT, "create", 20, true, "items"),
                field(ITEM_NEW_DESCRIPTION, ScreenElementType.TEXT_INPUT, "create", 30, false, "items"),
                field(ITEM_NEW_TYPE, ScreenElementType.SELECT, "create", 40, true, "items"),
                field(ITEM_NEW_SCOPE, ScreenElementType.SELECT, "create", 50, true, "items"),
                field(ITEM_NEW_BASE_UNIT, ScreenElementType.SELECT, "create", 60, true, "items"),
                field(ITEM_NEW_TAX_PROFILE, ScreenElementType.SELECT, "create", 70, true, "items"),
                action(REGISTER_ITEM, "create_actions", 10, "items"),
                field(ITEM_EDIT_CODE, ScreenElementType.TEXT_INPUT, "general", 10, true, "items"),
                field(ITEM_EDIT_NAME, ScreenElementType.TEXT_INPUT, "general", 20, true, "items"),
                field(ITEM_EDIT_DESCRIPTION, ScreenElementType.TEXT_INPUT, "general", 30, false, "items"),
                field(ITEM_EDIT_SCOPE, ScreenElementType.SELECT, "general", 40, true, "items"),
                action(REVISE_ITEM, "general_actions", 10, "items"),
                field(IDENTIFIER_TYPE, ScreenElementType.TEXT_INPUT, "identifiers", 10, true, "items"),
                field(IDENTIFIER_VALUE, ScreenElementType.TEXT_INPUT, "identifiers", 20, true, "items"),
                action(ADD_IDENTIFIER, "identifiers_actions", 10, "items"),
                field(MAIN_CATEGORY, ScreenElementType.SELECT, "classification", 10, true, "items"),
                field(BRAND, ScreenElementType.SELECT, "classification", 20, false, "items"),
                action(CLASSIFY_ITEM, "classification_actions", 10, "items"),
                field(CONVERSION_UNIT, ScreenElementType.SELECT, "units", 10, true, "items"),
                field(CONVERSION_FACTOR, ScreenElementType.TEXT_INPUT, "units", 20, true, "items"),
                field(CONVERSION_PURPOSE, ScreenElementType.SELECT, "units", 30, true, "items"),
                action(ADD_CONVERSION, "units_actions", 10, "items"),
                field(ITEM_TAX_PROFILE, ScreenElementType.SELECT, "tax", 10, true, "items"),
                action(ASSIGN_TAX_PROFILE, "tax_actions", 10, "items"),
                field(ITEM_VARIANT_FAMILY, ScreenElementType.SELECT,
                        "variants", 10, true, "items"),
                content(ITEM_VARIANT_STRUCTURE, ScreenElementType.DISPLAY_TEXT,
                        "variants", 20, "items"),
                field(ITEM_VARIANT_VALUES, ScreenElementType.TEXT_INPUT,
                        "variants", 30, true, "items"),
                action(PREPARE_ITEM_VARIANT, "variants_actions", 10, "items"),
                action(ASSIGN_ITEM_VARIANT, "variants_actions", 20, "items"),
                action(ACTIVATE_ITEM, "lifecycle_actions", 10, "items"),
                action(INACTIVATE_ITEM, "lifecycle_actions", 20, "items")));
    }

    public static ScreenDefinition priceListsDefinition() {
        return definition(PRICE_LISTS, "price_lists", "1.1.0", List.of(
                field(PRICE_SEARCH_TEXT, ScreenElementType.TEXT_INPUT, "search", 10, false, "price_lists"),
                field(PRICE_SEARCH_STATE, ScreenElementType.SELECT, "search", 20, false, "price_lists"),
                action(PRICE_SEARCH, "search_actions", 10, "price_lists"),
                content(PRICE_RESULTS, ScreenElementType.DATA_TABLE, "results", 10, "price_lists"),
                action(SELECT_PRICE_LIST, "row_actions", 10, "price_lists"),
                field(PRICE_NEW_CODE, ScreenElementType.TEXT_INPUT, "create", 10, false, "price_lists"),
                field(PRICE_NEW_NAME, ScreenElementType.TEXT_INPUT, "create", 20, true, "price_lists"),
                field(PRICE_CURRENCY, ScreenElementType.SELECT, "create", 30, true, "price_lists"),
                field(PRICE_TAX_MODE, ScreenElementType.SELECT, "create", 40, true, "price_lists"),
                field(PRICE_SCALE, ScreenElementType.SELECT, "create", 50, true, "price_lists"),
                field(PRICE_ROUNDING_MODE, ScreenElementType.SELECT, "create", 60, true, "price_lists"),
                action(REGISTER_PRICE_LIST, "create_actions", 10, "price_lists"),
                field(PRICE_EDIT_NAME, ScreenElementType.TEXT_INPUT, "general", 10, true, "price_lists"),
                action(RENAME_PRICE_LIST, "general_actions", 10, "price_lists"),
                field(PRICE_ENTRY_ITEM, ScreenElementType.SELECT, "entries", 10, true, "price_lists"),
                field(PRICE_ENTRY_UNIT, ScreenElementType.SELECT, "entries", 20, true, "price_lists"),
                field(PRICE_ENTRY_MINIMUM, ScreenElementType.TEXT_INPUT, "entries", 30, true, "price_lists"),
                field(PRICE_ENTRY_AMOUNT, ScreenElementType.TEXT_INPUT, "entries", 40, true, "price_lists"),
                field(PRICE_ENTRY_VALID_FROM, ScreenElementType.TEXT_INPUT, "entries", 50, true, "price_lists"),
                field(PRICE_ENTRY_VALID_UNTIL, ScreenElementType.TEXT_INPUT, "entries", 60, false, "price_lists"),
                action(ADD_PRICE_ENTRY, "entries_actions", 10, "price_lists"),
                field(PRICE_ENTRY_TO_INACTIVATE, ScreenElementType.SELECT, "entries", 70, false, "price_lists"),
                action(INACTIVATE_PRICE_ENTRY, "entries_actions", 20, "price_lists"),
                action(ACTIVATE_PRICE_LIST, "lifecycle_actions", 10, "price_lists"),
                action(INACTIVATE_PRICE_LIST, "lifecycle_actions", 20, "price_lists")));
    }

    public static ScreenDefinition taxProfilesDefinition() {
        return definition(TAX_PROFILES, "tax_profiles", List.of(
                field(TAX_PROFILE_SEARCH_TEXT, ScreenElementType.TEXT_INPUT,
                        "search", 10, false, "tax_profiles"),
                field(TAX_PROFILE_SEARCH_STATE, ScreenElementType.SELECT,
                        "search", 20, false, "tax_profiles"),
                action(TAX_PROFILE_SEARCH, "search_actions", 10, "tax_profiles"),
                content(TAX_PROFILE_RESULTS, ScreenElementType.DATA_TABLE,
                        "results", 10, "tax_profiles"),
                action(SELECT_TAX_PROFILE, "row_actions", 10, "tax_profiles"),
                field(TAX_PROFILE_NEW_CODE, ScreenElementType.TEXT_INPUT,
                        "create", 10, true, "tax_profiles"),
                field(TAX_PROFILE_NEW_NAME, ScreenElementType.TEXT_INPUT,
                        "create", 20, true, "tax_profiles"),
                field(TAX_PROFILE_NEW_KIND, ScreenElementType.TEXT_INPUT,
                        "create", 30, true, "tax_profiles"),
                field(TAX_PROFILE_NEW_DESCRIPTION, ScreenElementType.TEXT_INPUT,
                        "create", 40, true, "tax_profiles"),
                field(TAX_PROFILE_NEW_VALID_FROM, ScreenElementType.TEXT_INPUT,
                        "create", 50, true, "tax_profiles"),
                field(TAX_PROFILE_NEW_VALID_UNTIL, ScreenElementType.TEXT_INPUT,
                        "create", 60, false, "tax_profiles"),
                action(REGISTER_TAX_PROFILE, "create_actions", 10, "tax_profiles"),
                field(TAX_PROFILE_REVISION_KIND, ScreenElementType.TEXT_INPUT,
                        "revision", 10, true, "tax_profiles"),
                field(TAX_PROFILE_REVISION_DESCRIPTION, ScreenElementType.TEXT_INPUT,
                        "revision", 20, true, "tax_profiles"),
                field(TAX_PROFILE_REVISION_VALID_FROM, ScreenElementType.TEXT_INPUT,
                        "revision", 30, true, "tax_profiles"),
                field(TAX_PROFILE_REVISION_VALID_UNTIL, ScreenElementType.TEXT_INPUT,
                        "revision", 40, false, "tax_profiles"),
                action(REVISE_TAX_PROFILE, "revision_actions", 10, "tax_profiles"),
                content(TAX_PROFILE_HISTORY, ScreenElementType.DATA_TABLE,
                        "history", 10, "tax_profiles"),
                action(ACTIVATE_TAX_PROFILE, "lifecycle_actions", 10, "tax_profiles"),
                action(INACTIVATE_TAX_PROFILE, "lifecycle_actions", 20, "tax_profiles")));
    }

    public static ScreenDefinition definitionsDefinition() {
        return definition(DEFINITIONS, "definitions", List.of(
                field(DEFINITION_SEARCH_TEXT, ScreenElementType.TEXT_INPUT,
                        "search", 10, false, "definitions"),
                field(DEFINITION_SEARCH_KIND, ScreenElementType.SELECT,
                        "search", 20, false, "definitions"),
                field(DEFINITION_SEARCH_STATE, ScreenElementType.SELECT,
                        "search", 30, false, "definitions"),
                action(DEFINITION_SEARCH, "search_actions", 10, "definitions"),
                content(DEFINITION_RESULTS, ScreenElementType.DATA_TABLE,
                        "results", 10, "definitions"),
                action(SELECT_DEFINITION, "row_actions", 10, "definitions"),
                field(DEFINITION_NEW_KIND, ScreenElementType.SELECT,
                        "create", 10, true, "definitions"),
                field(DEFINITION_NEW_CODE, ScreenElementType.TEXT_INPUT,
                        "create", 20, true, "definitions"),
                field(DEFINITION_NEW_NAME, ScreenElementType.TEXT_INPUT,
                        "create", 30, true, "definitions"),
                field(DEFINITION_UNIT_SCALE, ScreenElementType.SELECT,
                        "create", 40, false, "definitions"),
                field(DEFINITION_CATEGORY_PARENT, ScreenElementType.SELECT,
                        "create", 50, false, "definitions"),
                action(REGISTER_DEFINITION, "create_actions", 10, "definitions"),
                field(DEFINITION_REVISION_NAME, ScreenElementType.TEXT_INPUT,
                        "revision", 10, true, "definitions"),
                field(DEFINITION_REVISION_UNIT_SCALE, ScreenElementType.SELECT,
                        "revision", 20, false, "definitions"),
                field(DEFINITION_REVISION_CATEGORY_PARENT, ScreenElementType.SELECT,
                        "revision", 30, false, "definitions"),
                action(REVISE_DEFINITION, "revision_actions", 10, "definitions"),
                content(DEFINITION_HISTORY, ScreenElementType.DATA_TABLE,
                        "history", 10, "definitions"),
                field(DEFINITION_REPLACEMENT_CODE, ScreenElementType.TEXT_INPUT,
                        "replacement", 10, true, "definitions"),
                field(DEFINITION_REPLACEMENT_NAME, ScreenElementType.TEXT_INPUT,
                        "replacement", 20, true, "definitions"),
                field(DEFINITION_REPLACEMENT_UNIT_SCALE, ScreenElementType.SELECT,
                        "replacement", 30, false, "definitions"),
                field(DEFINITION_REPLACEMENT_CATEGORY_PARENT, ScreenElementType.SELECT,
                        "replacement", 40, false, "definitions"),
                action(REPLACE_DEFINITION, "replacement_actions", 10, "definitions"),
                action(ACTIVATE_DEFINITION, "lifecycle_actions", 10, "definitions"),
                action(INACTIVATE_DEFINITION, "lifecycle_actions", 20, "definitions")));
    }

    public static ScreenDefinition variantFamiliesDefinition() {
        return definition(VARIANT_FAMILIES, "variant_families", List.of(
                field(VARIANT_FAMILY_SEARCH_TEXT, ScreenElementType.TEXT_INPUT,
                        "search", 10, false, "variant_families"),
                field(VARIANT_FAMILY_SEARCH_STATE, ScreenElementType.SELECT,
                        "search", 20, false, "variant_families"),
                action(VARIANT_FAMILY_SEARCH, "search_actions", 10, "variant_families"),
                content(VARIANT_FAMILY_RESULTS, ScreenElementType.DATA_TABLE,
                        "results", 10, "variant_families"),
                action(SELECT_VARIANT_FAMILY, "row_actions", 10, "variant_families"),
                field(VARIANT_FAMILY_NEW_CODE, ScreenElementType.TEXT_INPUT,
                        "create", 10, true, "variant_families"),
                field(VARIANT_FAMILY_NEW_NAME, ScreenElementType.TEXT_INPUT,
                        "create", 20, true, "variant_families"),
                field(VARIANT_ATTRIBUTE_CODE, ScreenElementType.TEXT_INPUT,
                        "create", 30, false, "variant_families"),
                field(VARIANT_ATTRIBUTE_NAME, ScreenElementType.TEXT_INPUT,
                        "create", 40, false, "variant_families"),
                field(VARIANT_ATTRIBUTE_TYPE, ScreenElementType.SELECT,
                        "create", 50, true, "variant_families"),
                field(VARIANT_ATTRIBUTE_REQUIRED, ScreenElementType.SELECT,
                        "create", 60, true, "variant_families"),
                content(VARIANT_ATTRIBUTE_DRAFT, ScreenElementType.DISPLAY_TEXT,
                        "create", 70, "variant_families"),
                action(ADD_VARIANT_ATTRIBUTE, "create_actions", 10, "variant_families"),
                action(REMOVE_VARIANT_ATTRIBUTE, "create_actions", 20, "variant_families"),
                action(REGISTER_VARIANT_FAMILY, "create_actions", 30, "variant_families"),
                field(VARIANT_FAMILY_REVISION_NAME, ScreenElementType.TEXT_INPUT,
                        "revision", 10, true, "variant_families"),
                field(VARIANT_REVISION_ATTRIBUTE_CODE, ScreenElementType.TEXT_INPUT,
                        "revision", 20, false, "variant_families"),
                field(VARIANT_REVISION_ATTRIBUTE_NAME, ScreenElementType.TEXT_INPUT,
                        "revision", 30, false, "variant_families"),
                field(VARIANT_REVISION_ATTRIBUTE_TYPE, ScreenElementType.SELECT,
                        "revision", 40, true, "variant_families"),
                field(VARIANT_REVISION_ATTRIBUTE_REQUIRED, ScreenElementType.SELECT,
                        "revision", 50, true, "variant_families"),
                content(VARIANT_REVISION_ATTRIBUTE_DRAFT, ScreenElementType.DISPLAY_TEXT,
                        "revision", 60, "variant_families"),
                action(ADD_VARIANT_REVISION_ATTRIBUTE,
                        "revision_actions", 10, "variant_families"),
                action(REMOVE_VARIANT_REVISION_ATTRIBUTE,
                        "revision_actions", 20, "variant_families"),
                action(REVISE_VARIANT_FAMILY,
                        "revision_actions", 30, "variant_families"),
                content(VARIANT_FAMILY_HISTORY, ScreenElementType.DATA_TABLE,
                        "history", 10, "variant_families"),
                action(ACTIVATE_VARIANT_FAMILY,
                        "lifecycle_actions", 10, "variant_families"),
                action(INACTIVATE_VARIANT_FAMILY,
                        "lifecycle_actions", 20, "variant_families")));
    }

    private static ScreenDefinition definition(
            ScreenId id, String localName, List<ScreenElementDefinition> elements) {
        return definition(id, localName, "1.0.0", elements);
    }

    private static ScreenDefinition definition(
            ScreenId id,
            String localName,
            String version,
            List<ScreenElementDefinition> elements) {
        return new ScreenDefinition(
                id,
                SemanticVersion.parse(version),
                elements,
                List.of(
                        new ScreenSlotDefinition(
                                DIRECTORY_EXTENSIONS,
                                new ScreenRegionId("directory_extensions"),
                                10,
                                2),
                        new ScreenSlotDefinition(
                                DETAIL_EXTENSIONS,
                                new ScreenRegionId("detail_extensions"),
                                10,
                                2)));
    }

    private static ScreenElementDefinition field(
            ScreenElementId id,
            ScreenElementType type,
            String region,
            int order,
            boolean required,
            String screen) {
        return element(id, type, region, order, required, FIELD_CHANGES, screen);
    }

    private static ScreenElementDefinition action(
            ScreenElementId id, String region, int order, String screen) {
        return element(id, ScreenElementType.ACTION, region, order, false, ACTION_CHANGES, screen);
    }

    private static ScreenElementDefinition content(
            ScreenElementId id,
            ScreenElementType type,
            String region,
            int order,
            String screen) {
        return element(id, type, region, order, false, CONTENT_CHANGES, screen);
    }

    private static ScreenElementDefinition element(
            ScreenElementId id,
            ScreenElementType type,
            String region,
            int order,
            boolean required,
            Set<ScreenCustomizationOperation> changes,
            String screen) {
        String key = "commercial_catalog." + screen + "." + id.value();
        return new ScreenElementDefinition(
                id,
                type,
                new ScreenRegionId(region),
                order,
                new ScreenTextKey(key + ".label"),
                Optional.of(new ScreenTextKey(key + ".help")),
                true,
                true,
                required,
                changes);
    }

    private static ScreenElementId id(String value) {
        return new ScreenElementId(value);
    }
}
