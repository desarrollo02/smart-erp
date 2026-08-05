package py.com.logixone.plugins.businesspartners;

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
import py.com.logixone.plugins.businesspartners.application.BusinessPartnersIdentity;

/** Stable public surface customized by company overlays and rendered by the shell. */
public final class BusinessPartnersScreenContract {

    public static final String ROUTE = "/business-partners";
    public static final String DEFINITIONS_ROUTE = "/business-partners/definitions";
    public static final ScreenId DIRECTORY = new ScreenId(BusinessPartnersIdentity.PLUGIN_ID, "directory");
    public static final ScreenId DEFINITIONS =
            new ScreenId(BusinessPartnersIdentity.PLUGIN_ID, "definitions");

    public static final ScreenElementId SEARCH_TEXT = id("search_text");
    public static final ScreenElementId SEARCH_ROLE = id("search_role");
    public static final ScreenElementId SEARCH_STATE = id("search_state");
    public static final ScreenElementId SEARCH = id("search");
    public static final ScreenElementId RESULTS = id("results");
    public static final ScreenElementId SELECT_PARTNER = id("select_partner");
    public static final ScreenElementId NEW_CODE = id("new_code");
    public static final ScreenElementId NEW_KIND = id("new_kind");
    public static final ScreenElementId NEW_DISPLAY_NAME = id("new_display_name");
    public static final ScreenElementId NEW_LEGAL_NAME = id("new_legal_name");
    public static final ScreenElementId NEW_TRADE_NAME = id("new_trade_name");
    public static final ScreenElementId REGISTER = id("register");
    public static final ScreenElementId EDIT_CODE = id("edit_code");
    public static final ScreenElementId EDIT_DISPLAY_NAME = id("edit_display_name");
    public static final ScreenElementId EDIT_LEGAL_NAME = id("edit_legal_name");
    public static final ScreenElementId EDIT_TRADE_NAME = id("edit_trade_name");
    public static final ScreenElementId CHANGE_CODE = id("change_code");
    public static final ScreenElementId RENAME = id("rename");
    public static final ScreenElementId IDENTIFICATION_TYPE = id("identification_type");
    public static final ScreenElementId IDENTIFICATION_COUNTRY = id("identification_country");
    public static final ScreenElementId IDENTIFICATION_VALUE = id("identification_value");
    public static final ScreenElementId ADD_IDENTIFICATION = id("add_identification");
    public static final ScreenElementId ADDRESS_TYPE = id("address_type");
    public static final ScreenElementId ADDRESS_PURPOSE = id("address_purpose");
    public static final ScreenElementId ADDRESS_LINE = id("address_line");
    public static final ScreenElementId ADDRESS_LOCALITY = id("address_locality");
    public static final ScreenElementId ADD_ADDRESS = id("add_address");
    public static final ScreenElementId CHANNEL_KIND = id("channel_kind");
    public static final ScreenElementId CHANNEL_VALUE = id("channel_value");
    public static final ScreenElementId ADD_CHANNEL = id("add_channel");
    public static final ScreenElementId CONTACT_NAME = id("contact_name");
    public static final ScreenElementId CONTACT_POSITION = id("contact_position");
    public static final ScreenElementId ADD_CONTACT = id("add_contact");
    public static final ScreenElementId ASSIGN_CLIENT = id("assign_client");
    public static final ScreenElementId ASSIGN_SUPPLIER = id("assign_supplier");
    public static final ScreenElementId ACTIVATE_CLIENT = id("activate_client");
    public static final ScreenElementId DEACTIVATE_CLIENT = id("deactivate_client");
    public static final ScreenElementId ACTIVATE_SUPPLIER = id("activate_supplier");
    public static final ScreenElementId DEACTIVATE_SUPPLIER = id("deactivate_supplier");
    public static final ScreenElementId DEACTIVATE_PARTNER = id("deactivate_partner");
    public static final ScreenElementId REACTIVATE_PARTNER = id("reactivate_partner");

    public static final ScreenElementId DEFINITION_KIND = id("definition_kind");
    public static final ScreenElementId DEFINITION_SEARCH_TEXT = id("definition_search_text");
    public static final ScreenElementId DEFINITION_SEARCH_STATE = id("definition_search_state");
    public static final ScreenElementId DEFINITION_SEARCH = id("definition_search");
    public static final ScreenElementId DEFINITION_RESULTS = id("definition_results");
    public static final ScreenElementId DEFINITION_HISTORY = id("definition_history");
    public static final ScreenElementId SELECT_DEFINITION = id("select_definition");
    public static final ScreenElementId DEFINITION_NEW_KIND = id("definition_new_kind");
    public static final ScreenElementId DEFINITION_NEW_CODE = id("definition_new_code");
    public static final ScreenElementId DEFINITION_NEW_NAME = id("definition_new_name");
    public static final ScreenElementId REGISTER_DEFINITION = id("register_definition");
    public static final ScreenElementId DEFINITION_EDIT_NAME = id("definition_edit_name");
    public static final ScreenElementId REVISE_DEFINITION = id("revise_definition");
    public static final ScreenElementId ACTIVATE_DEFINITION = id("activate_definition");
    public static final ScreenElementId INACTIVATE_DEFINITION = id("inactivate_definition");

    public static final ScreenSlotId DIRECTORY_EXTENSIONS = new ScreenSlotId("directory_extensions");
    public static final ScreenSlotId DETAIL_EXTENSIONS = new ScreenSlotId("detail_extensions");

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

    private BusinessPartnersScreenContract() {
    }

    public static ScreenDefinition definition() {
        return new ScreenDefinition(
                DIRECTORY,
                SemanticVersion.parse("1.2.0"),
                List.of(
                        field(SEARCH_TEXT, ScreenElementType.TEXT_INPUT, "search", 10, false),
                        field(SEARCH_ROLE, ScreenElementType.SELECT, "search", 20, false),
                        field(SEARCH_STATE, ScreenElementType.SELECT, "search", 30, false),
                        action(SEARCH, "search_actions", 10),
                        content(RESULTS, ScreenElementType.DATA_TABLE, "results", 10),
                        action(SELECT_PARTNER, "row_actions", 10),
                        field(NEW_CODE, ScreenElementType.TEXT_INPUT, "create", 10, false),
                        field(NEW_KIND, ScreenElementType.SELECT, "create", 20, true),
                        field(NEW_DISPLAY_NAME, ScreenElementType.TEXT_INPUT, "create", 30, true),
                        field(NEW_LEGAL_NAME, ScreenElementType.TEXT_INPUT, "create", 40, false),
                        field(NEW_TRADE_NAME, ScreenElementType.TEXT_INPUT, "create", 50, false),
                        action(REGISTER, "create_actions", 10),
                        field(EDIT_CODE, ScreenElementType.TEXT_INPUT, "code", 10, true),
                        action(CHANGE_CODE, "code_actions", 10),
                        field(EDIT_DISPLAY_NAME, ScreenElementType.TEXT_INPUT, "names", 10, true),
                        field(EDIT_LEGAL_NAME, ScreenElementType.TEXT_INPUT, "names", 20, false),
                        field(EDIT_TRADE_NAME, ScreenElementType.TEXT_INPUT, "names", 30, false),
                        action(RENAME, "names_actions", 10),
                        field(IDENTIFICATION_TYPE, ScreenElementType.SELECT, "identification", 10, true),
                        field(IDENTIFICATION_COUNTRY, ScreenElementType.SELECT, "identification", 20, false),
                        field(IDENTIFICATION_VALUE, ScreenElementType.TEXT_INPUT, "identification", 30, true),
                        action(ADD_IDENTIFICATION, "identification_actions", 10),
                        field(ADDRESS_TYPE, ScreenElementType.SELECT, "address", 10, true),
                        field(ADDRESS_PURPOSE, ScreenElementType.SELECT, "address", 20, true),
                        field(ADDRESS_LINE, ScreenElementType.TEXT_INPUT, "address", 30, true),
                        field(ADDRESS_LOCALITY, ScreenElementType.TEXT_INPUT, "address", 40, false),
                        action(ADD_ADDRESS, "address_actions", 10),
                        field(CHANNEL_KIND, ScreenElementType.SELECT, "channel", 10, true),
                        field(CHANNEL_VALUE, ScreenElementType.TEXT_INPUT, "channel", 20, true),
                        action(ADD_CHANNEL, "channel_actions", 10),
                        field(CONTACT_NAME, ScreenElementType.TEXT_INPUT, "contact", 10, true),
                        field(CONTACT_POSITION, ScreenElementType.TEXT_INPUT, "contact", 20, false),
                        action(ADD_CONTACT, "contact_actions", 10),
                        action(ASSIGN_CLIENT, "role_actions", 10),
                        action(ASSIGN_SUPPLIER, "role_actions", 20),
                        action(ACTIVATE_CLIENT, "role_actions", 30),
                        action(DEACTIVATE_CLIENT, "role_actions", 40),
                        action(ACTIVATE_SUPPLIER, "role_actions", 50),
                        action(DEACTIVATE_SUPPLIER, "role_actions", 60),
                        action(DEACTIVATE_PARTNER, "lifecycle_actions", 10),
                        action(REACTIVATE_PARTNER, "lifecycle_actions", 20)),
                List.of(
                        new ScreenSlotDefinition(
                                DIRECTORY_EXTENSIONS, new ScreenRegionId("directory_extensions"), 10, 2),
                        new ScreenSlotDefinition(
                                DETAIL_EXTENSIONS, new ScreenRegionId("detail_extensions"), 10, 2)));
    }

    public static ScreenDefinition definitions() {
        return new ScreenDefinition(
                DEFINITIONS,
                SemanticVersion.parse("1.1.0"),
                List.of(
                        definitionField(
                                DEFINITION_KIND, ScreenElementType.SELECT,
                                "search", 10, true),
                        definitionField(
                                DEFINITION_SEARCH_TEXT, ScreenElementType.TEXT_INPUT,
                                "search", 20, false),
                        definitionField(
                                DEFINITION_SEARCH_STATE, ScreenElementType.SELECT,
                                "search", 30, false),
                        definitionAction(DEFINITION_SEARCH, "search_actions", 10),
                        definitionContent(
                                DEFINITION_RESULTS, ScreenElementType.DATA_TABLE,
                                "results", 10),
                        definitionContent(
                                DEFINITION_HISTORY, ScreenElementType.DATA_TABLE,
                                "history", 10),
                        definitionAction(SELECT_DEFINITION, "row_actions", 10),
                        definitionField(
                                DEFINITION_NEW_KIND, ScreenElementType.SELECT,
                                "create", 10, true),
                        definitionField(
                                DEFINITION_NEW_CODE, ScreenElementType.TEXT_INPUT,
                                "create", 20, true),
                        definitionField(
                                DEFINITION_NEW_NAME, ScreenElementType.TEXT_INPUT,
                                "create", 30, true),
                        definitionAction(REGISTER_DEFINITION, "create_actions", 10),
                        definitionField(
                                DEFINITION_EDIT_NAME, ScreenElementType.TEXT_INPUT,
                                "revision", 10, true),
                        definitionAction(REVISE_DEFINITION, "revision_actions", 10),
                        definitionAction(ACTIVATE_DEFINITION, "lifecycle_actions", 10),
                        definitionAction(INACTIVATE_DEFINITION, "lifecycle_actions", 20)),
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
            ScreenElementId id, ScreenElementType type, String region, int order, boolean required) {
        return element(id, type, region, order, required, FIELD_CHANGES, "directory");
    }

    private static ScreenElementDefinition action(ScreenElementId id, String region, int order) {
        return element(id, ScreenElementType.ACTION, region, order, false, ACTION_CHANGES, "directory");
    }

    private static ScreenElementDefinition content(
            ScreenElementId id, ScreenElementType type, String region, int order) {
        return element(id, type, region, order, false, CONTENT_CHANGES, "directory");
    }

    private static ScreenElementDefinition definitionField(
            ScreenElementId id, ScreenElementType type, String region, int order, boolean required) {
        return element(id, type, region, order, required, FIELD_CHANGES, "definitions");
    }

    private static ScreenElementDefinition definitionAction(
            ScreenElementId id, String region, int order) {
        return element(id, ScreenElementType.ACTION, region, order, false, ACTION_CHANGES, "definitions");
    }

    private static ScreenElementDefinition definitionContent(
            ScreenElementId id, ScreenElementType type, String region, int order) {
        return element(id, type, region, order, false, CONTENT_CHANGES, "definitions");
    }

    private static ScreenElementDefinition element(
            ScreenElementId id,
            ScreenElementType type,
            String region,
            int order,
            boolean required,
            Set<ScreenCustomizationOperation> changes,
            String screen) {
        String key = "business_partners." + screen + "." + id.value();
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
