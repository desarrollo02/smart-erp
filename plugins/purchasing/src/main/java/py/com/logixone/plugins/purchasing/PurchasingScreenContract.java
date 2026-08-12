package py.com.logixone.plugins.purchasing;

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
import py.com.logixone.plugins.purchasing.application.PurchasingIdentity;

/** Stable purchasing UI contracts rendered exclusively by the shell. */
public final class PurchasingScreenContract {
    public static final String REQUESTS_ROUTE = "/purchasing/requests";
    public static final String ORDERS_ROUTE = "/purchasing/orders";
    public static final String RECEIPTS_ROUTE = "/purchasing/receipts";
    public static final String RETURNS_ROUTE = "/purchasing/returns";
    public static final String TRACKING_ROUTE = "/purchasing/tracking";

    public static final ScreenId REQUESTS = screen("requests");
    public static final ScreenId ORDERS = screen("orders");
    public static final ScreenId RECEIPTS = screen("receipts");
    public static final ScreenId RETURNS = screen("returns");
    public static final ScreenId TRACKING = screen("tracking");

    public static final ScreenSlotId DIRECTORY_EXTENSIONS =
            new ScreenSlotId("directory_extensions");
    public static final ScreenSlotId DETAIL_EXTENSIONS =
            new ScreenSlotId("detail_extensions");

    public static final ScreenElementId REQUEST_SEARCH_TEXT = id("request_search_text");
    public static final ScreenElementId REQUEST_SEARCH_STATE = id("request_search_state");
    public static final ScreenElementId REQUEST_SEARCH = id("request_search");
    public static final ScreenElementId REQUEST_RESULTS = id("request_results");
    public static final ScreenElementId SELECT_REQUEST = id("select_request");
    public static final ScreenElementId REQUEST_NUMBER = id("request_number");
    public static final ScreenElementId REQUEST_DATE = id("request_date");
    public static final ScreenElementId REQUEST_KIND = id("request_kind");
    public static final ScreenElementId REQUEST_ITEM = id("request_item");
    public static final ScreenElementId REQUEST_DESCRIPTION = id("request_description");
    public static final ScreenElementId REQUEST_UNIT = id("request_unit");
    public static final ScreenElementId REQUEST_QUANTITY = id("request_quantity");
    public static final ScreenElementId REQUEST_EXPECTED_PRICE = id("request_expected_price");
    public static final ScreenElementId REQUEST_CURRENCY = id("request_currency");
    public static final ScreenElementId CREATE_REQUEST = id("create_request");
    public static final ScreenElementId REQUEST_ADD_KIND = id("request_add_kind");
    public static final ScreenElementId REQUEST_ADD_ITEM = id("request_add_item");
    public static final ScreenElementId REQUEST_ADD_DESCRIPTION = id("request_add_description");
    public static final ScreenElementId REQUEST_ADD_UNIT = id("request_add_unit");
    public static final ScreenElementId REQUEST_ADD_QUANTITY = id("request_add_quantity");
    public static final ScreenElementId REQUEST_ADD_EXPECTED_PRICE = id("request_add_expected_price");
    public static final ScreenElementId REQUEST_ADD_CURRENCY = id("request_add_currency");
    public static final ScreenElementId ADD_REQUEST_LINE = id("add_request_line");
    public static final ScreenElementId REQUEST_REASON = id("request_reason");
    public static final ScreenElementId SUBMIT_REQUEST = id("submit_request");
    public static final ScreenElementId APPROVE_REQUEST = id("approve_request");
    public static final ScreenElementId REJECT_REQUEST = id("reject_request");
    public static final ScreenElementId CANCEL_REQUEST = id("cancel_request");
    public static final ScreenElementId REQUEST_CLONE_NUMBER = id("request_clone_number");
    public static final ScreenElementId REQUEST_CLONE_DATE = id("request_clone_date");
    public static final ScreenElementId CLONE_REQUEST = id("clone_request");

    public static final ScreenElementId ORDER_SEARCH_TEXT = id("order_search_text");
    public static final ScreenElementId ORDER_SEARCH_STATE = id("order_search_state");
    public static final ScreenElementId ORDER_SEARCH = id("order_search");
    public static final ScreenElementId ORDER_RESULTS = id("order_results");
    public static final ScreenElementId SELECT_ORDER = id("select_order");
    public static final ScreenElementId ORDER_NUMBER = id("order_number");
    public static final ScreenElementId ORDER_SUPPLIER = id("order_supplier");
    public static final ScreenElementId ORDER_CURRENCY = id("order_currency");
    public static final ScreenElementId ORDER_JUSTIFICATION = id("order_justification");
    public static final ScreenElementId ORDER_KIND = id("order_kind");
    public static final ScreenElementId ORDER_ITEM = id("order_item");
    public static final ScreenElementId ORDER_DESCRIPTION = id("order_description");
    public static final ScreenElementId ORDER_UNIT = id("order_unit");
    public static final ScreenElementId ORDER_QUANTITY = id("order_quantity");
    public static final ScreenElementId ORDER_PRICE = id("order_price");
    public static final ScreenElementId ORDER_REQUEST = id("order_request");
    public static final ScreenElementId ORDER_REQUEST_LINE = id("order_request_line");
    public static final ScreenElementId ORDER_ALLOCATION_QUANTITY = id("order_allocation_quantity");
    public static final ScreenElementId CREATE_ORDER = id("create_order");
    public static final ScreenElementId ORDER_ADD_KIND = id("order_add_kind");
    public static final ScreenElementId ORDER_ADD_ITEM = id("order_add_item");
    public static final ScreenElementId ORDER_ADD_DESCRIPTION = id("order_add_description");
    public static final ScreenElementId ORDER_ADD_UNIT = id("order_add_unit");
    public static final ScreenElementId ORDER_ADD_QUANTITY = id("order_add_quantity");
    public static final ScreenElementId ORDER_ADD_PRICE = id("order_add_price");
    public static final ScreenElementId ADD_ORDER_LINE = id("add_order_line");
    public static final ScreenElementId ORDER_REASON = id("order_reason");
    public static final ScreenElementId ISSUE_ORDER = id("issue_order");
    public static final ScreenElementId CANCEL_ORDER = id("cancel_order");
    public static final ScreenElementId CLOSE_ORDER_SHORT = id("close_order_short");

    public static final ScreenElementId RECEIPT_SEARCH_TEXT = id("receipt_search_text");
    public static final ScreenElementId RECEIPT_SEARCH_STATE = id("receipt_search_state");
    public static final ScreenElementId RECEIPT_SEARCH = id("receipt_search");
    public static final ScreenElementId RECEIPT_RESULTS = id("receipt_results");
    public static final ScreenElementId SELECT_RECEIPT = id("select_receipt");
    public static final ScreenElementId RECEIPT_NUMBER = id("receipt_number");
    public static final ScreenElementId RECEIPT_ORDER = id("receipt_order");
    public static final ScreenElementId RECEIPT_ORDER_LINE = id("receipt_order_line");
    public static final ScreenElementId RECEIPT_QUANTITY = id("receipt_quantity");
    public static final ScreenElementId RECEIPT_WAREHOUSE = id("receipt_warehouse");
    public static final ScreenElementId RECEIPT_LOCATION = id("receipt_location");
    public static final ScreenElementId RECEIPT_LOT = id("receipt_lot");
    public static final ScreenElementId RECEIPT_SERIAL = id("receipt_serial");
    public static final ScreenElementId RECEIPT_EXPIRY = id("receipt_expiry");
    public static final ScreenElementId RECEIPT_CONDITION = id("receipt_condition");
    public static final ScreenElementId CREATE_RECEIPT = id("create_receipt");
    public static final ScreenElementId CONFIRM_RECEIPT = id("confirm_receipt");

    public static final ScreenElementId RETURN_SEARCH_TEXT = id("return_search_text");
    public static final ScreenElementId RETURN_SEARCH_STATE = id("return_search_state");
    public static final ScreenElementId RETURN_SEARCH = id("return_search");
    public static final ScreenElementId RETURN_RESULTS = id("return_results");
    public static final ScreenElementId SELECT_RETURN = id("select_return");
    public static final ScreenElementId RETURN_NUMBER = id("return_number");
    public static final ScreenElementId RETURN_ORDER = id("return_order");
    public static final ScreenElementId RETURN_RECEIPT = id("return_receipt");
    public static final ScreenElementId RETURN_RECEIPT_LINE = id("return_receipt_line");
    public static final ScreenElementId RETURN_QUANTITY = id("return_quantity");
    public static final ScreenElementId RETURN_REASON = id("return_reason");
    public static final ScreenElementId CREATE_RETURN = id("create_return");
    public static final ScreenElementId CONFIRM_RETURN = id("confirm_return");

    public static final ScreenElementId TRACKING_SEARCH_TEXT = id("tracking_search_text");
    public static final ScreenElementId TRACKING_SEARCH_STATE = id("tracking_search_state");
    public static final ScreenElementId TRACKING_SEARCH = id("tracking_search");
    public static final ScreenElementId TRACKING_RESULTS = id("tracking_results");
    public static final ScreenElementId SELECT_TRACKING_ORDER = id("select_tracking_order");

    private static final Set<ScreenCustomizationOperation> FIELD_CHANGES = EnumSet.of(
            ScreenCustomizationOperation.CHANGE_LABEL, ScreenCustomizationOperation.CHANGE_HELP,
            ScreenCustomizationOperation.HIDE, ScreenCustomizationOperation.DISABLE,
            ScreenCustomizationOperation.REQUIRE, ScreenCustomizationOperation.REORDER);
    private static final Set<ScreenCustomizationOperation> ACTION_CHANGES = EnumSet.of(
            ScreenCustomizationOperation.CHANGE_LABEL, ScreenCustomizationOperation.CHANGE_HELP,
            ScreenCustomizationOperation.HIDE, ScreenCustomizationOperation.DISABLE,
            ScreenCustomizationOperation.REORDER);
    private static final Set<ScreenCustomizationOperation> CONTENT_CHANGES = EnumSet.of(
            ScreenCustomizationOperation.CHANGE_LABEL, ScreenCustomizationOperation.CHANGE_HELP,
            ScreenCustomizationOperation.HIDE, ScreenCustomizationOperation.REORDER);

    private PurchasingScreenContract() {
    }

    public static ScreenDefinition requestsDefinition() {
        return definition(REQUESTS, "requests", List.of(
                field(REQUEST_SEARCH_TEXT, ScreenElementType.TEXT_INPUT, "search", 10, false, "requests"),
                field(REQUEST_SEARCH_STATE, ScreenElementType.SELECT, "search", 20, false, "requests"),
                action(REQUEST_SEARCH, "search_actions", 10, "requests"),
                content(REQUEST_RESULTS, "results", 10, "requests"), action(SELECT_REQUEST, "row_actions", 10, "requests"),
                field(REQUEST_NUMBER, ScreenElementType.TEXT_INPUT, "create", 10, true, "requests"),
                field(REQUEST_DATE, ScreenElementType.TEXT_INPUT, "create", 20, true, "requests"),
                field(REQUEST_KIND, ScreenElementType.SELECT, "create", 30, true, "requests"),
                field(REQUEST_ITEM, ScreenElementType.SELECT, "create", 40, false, "requests"),
                field(REQUEST_DESCRIPTION, ScreenElementType.TEXT_INPUT, "create", 50, true, "requests"),
                field(REQUEST_UNIT, ScreenElementType.TEXT_INPUT, "create", 60, true, "requests"),
                field(REQUEST_QUANTITY, ScreenElementType.TEXT_INPUT, "create", 70, true, "requests"),
                field(REQUEST_EXPECTED_PRICE, ScreenElementType.TEXT_INPUT, "create", 80, false, "requests"),
                field(REQUEST_CURRENCY, ScreenElementType.SELECT, "create", 90, false, "requests"),
                action(CREATE_REQUEST, "create_actions", 10, "requests"),
                field(REQUEST_ADD_KIND, ScreenElementType.SELECT, "lines", 10, true, "requests"),
                field(REQUEST_ADD_ITEM, ScreenElementType.SELECT, "lines", 20, false, "requests"),
                field(REQUEST_ADD_DESCRIPTION, ScreenElementType.TEXT_INPUT, "lines", 30, true, "requests"),
                field(REQUEST_ADD_UNIT, ScreenElementType.TEXT_INPUT, "lines", 40, true, "requests"),
                field(REQUEST_ADD_QUANTITY, ScreenElementType.TEXT_INPUT, "lines", 50, true, "requests"),
                field(REQUEST_ADD_EXPECTED_PRICE, ScreenElementType.TEXT_INPUT, "lines", 60, false, "requests"),
                field(REQUEST_ADD_CURRENCY, ScreenElementType.SELECT, "lines", 70, false, "requests"),
                action(ADD_REQUEST_LINE, "lines_actions", 10, "requests"),
                field(REQUEST_REASON, ScreenElementType.TEXT_INPUT, "lifecycle", 10, false, "requests"),
                action(SUBMIT_REQUEST, "lifecycle_actions", 10, "requests"),
                action(APPROVE_REQUEST, "lifecycle_actions", 20, "requests"),
                action(REJECT_REQUEST, "lifecycle_actions", 30, "requests"),
                action(CANCEL_REQUEST, "lifecycle_actions", 40, "requests"),
                field(REQUEST_CLONE_NUMBER, ScreenElementType.TEXT_INPUT, "clone", 10, true, "requests"),
                field(REQUEST_CLONE_DATE, ScreenElementType.TEXT_INPUT, "clone", 20, true, "requests"),
                action(CLONE_REQUEST, "clone_actions", 10, "requests")));
    }

    public static ScreenDefinition ordersDefinition() {
        return definition(ORDERS, "orders", List.of(
                field(ORDER_SEARCH_TEXT, ScreenElementType.TEXT_INPUT, "search", 10, false, "orders"),
                field(ORDER_SEARCH_STATE, ScreenElementType.SELECT, "search", 20, false, "orders"),
                action(ORDER_SEARCH, "search_actions", 10, "orders"), content(ORDER_RESULTS, "results", 10, "orders"),
                action(SELECT_ORDER, "row_actions", 10, "orders"),
                field(ORDER_NUMBER, ScreenElementType.TEXT_INPUT, "create", 10, true, "orders"),
                field(ORDER_SUPPLIER, ScreenElementType.SELECT, "create", 20, true, "orders"),
                field(ORDER_CURRENCY, ScreenElementType.SELECT, "create", 30, true, "orders"),
                field(ORDER_JUSTIFICATION, ScreenElementType.TEXT_INPUT, "create", 40, false, "orders"),
                field(ORDER_KIND, ScreenElementType.SELECT, "create", 50, true, "orders"),
                field(ORDER_ITEM, ScreenElementType.SELECT, "create", 60, false, "orders"),
                field(ORDER_DESCRIPTION, ScreenElementType.TEXT_INPUT, "create", 70, true, "orders"),
                field(ORDER_UNIT, ScreenElementType.TEXT_INPUT, "create", 80, true, "orders"),
                field(ORDER_QUANTITY, ScreenElementType.TEXT_INPUT, "create", 90, true, "orders"),
                field(ORDER_PRICE, ScreenElementType.TEXT_INPUT, "create", 100, true, "orders"),
                field(ORDER_REQUEST, ScreenElementType.SELECT, "create", 110, false, "orders"),
                field(ORDER_REQUEST_LINE, ScreenElementType.SELECT, "create", 120, false, "orders"),
                field(ORDER_ALLOCATION_QUANTITY, ScreenElementType.TEXT_INPUT, "create", 130, false, "orders"),
                action(CREATE_ORDER, "create_actions", 10, "orders"),
                field(ORDER_ADD_KIND, ScreenElementType.SELECT, "lines", 10, true, "orders"),
                field(ORDER_ADD_ITEM, ScreenElementType.SELECT, "lines", 20, false, "orders"),
                field(ORDER_ADD_DESCRIPTION, ScreenElementType.TEXT_INPUT, "lines", 30, true, "orders"),
                field(ORDER_ADD_UNIT, ScreenElementType.TEXT_INPUT, "lines", 40, true, "orders"),
                field(ORDER_ADD_QUANTITY, ScreenElementType.TEXT_INPUT, "lines", 50, true, "orders"),
                field(ORDER_ADD_PRICE, ScreenElementType.TEXT_INPUT, "lines", 60, true, "orders"),
                action(ADD_ORDER_LINE, "lines_actions", 10, "orders"),
                field(ORDER_REASON, ScreenElementType.TEXT_INPUT, "lifecycle", 10, false, "orders"),
                action(ISSUE_ORDER, "lifecycle_actions", 10, "orders"),
                action(CANCEL_ORDER, "lifecycle_actions", 20, "orders"),
                action(CLOSE_ORDER_SHORT, "lifecycle_actions", 30, "orders")));
    }

    public static ScreenDefinition receiptsDefinition() {
        return definition(RECEIPTS, "receipts", List.of(
                field(RECEIPT_SEARCH_TEXT, ScreenElementType.TEXT_INPUT, "search", 10, false, "receipts"),
                field(RECEIPT_SEARCH_STATE, ScreenElementType.SELECT, "search", 20, false, "receipts"),
                action(RECEIPT_SEARCH, "search_actions", 10, "receipts"), content(RECEIPT_RESULTS, "results", 10, "receipts"),
                action(SELECT_RECEIPT, "row_actions", 10, "receipts"),
                field(RECEIPT_NUMBER, ScreenElementType.TEXT_INPUT, "create", 10, true, "receipts"),
                field(RECEIPT_ORDER, ScreenElementType.SELECT, "create", 20, true, "receipts"),
                field(RECEIPT_ORDER_LINE, ScreenElementType.SELECT, "create", 30, true, "receipts"),
                field(RECEIPT_QUANTITY, ScreenElementType.TEXT_INPUT, "create", 40, true, "receipts"),
                field(RECEIPT_WAREHOUSE, ScreenElementType.SELECT, "create", 50, false, "receipts"),
                field(RECEIPT_LOCATION, ScreenElementType.SELECT, "create", 60, false, "receipts"),
                field(RECEIPT_LOT, ScreenElementType.TEXT_INPUT, "create", 70, false, "receipts"),
                field(RECEIPT_SERIAL, ScreenElementType.TEXT_INPUT, "create", 80, false, "receipts"),
                field(RECEIPT_EXPIRY, ScreenElementType.TEXT_INPUT, "create", 90, false, "receipts"),
                field(RECEIPT_CONDITION, ScreenElementType.SELECT, "create", 100, false, "receipts"),
                action(CREATE_RECEIPT, "create_actions", 10, "receipts"),
                action(CONFIRM_RECEIPT, "lifecycle_actions", 10, "receipts")));
    }

    public static ScreenDefinition returnsDefinition() {
        return definition(RETURNS, "returns", List.of(
                field(RETURN_SEARCH_TEXT, ScreenElementType.TEXT_INPUT, "search", 10, false, "returns"),
                field(RETURN_SEARCH_STATE, ScreenElementType.SELECT, "search", 20, false, "returns"),
                action(RETURN_SEARCH, "search_actions", 10, "returns"), content(RETURN_RESULTS, "results", 10, "returns"),
                action(SELECT_RETURN, "row_actions", 10, "returns"),
                field(RETURN_NUMBER, ScreenElementType.TEXT_INPUT, "create", 10, true, "returns"),
                field(RETURN_ORDER, ScreenElementType.SELECT, "create", 20, true, "returns"),
                field(RETURN_RECEIPT, ScreenElementType.SELECT, "create", 30, true, "returns"),
                field(RETURN_RECEIPT_LINE, ScreenElementType.SELECT, "create", 40, true, "returns"),
                field(RETURN_QUANTITY, ScreenElementType.TEXT_INPUT, "create", 50, true, "returns"),
                field(RETURN_REASON, ScreenElementType.TEXT_INPUT, "create", 60, true, "returns"),
                action(CREATE_RETURN, "create_actions", 10, "returns"),
                action(CONFIRM_RETURN, "lifecycle_actions", 10, "returns")));
    }

    public static ScreenDefinition trackingDefinition() {
        return definition(TRACKING, "tracking", List.of(
                field(TRACKING_SEARCH_TEXT, ScreenElementType.TEXT_INPUT, "search", 10, false, "tracking"),
                field(TRACKING_SEARCH_STATE, ScreenElementType.SELECT, "search", 20, false, "tracking"),
                action(TRACKING_SEARCH, "search_actions", 10, "tracking"), content(TRACKING_RESULTS, "results", 10, "tracking"),
                action(SELECT_TRACKING_ORDER, "row_actions", 10, "tracking")));
    }

    private static ScreenDefinition definition(
            ScreenId id, String screen, List<ScreenElementDefinition> elements) {
        return new ScreenDefinition(id, SemanticVersion.parse("1.0.0"), elements, List.of(
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
        return element(id, ScreenElementType.ACTION, region, order, false,
                ACTION_CHANGES, screen);
    }

    private static ScreenElementDefinition content(
            ScreenElementId id, String region, int order, String screen) {
        return element(id, ScreenElementType.DATA_TABLE, region, order, false,
                CONTENT_CHANGES, screen);
    }

    private static ScreenElementDefinition element(
            ScreenElementId id, ScreenElementType type, String region, int order,
            boolean required, Set<ScreenCustomizationOperation> changes, String screen) {
        String key = "purchasing." + screen + "." + id.value();
        return new ScreenElementDefinition(
                id, type, new ScreenRegionId(region), order,
                new ScreenTextKey(key + ".label"),
                Optional.of(new ScreenTextKey(key + ".help")),
                true, true, required, changes);
    }

    private static ScreenId screen(String value) {
        return new ScreenId(PurchasingIdentity.PLUGIN_ID, value);
    }

    private static ScreenElementId id(String value) {
        return new ScreenElementId(value);
    }
}
