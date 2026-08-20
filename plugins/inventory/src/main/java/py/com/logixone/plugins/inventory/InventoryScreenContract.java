package py.com.logixone.plugins.inventory;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import py.com.logixone.plugin.api.ScreenActionDefinition;
import py.com.logixone.plugin.api.ScreenActionEmphasis;
import py.com.logixone.plugin.api.ScreenActionIntent;
import py.com.logixone.plugin.api.ScreenConfirmationMode;
import py.com.logixone.plugin.api.ScreenCustomizationOperation;
import py.com.logixone.plugin.api.ScreenDefinition;
import py.com.logixone.plugin.api.ScreenElementDefinition;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenElementType;
import py.com.logixone.plugin.api.ScreenExperienceDefinition;
import py.com.logixone.plugin.api.ScreenId;
import py.com.logixone.plugin.api.ScreenPurpose;
import py.com.logixone.plugin.api.ScreenRegionDefinition;
import py.com.logixone.plugin.api.ScreenRegionId;
import py.com.logixone.plugin.api.ScreenRegionRole;
import py.com.logixone.plugin.api.ScreenSemanticType;
import py.com.logixone.plugin.api.ScreenSlotDefinition;
import py.com.logixone.plugin.api.ScreenSlotId;
import py.com.logixone.plugin.api.ScreenTextKey;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugins.inventory.application.InventoryIdentity;

/** Stable inventory UI contracts rendered exclusively by the shell. */
public final class InventoryScreenContract {
    public static final String STOCK_ROUTE = "/inventory";
    public static final String WAREHOUSES_ROUTE = "/inventory/warehouses";
    public static final String COUNTS_ROUTE = "/inventory/counts";

    public static final ScreenId STOCK = new ScreenId(InventoryIdentity.PLUGIN_ID, "stock");
    public static final ScreenId WAREHOUSES = new ScreenId(InventoryIdentity.PLUGIN_ID, "warehouses");
    public static final ScreenId COUNTS = new ScreenId(InventoryIdentity.PLUGIN_ID, "counts");

    public static final ScreenSlotId DIRECTORY_EXTENSIONS = new ScreenSlotId("directory_extensions");
    public static final ScreenSlotId DETAIL_EXTENSIONS = new ScreenSlotId("detail_extensions");

    public static final ScreenElementId WAREHOUSE_SEARCH_TEXT = id("warehouse_search_text");
    public static final ScreenElementId WAREHOUSE_SEARCH_STATE = id("warehouse_search_state");
    public static final ScreenElementId WAREHOUSE_SEARCH = id("warehouse_search");
    public static final ScreenElementId WAREHOUSE_RESULTS = id("warehouse_results");
    public static final ScreenElementId SELECT_WAREHOUSE = id("select_warehouse");
    public static final ScreenElementId WAREHOUSE_NEW_CODE = id("warehouse_new_code");
    public static final ScreenElementId WAREHOUSE_NEW_NAME = id("warehouse_new_name");
    public static final ScreenElementId OPEN_WAREHOUSE = id("open_warehouse");
    public static final ScreenElementId WAREHOUSE_EDIT_NAME = id("warehouse_edit_name");
    public static final ScreenElementId RENAME_WAREHOUSE = id("rename_warehouse");
    public static final ScreenElementId LOCATION_NEW_CODE = id("location_new_code");
    public static final ScreenElementId LOCATION_NEW_NAME = id("location_new_name");
    public static final ScreenElementId LOCATION_NEW_TYPE = id("location_new_type");
    public static final ScreenElementId ADD_LOCATION = id("add_location");
    public static final ScreenElementId LOCATION_TO_RENAME = id("location_to_rename");
    public static final ScreenElementId LOCATION_EDIT_NAME = id("location_edit_name");
    public static final ScreenElementId RENAME_LOCATION = id("rename_location");
    public static final ScreenElementId LOCATION_TO_INACTIVATE = id("location_to_inactivate");
    public static final ScreenElementId INACTIVATE_LOCATION = id("inactivate_location");
    public static final ScreenElementId INACTIVATE_WAREHOUSE = id("inactivate_warehouse");

    public static final ScreenElementId STOCK_SEARCH_TEXT = id("stock_search_text");
    public static final ScreenElementId STOCK_SEARCH_STATE = id("stock_search_state");
    public static final ScreenElementId STOCK_SEARCH = id("stock_search");
    public static final ScreenElementId STOCK_RESULTS = id("stock_results");
    public static final ScreenElementId SELECT_STOCK_ITEM = id("select_stock_item");
    public static final ScreenElementId STOCK_TASK = id("stock_task");
    public static final ScreenElementId APPLY_STOCK_TASK = id("apply_stock_task");
    public static final ScreenElementId STOCK_GUIDANCE = id("stock_guidance");
    public static final ScreenElementId STOCK_SUMMARY = id("stock_summary");
    public static final ScreenElementId STOCK_NEW_CATALOG_ITEM = id("stock_new_catalog_item");
    public static final ScreenElementId STOCK_NEW_TRACKING = id("stock_new_tracking");
    public static final ScreenElementId STOCK_NEW_EXPIRY = id("stock_new_expiry");
    public static final ScreenElementId ENROLL_STOCK_ITEM = id("enroll_stock_item");
    public static final ScreenElementId AVAILABILITY_WAREHOUSE = id("availability_warehouse");
    public static final ScreenElementId AVAILABILITY_LOCATION = id("availability_location");
    public static final ScreenElementId AVAILABILITY_CONDITION = id("availability_condition");
    public static final ScreenElementId AVAILABILITY_LOT = id("availability_lot");
    public static final ScreenElementId AVAILABILITY_SERIAL = id("availability_serial");
    public static final ScreenElementId AVAILABILITY_EXPIRY = id("availability_expiry");
    public static final ScreenElementId CHECK_AVAILABILITY = id("check_availability");
    public static final ScreenElementId MOVEMENT_TYPE = id("movement_type");
    public static final ScreenElementId MOVEMENT_ITEM = id("movement_item");
    public static final ScreenElementId MOVEMENT_WAREHOUSE = id("movement_warehouse");
    public static final ScreenElementId MOVEMENT_LOCATION = id("movement_location");
    public static final ScreenElementId MOVEMENT_TARGET_WAREHOUSE = id("movement_target_warehouse");
    public static final ScreenElementId MOVEMENT_TARGET_LOCATION = id("movement_target_location");
    public static final ScreenElementId MOVEMENT_CONDITION = id("movement_condition");
    public static final ScreenElementId MOVEMENT_LOT = id("movement_lot");
    public static final ScreenElementId MOVEMENT_SERIAL = id("movement_serial");
    public static final ScreenElementId MOVEMENT_EXPIRY = id("movement_expiry");
    public static final ScreenElementId MOVEMENT_QUANTITY = id("movement_quantity");
    public static final ScreenElementId MOVEMENT_REASON = id("movement_reason");
    public static final ScreenElementId MOVEMENT_SOURCE_TYPE = id("movement_source_type");
    public static final ScreenElementId MOVEMENT_SOURCE_ID = id("movement_source_id");
    public static final ScreenElementId MOVEMENT_IDEMPOTENCY = id("movement_idempotency");
    public static final ScreenElementId POST_MOVEMENT = id("post_movement");
    public static final ScreenElementId MOVEMENT_GUIDANCE = id("movement_guidance");
    public static final ScreenElementId MOVEMENT_SUMMARY = id("movement_summary");
    public static final ScreenElementId RESERVATION_WAREHOUSE = id("reservation_warehouse");
    public static final ScreenElementId RESERVATION_LOCATION = id("reservation_location");
    public static final ScreenElementId RESERVATION_CONDITION = id("reservation_condition");
    public static final ScreenElementId RESERVATION_LOT = id("reservation_lot");
    public static final ScreenElementId RESERVATION_SERIAL = id("reservation_serial");
    public static final ScreenElementId RESERVATION_EXPIRY_DATE = id("reservation_expiry_date");
    public static final ScreenElementId RESERVATION_QUANTITY = id("reservation_quantity");
    public static final ScreenElementId RESERVATION_EXPIRES_AT = id("reservation_expires_at");
    public static final ScreenElementId RESERVATION_SOURCE_TYPE = id("reservation_source_type");
    public static final ScreenElementId RESERVATION_SOURCE_ID = id("reservation_source_id");
    public static final ScreenElementId RESERVATION_IDEMPOTENCY = id("reservation_idempotency");
    public static final ScreenElementId CREATE_RESERVATION = id("create_reservation");
    public static final ScreenElementId MANAGE_RESERVATION_ID = id("manage_reservation_id");
    public static final ScreenElementId MANAGE_RESERVATION_VERSION = id("manage_reservation_version");
    public static final ScreenElementId MANAGE_RESERVATION_QUANTITY = id("manage_reservation_quantity");
    public static final ScreenElementId MANAGE_RESERVATION_IDEMPOTENCY = id("manage_reservation_idempotency");
    public static final ScreenElementId CONSUME_RESERVATION = id("consume_reservation");
    public static final ScreenElementId RELEASE_RESERVATION = id("release_reservation");
    public static final ScreenElementId EXPIRE_RESERVATION = id("expire_reservation");
    public static final ScreenElementId REFRESH_STOCK_ITEM = id("refresh_stock_item");
    public static final ScreenElementId INACTIVATE_STOCK_ITEM = id("inactivate_stock_item");

    public static final ScreenElementId COUNT_SEARCH_STATE = id("count_search_state");
    public static final ScreenElementId COUNT_SEARCH = id("count_search");
    public static final ScreenElementId COUNT_RESULTS = id("count_results");
    public static final ScreenElementId SELECT_COUNT = id("select_count");
    public static final ScreenElementId COUNT_NEW_WAREHOUSE = id("count_new_warehouse");
    public static final ScreenElementId COUNT_NEW_LOCATION = id("count_new_location");
    public static final ScreenElementId DRAFT_COUNT = id("draft_count");
    public static final ScreenElementId COUNT_LINE_ITEM = id("count_line_item");
    public static final ScreenElementId COUNT_LINE_LOCATION = id("count_line_location");
    public static final ScreenElementId COUNT_LINE_CONDITION = id("count_line_condition");
    public static final ScreenElementId COUNT_LINE_LOT = id("count_line_lot");
    public static final ScreenElementId COUNT_LINE_SERIAL = id("count_line_serial");
    public static final ScreenElementId COUNT_LINE_EXPIRY = id("count_line_expiry");
    public static final ScreenElementId ADD_COUNT_LINE = id("add_count_line");
    public static final ScreenElementId COUNT_CAPTURE_LINE = id("count_capture_line");
    public static final ScreenElementId COUNT_CAPTURE_QUANTITY = id("count_capture_quantity");
    public static final ScreenElementId RECORD_COUNT = id("record_count");
    public static final ScreenElementId START_COUNT = id("start_count");
    public static final ScreenElementId REVIEW_COUNT = id("review_count");
    public static final ScreenElementId POST_COUNT = id("post_count");
    public static final ScreenElementId CANCEL_COUNT = id("cancel_count");

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

    private InventoryScreenContract() {
    }

    public static ScreenDefinition warehousesDefinition() {
        return definition(WAREHOUSES, "warehouses", List.of(
                field(WAREHOUSE_SEARCH_TEXT, ScreenElementType.TEXT_INPUT, "search", 10, false, "warehouses"),
                field(WAREHOUSE_SEARCH_STATE, ScreenElementType.SELECT, "search", 20, false, "warehouses"),
                action(WAREHOUSE_SEARCH, "search_actions", 10, "warehouses"),
                content(WAREHOUSE_RESULTS, ScreenElementType.DATA_TABLE, "results", 10, "warehouses"),
                action(SELECT_WAREHOUSE, "row_actions", 10, "warehouses"),
                field(WAREHOUSE_NEW_CODE, ScreenElementType.TEXT_INPUT, "create", 10, true, "warehouses"),
                field(WAREHOUSE_NEW_NAME, ScreenElementType.TEXT_INPUT, "create", 20, true, "warehouses"),
                action(OPEN_WAREHOUSE, "create_actions", 10, "warehouses"),
                field(WAREHOUSE_EDIT_NAME, ScreenElementType.TEXT_INPUT, "general", 10, true, "warehouses"),
                action(RENAME_WAREHOUSE, "general_actions", 10, "warehouses"),
                field(LOCATION_NEW_CODE, ScreenElementType.TEXT_INPUT, "locations", 10, true, "warehouses"),
                field(LOCATION_NEW_NAME, ScreenElementType.TEXT_INPUT, "locations", 20, true, "warehouses"),
                field(LOCATION_NEW_TYPE, ScreenElementType.SELECT, "locations", 30, true, "warehouses"),
                field(LOCATION_TO_RENAME, ScreenElementType.SELECT, "locations", 40, false, "warehouses"),
                field(LOCATION_EDIT_NAME, ScreenElementType.TEXT_INPUT, "locations", 50, false, "warehouses"),
                field(LOCATION_TO_INACTIVATE, ScreenElementType.SELECT, "locations", 60, false, "warehouses"),
                action(ADD_LOCATION, "locations_actions", 10, "warehouses"),
                action(RENAME_LOCATION, "locations_actions", 20, "warehouses"),
                action(INACTIVATE_LOCATION, "locations_actions", 30, "warehouses"),
                action(INACTIVATE_WAREHOUSE, "lifecycle_actions", 10, "warehouses")));
    }

    public static ScreenDefinition stockDefinition() {
        List<ScreenElementDefinition> elements = List.of(
                field(STOCK_TASK, ScreenElementType.SELECT, "context", 10, true, "stock"),
                field(MOVEMENT_ITEM, ScreenElementType.SELECT, "context", 20, false, "movements"),
                field(MOVEMENT_TYPE, ScreenElementType.SELECT, "context", 30, true, "movements"),
                action(APPLY_STOCK_TASK, "context", 40, "stock"),
                field(STOCK_NEW_CATALOG_ITEM, ScreenElementType.SELECT, "content", 10, true, "stock"),
                field(STOCK_NEW_TRACKING, ScreenElementType.SELECT, "content", 20, true, "stock"),
                field(STOCK_NEW_EXPIRY, ScreenElementType.SELECT, "content", 30, true, "stock"),
                field(AVAILABILITY_WAREHOUSE, ScreenElementType.SELECT, "content", 40, true, "stock"),
                field(AVAILABILITY_LOCATION, ScreenElementType.SELECT, "content", 50, true, "stock"),
                field(AVAILABILITY_CONDITION, ScreenElementType.SELECT, "content", 60, true, "stock"),
                field(AVAILABILITY_LOT, ScreenElementType.TEXT_INPUT, "content", 70, false, "stock"),
                field(AVAILABILITY_SERIAL, ScreenElementType.TEXT_INPUT, "content", 80, false, "stock"),
                field(AVAILABILITY_EXPIRY, ScreenElementType.TEXT_INPUT, "content", 90, false, "stock"),
                field(MOVEMENT_WAREHOUSE, ScreenElementType.SELECT, "content", 20, true, "movements"),
                field(MOVEMENT_LOCATION, ScreenElementType.SELECT, "content", 30, true, "movements"),
                field(MOVEMENT_TARGET_WAREHOUSE, ScreenElementType.SELECT, "content", 40, false, "movements"),
                field(MOVEMENT_TARGET_LOCATION, ScreenElementType.SELECT, "content", 50, false, "movements"),
                field(MOVEMENT_CONDITION, ScreenElementType.SELECT, "content", 60, true, "movements"),
                field(MOVEMENT_LOT, ScreenElementType.TEXT_INPUT, "content", 70, false, "movements"),
                field(MOVEMENT_SERIAL, ScreenElementType.TEXT_INPUT, "content", 80, false, "movements"),
                field(MOVEMENT_EXPIRY, ScreenElementType.TEXT_INPUT, "content", 90, false, "movements"),
                field(MOVEMENT_QUANTITY, ScreenElementType.TEXT_INPUT, "content", 100, true, "movements"),
                field(MOVEMENT_REASON, ScreenElementType.TEXT_INPUT, "content", 110, true, "movements"),
                field(MOVEMENT_IDEMPOTENCY, ScreenElementType.TEXT_INPUT, "content", 120, true, "movements"),
                field(RESERVATION_WAREHOUSE, ScreenElementType.SELECT, "content", 130, true, "stock"),
                field(RESERVATION_LOCATION, ScreenElementType.SELECT, "content", 140, true, "stock"),
                field(RESERVATION_CONDITION, ScreenElementType.SELECT, "content", 150, true, "stock"),
                field(RESERVATION_LOT, ScreenElementType.TEXT_INPUT, "content", 160, false, "stock"),
                field(RESERVATION_SERIAL, ScreenElementType.TEXT_INPUT, "content", 170, false, "stock"),
                field(RESERVATION_EXPIRY_DATE, ScreenElementType.TEXT_INPUT, "content", 180, false, "stock"),
                field(RESERVATION_QUANTITY, ScreenElementType.TEXT_INPUT, "content", 190, true, "stock"),
                field(RESERVATION_EXPIRES_AT, ScreenElementType.TEXT_INPUT, "content", 200, true, "stock"),
                field(RESERVATION_IDEMPOTENCY, ScreenElementType.TEXT_INPUT, "content", 210, true, "stock"),
                field(MANAGE_RESERVATION_ID, ScreenElementType.TEXT_INPUT, "content", 220, true, "stock"),
                field(MANAGE_RESERVATION_VERSION, ScreenElementType.TEXT_INPUT, "content", 230, true, "stock"),
                field(MANAGE_RESERVATION_QUANTITY, ScreenElementType.TEXT_INPUT, "content", 240, false, "stock"),
                field(MANAGE_RESERVATION_IDEMPOTENCY, ScreenElementType.TEXT_INPUT, "content", 250, true, "stock"),
                content(STOCK_GUIDANCE, ScreenElementType.DISPLAY_TEXT, "guidance", 10, "stock"),
                content(STOCK_SUMMARY, ScreenElementType.DISPLAY_TEXT, "summary", 10, "stock"),
                action(ENROLL_STOCK_ITEM, "actions", 10, "stock"),
                action(CHECK_AVAILABILITY, "actions", 20, "stock"),
                action(POST_MOVEMENT, "actions", 30, "stock"),
                action(CREATE_RESERVATION, "actions", 40, "stock"),
                action(CONSUME_RESERVATION, "actions", 50, "stock"),
                action(RELEASE_RESERVATION, "actions", 60, "stock"),
                action(EXPIRE_RESERVATION, "actions", 70, "stock"),
                action(REFRESH_STOCK_ITEM, "actions", 80, "stock"),
                action(INACTIVATE_STOCK_ITEM, "actions", 90, "stock"));
        return new ScreenDefinition(
                STOCK,
                SemanticVersion.parse("2.0.0"),
                elements,
                List.of(),
                Optional.of(new ScreenExperienceDefinition(
                        ScreenPurpose.GUIDED_OPERATION,
                        List.of(
                                region("context", ScreenRegionRole.CONTEXT, 10),
                                region("content", ScreenRegionRole.CONTENT, 20),
                                region("guidance", ScreenRegionRole.GUIDANCE, 30),
                                region("summary", ScreenRegionRole.SUMMARY, 40),
                                region("actions", ScreenRegionRole.ACTIONS, 50)),
                        Map.ofEntries(
                                semantic(STOCK_TASK, ScreenSemanticType.STATUS),
                                semantic(MOVEMENT_ITEM, ScreenSemanticType.SEARCHABLE_REFERENCE),
                                semantic(STOCK_NEW_CATALOG_ITEM, ScreenSemanticType.SEARCHABLE_REFERENCE),
                                semantic(STOCK_NEW_TRACKING, ScreenSemanticType.STATUS),
                                semantic(STOCK_NEW_EXPIRY, ScreenSemanticType.STATUS),
                                semantic(AVAILABILITY_WAREHOUSE, ScreenSemanticType.SEARCHABLE_REFERENCE),
                                semantic(AVAILABILITY_LOCATION, ScreenSemanticType.SEARCHABLE_REFERENCE),
                                semantic(AVAILABILITY_CONDITION, ScreenSemanticType.STATUS),
                                semantic(AVAILABILITY_LOT, ScreenSemanticType.TEXT),
                                semantic(AVAILABILITY_SERIAL, ScreenSemanticType.TEXT),
                                semantic(AVAILABILITY_EXPIRY, ScreenSemanticType.DATE),
                                semantic(MOVEMENT_TYPE, ScreenSemanticType.STATUS),
                                semantic(MOVEMENT_WAREHOUSE, ScreenSemanticType.SEARCHABLE_REFERENCE),
                                semantic(MOVEMENT_LOCATION, ScreenSemanticType.SEARCHABLE_REFERENCE),
                                semantic(MOVEMENT_TARGET_WAREHOUSE, ScreenSemanticType.SEARCHABLE_REFERENCE),
                                semantic(MOVEMENT_TARGET_LOCATION, ScreenSemanticType.SEARCHABLE_REFERENCE),
                                semantic(MOVEMENT_CONDITION, ScreenSemanticType.STATUS),
                                semantic(MOVEMENT_LOT, ScreenSemanticType.TEXT),
                                semantic(MOVEMENT_SERIAL, ScreenSemanticType.TEXT),
                                semantic(MOVEMENT_EXPIRY, ScreenSemanticType.DATE),
                                semantic(MOVEMENT_QUANTITY, ScreenSemanticType.QUANTITY),
                                semantic(MOVEMENT_REASON, ScreenSemanticType.TEXT),
                                semantic(MOVEMENT_IDEMPOTENCY, ScreenSemanticType.TECHNICAL_TOKEN),
                                semantic(RESERVATION_WAREHOUSE, ScreenSemanticType.SEARCHABLE_REFERENCE),
                                semantic(RESERVATION_LOCATION, ScreenSemanticType.SEARCHABLE_REFERENCE),
                                semantic(RESERVATION_CONDITION, ScreenSemanticType.STATUS),
                                semantic(RESERVATION_LOT, ScreenSemanticType.TEXT),
                                semantic(RESERVATION_SERIAL, ScreenSemanticType.TEXT),
                                semantic(RESERVATION_EXPIRY_DATE, ScreenSemanticType.DATE),
                                semantic(RESERVATION_QUANTITY, ScreenSemanticType.QUANTITY),
                                semantic(RESERVATION_EXPIRES_AT, ScreenSemanticType.DATE),
                                semantic(RESERVATION_IDEMPOTENCY, ScreenSemanticType.TECHNICAL_TOKEN),
                                semantic(MANAGE_RESERVATION_ID, ScreenSemanticType.TECHNICAL_TOKEN),
                                semantic(MANAGE_RESERVATION_VERSION, ScreenSemanticType.TECHNICAL_TOKEN),
                                semantic(MANAGE_RESERVATION_QUANTITY, ScreenSemanticType.QUANTITY),
                                semantic(MANAGE_RESERVATION_IDEMPOTENCY, ScreenSemanticType.TECHNICAL_TOKEN),
                                semantic(STOCK_GUIDANCE, ScreenSemanticType.SUMMARY),
                                semantic(STOCK_SUMMARY, ScreenSemanticType.SUMMARY)),
                        List.of(
                                actionDefinition(APPLY_STOCK_TASK, ScreenActionIntent.NAVIGATE,
                                        ScreenActionEmphasis.SECONDARY, ScreenConfirmationMode.NONE),
                                actionDefinition(ENROLL_STOCK_ITEM, ScreenActionIntent.CREATE,
                                        ScreenActionEmphasis.PRIMARY, ScreenConfirmationMode.ACKNOWLEDGEMENT),
                                actionDefinition(CHECK_AVAILABILITY, ScreenActionIntent.SEARCH,
                                        ScreenActionEmphasis.SECONDARY, ScreenConfirmationMode.NONE),
                                actionDefinition(POST_MOVEMENT, ScreenActionIntent.EXECUTE,
                                        ScreenActionEmphasis.PRIMARY, ScreenConfirmationMode.ACKNOWLEDGEMENT),
                                actionDefinition(CREATE_RESERVATION, ScreenActionIntent.EXECUTE,
                                        ScreenActionEmphasis.PRIMARY, ScreenConfirmationMode.ACKNOWLEDGEMENT),
                                actionDefinition(CONSUME_RESERVATION, ScreenActionIntent.EXECUTE,
                                        ScreenActionEmphasis.SECONDARY, ScreenConfirmationMode.ACKNOWLEDGEMENT),
                                actionDefinition(RELEASE_RESERVATION, ScreenActionIntent.EXECUTE,
                                        ScreenActionEmphasis.SECONDARY, ScreenConfirmationMode.ACKNOWLEDGEMENT),
                                actionDefinition(EXPIRE_RESERVATION, ScreenActionIntent.EXECUTE,
                                        ScreenActionEmphasis.DESTRUCTIVE, ScreenConfirmationMode.ACKNOWLEDGEMENT),
                                actionDefinition(REFRESH_STOCK_ITEM, ScreenActionIntent.EXECUTE,
                                        ScreenActionEmphasis.SECONDARY, ScreenConfirmationMode.ACKNOWLEDGEMENT),
                                actionDefinition(INACTIVATE_STOCK_ITEM, ScreenActionIntent.EXECUTE,
                                        ScreenActionEmphasis.DESTRUCTIVE, ScreenConfirmationMode.ACKNOWLEDGEMENT)))));
    }

    public static ScreenDefinition countsDefinition() {
        return definition(COUNTS, "counts", List.of(
                field(COUNT_SEARCH_STATE, ScreenElementType.SELECT, "search", 10, false, "counts"),
                action(COUNT_SEARCH, "search_actions", 10, "counts"),
                content(COUNT_RESULTS, ScreenElementType.DATA_TABLE, "results", 10, "counts"),
                action(SELECT_COUNT, "row_actions", 10, "counts"),
                field(COUNT_NEW_WAREHOUSE, ScreenElementType.SELECT, "create", 10, true, "counts"),
                field(COUNT_NEW_LOCATION, ScreenElementType.SELECT, "create", 20, false, "counts"),
                action(DRAFT_COUNT, "create_actions", 10, "counts"),
                field(COUNT_LINE_ITEM, ScreenElementType.SELECT, "lines", 10, true, "counts"),
                field(COUNT_LINE_LOCATION, ScreenElementType.SELECT, "lines", 20, true, "counts"),
                field(COUNT_LINE_CONDITION, ScreenElementType.SELECT, "lines", 30, true, "counts"),
                field(COUNT_LINE_LOT, ScreenElementType.TEXT_INPUT, "lines", 40, false, "counts"),
                field(COUNT_LINE_SERIAL, ScreenElementType.TEXT_INPUT, "lines", 50, false, "counts"),
                field(COUNT_LINE_EXPIRY, ScreenElementType.TEXT_INPUT, "lines", 60, false, "counts"),
                action(ADD_COUNT_LINE, "lines_actions", 10, "counts"),
                field(COUNT_CAPTURE_LINE, ScreenElementType.SELECT, "capture", 10, true, "counts"),
                field(COUNT_CAPTURE_QUANTITY, ScreenElementType.TEXT_INPUT, "capture", 20, true, "counts"),
                action(RECORD_COUNT, "capture_actions", 10, "counts"),
                action(START_COUNT, "lifecycle_actions", 10, "counts"),
                action(REVIEW_COUNT, "lifecycle_actions", 20, "counts"),
                action(POST_COUNT, "lifecycle_actions", 30, "counts"),
                action(CANCEL_COUNT, "lifecycle_actions", 40, "counts")));
    }

    private static ScreenDefinition definition(
            ScreenId id, String screen, List<ScreenElementDefinition> elements) {
        return new ScreenDefinition(
                id,
                SemanticVersion.parse("1.0.0"),
                elements,
                List.of(
                        new ScreenSlotDefinition(DIRECTORY_EXTENSIONS,
                                new ScreenRegionId("directory_extensions"), 10, 2),
                        new ScreenSlotDefinition(DETAIL_EXTENSIONS,
                                new ScreenRegionId("detail_extensions"), 10, 2)));
    }

    private static ScreenElementDefinition field(
            ScreenElementId id, ScreenElementType type, String region, int order,
            boolean required, String screen) {
        return element(id, type, region, order, required, FIELD_CHANGES, screen);
    }

    private static ScreenElementDefinition action(
            ScreenElementId id, String region, int order, String screen) {
        return element(id, ScreenElementType.ACTION, region, order, false, ACTION_CHANGES, screen);
    }

    private static ScreenElementDefinition content(
            ScreenElementId id, ScreenElementType type, String region, int order, String screen) {
        return element(id, type, region, order, false, CONTENT_CHANGES, screen);
    }

    private static ScreenElementDefinition element(
            ScreenElementId id, ScreenElementType type, String region, int order,
            boolean required, Set<ScreenCustomizationOperation> changes, String screen) {
        String key = "inventory." + screen + "." + id.value();
        return new ScreenElementDefinition(
                id, type, new ScreenRegionId(region), order,
                new ScreenTextKey(key + ".label"),
                Optional.of(new ScreenTextKey(key + ".help")),
                true, true, required, changes);
    }

    private static ScreenElementId id(String value) {
        return new ScreenElementId(value);
    }

    private static ScreenRegionDefinition region(
            String id, ScreenRegionRole role, int order) {
        return new ScreenRegionDefinition(new ScreenRegionId(id), role, order);
    }

    private static Map.Entry<ScreenElementId, ScreenSemanticType> semantic(
            ScreenElementId id, ScreenSemanticType type) {
        return Map.entry(id, type);
    }

    private static ScreenActionDefinition actionDefinition(
            ScreenElementId id,
            ScreenActionIntent intent,
            ScreenActionEmphasis emphasis,
            ScreenConfirmationMode confirmation) {
        return new ScreenActionDefinition(id, intent, emphasis, confirmation);
    }
}
