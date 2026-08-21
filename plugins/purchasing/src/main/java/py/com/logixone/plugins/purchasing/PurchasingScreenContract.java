package py.com.logixone.plugins.purchasing;

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
    public static final ScreenElementId REQUEST_LINES = id("request_lines");
    public static final ScreenElementId REQUEST_SUMMARY = id("request_summary");
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
    public static final ScreenElementId ORDER_LINES = id("order_lines");
    public static final ScreenElementId ORDER_SUMMARY = id("order_summary");
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
    public static final ScreenElementId RECEIPT_GUIDANCE = id("receipt_guidance");
    public static final ScreenElementId RECEIPT_SUMMARY = id("receipt_summary");
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
    public static final ScreenElementId RETURN_GUIDANCE = id("return_guidance");
    public static final ScreenElementId RETURN_SUMMARY = id("return_summary");
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
    public static final ScreenElementId TRACKING_SUMMARY = id("tracking_summary");
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
        List<ScreenElementDefinition> elements = List.of(
                field(REQUEST_SEARCH_TEXT, ScreenElementType.TEXT_INPUT, "filters", 10, false, "requests"),
                field(REQUEST_SEARCH_STATE, ScreenElementType.SELECT, "filters", 20, false, "requests"),
                action(REQUEST_SEARCH, "filters", 30, "requests"),
                content(REQUEST_RESULTS, ScreenElementType.DATA_TABLE, "work_items", 10, "requests"),
                action(SELECT_REQUEST, "work_items", 20, "requests"),
                field(REQUEST_NUMBER, ScreenElementType.TEXT_INPUT, "header", 10, true, "requests"),
                field(REQUEST_DATE, ScreenElementType.TEXT_INPUT, "header", 20, true, "requests"),
                field(REQUEST_KIND, ScreenElementType.SELECT, "lines", 10, true, "requests"),
                field(REQUEST_ITEM, ScreenElementType.SELECT, "lines", 20, false, "requests"),
                field(REQUEST_DESCRIPTION, ScreenElementType.TEXT_INPUT, "lines", 30, true, "requests"),
                field(REQUEST_UNIT, ScreenElementType.TEXT_INPUT, "lines", 40, true, "requests"),
                field(REQUEST_QUANTITY, ScreenElementType.TEXT_INPUT, "lines", 50, true, "requests"),
                field(REQUEST_EXPECTED_PRICE, ScreenElementType.TEXT_INPUT, "lines", 60, false, "requests"),
                field(REQUEST_CURRENCY, ScreenElementType.SELECT, "lines", 70, false, "requests"),
                content(REQUEST_LINES, ScreenElementType.DATA_TABLE, "lines", 80, "requests"),
                field(REQUEST_ADD_KIND, ScreenElementType.SELECT, "lines", 90, true, "requests"),
                field(REQUEST_ADD_ITEM, ScreenElementType.SELECT, "lines", 100, false, "requests"),
                field(REQUEST_ADD_DESCRIPTION, ScreenElementType.TEXT_INPUT, "lines", 110, true, "requests"),
                field(REQUEST_ADD_UNIT, ScreenElementType.TEXT_INPUT, "lines", 120, true, "requests"),
                field(REQUEST_ADD_QUANTITY, ScreenElementType.TEXT_INPUT, "lines", 130, true, "requests"),
                field(REQUEST_ADD_EXPECTED_PRICE, ScreenElementType.TEXT_INPUT, "lines", 140, false, "requests"),
                field(REQUEST_ADD_CURRENCY, ScreenElementType.SELECT, "lines", 150, false, "requests"),
                content(REQUEST_SUMMARY, ScreenElementType.DISPLAY_TEXT, "summary", 10, "requests"),
                field(REQUEST_REASON, ScreenElementType.TEXT_INPUT, "summary", 20, false, "requests"),
                field(REQUEST_CLONE_NUMBER, ScreenElementType.TEXT_INPUT, "summary", 30, true, "requests"),
                field(REQUEST_CLONE_DATE, ScreenElementType.TEXT_INPUT, "summary", 40, true, "requests"),
                action(CREATE_REQUEST, "actions", 10, "requests"),
                action(ADD_REQUEST_LINE, "actions", 20, "requests"),
                action(SUBMIT_REQUEST, "actions", 30, "requests"),
                action(APPROVE_REQUEST, "actions", 40, "requests"),
                action(REJECT_REQUEST, "actions", 50, "requests"),
                action(CANCEL_REQUEST, "actions", 60, "requests"),
                action(CLONE_REQUEST, "actions", 70, "requests"));
        return v2(REQUESTS, ScreenPurpose.WORKLIST, elements,
                List.of(
                        region("filters", ScreenRegionRole.FILTERS, 10),
                        region("work_items", ScreenRegionRole.WORK_ITEMS, 20),
                        region("header", ScreenRegionRole.HEADER, 30),
                        region("lines", ScreenRegionRole.LINES, 40),
                        region("summary", ScreenRegionRole.SUMMARY, 50),
                        region("actions", ScreenRegionRole.ACTIONS, 60)),
                Map.ofEntries(
                        semantic(REQUEST_SEARCH_TEXT, ScreenSemanticType.TEXT),
                        semantic(REQUEST_SEARCH_STATE, ScreenSemanticType.STATUS),
                        semantic(REQUEST_RESULTS, ScreenSemanticType.SUMMARY),
                        semantic(REQUEST_NUMBER, ScreenSemanticType.TEXT),
                        semantic(REQUEST_DATE, ScreenSemanticType.DATE),
                        semantic(REQUEST_KIND, ScreenSemanticType.STATUS),
                        semantic(REQUEST_ITEM, ScreenSemanticType.SEARCHABLE_REFERENCE),
                        semantic(REQUEST_DESCRIPTION, ScreenSemanticType.TEXT),
                        semantic(REQUEST_UNIT, ScreenSemanticType.TEXT),
                        semantic(REQUEST_QUANTITY, ScreenSemanticType.QUANTITY),
                        semantic(REQUEST_EXPECTED_PRICE, ScreenSemanticType.MONEY),
                        semantic(REQUEST_CURRENCY, ScreenSemanticType.SEARCHABLE_REFERENCE),
                        semantic(REQUEST_LINES, ScreenSemanticType.EDITABLE_LINES),
                        semantic(REQUEST_ADD_KIND, ScreenSemanticType.STATUS),
                        semantic(REQUEST_ADD_ITEM, ScreenSemanticType.SEARCHABLE_REFERENCE),
                        semantic(REQUEST_ADD_DESCRIPTION, ScreenSemanticType.TEXT),
                        semantic(REQUEST_ADD_UNIT, ScreenSemanticType.TEXT),
                        semantic(REQUEST_ADD_QUANTITY, ScreenSemanticType.QUANTITY),
                        semantic(REQUEST_ADD_EXPECTED_PRICE, ScreenSemanticType.MONEY),
                        semantic(REQUEST_ADD_CURRENCY, ScreenSemanticType.SEARCHABLE_REFERENCE),
                        semantic(REQUEST_SUMMARY, ScreenSemanticType.SUMMARY),
                        semantic(REQUEST_REASON, ScreenSemanticType.TEXT),
                        semantic(REQUEST_CLONE_NUMBER, ScreenSemanticType.TEXT),
                        semantic(REQUEST_CLONE_DATE, ScreenSemanticType.DATE)),
                List.of(
                        actionDefinition(REQUEST_SEARCH, ScreenActionIntent.SEARCH,
                                ScreenActionEmphasis.SECONDARY, ScreenConfirmationMode.NONE),
                        actionDefinition(SELECT_REQUEST, ScreenActionIntent.NAVIGATE,
                                ScreenActionEmphasis.SECONDARY, ScreenConfirmationMode.NONE),
                        actionDefinition(CREATE_REQUEST, ScreenActionIntent.CREATE,
                                ScreenActionEmphasis.PRIMARY, ScreenConfirmationMode.ACKNOWLEDGEMENT),
                        actionDefinition(ADD_REQUEST_LINE, ScreenActionIntent.ADD_LINE,
                                ScreenActionEmphasis.SECONDARY, ScreenConfirmationMode.ACKNOWLEDGEMENT),
                        actionDefinition(SUBMIT_REQUEST, ScreenActionIntent.SUBMIT,
                                ScreenActionEmphasis.PRIMARY, ScreenConfirmationMode.ACKNOWLEDGEMENT),
                        actionDefinition(APPROVE_REQUEST, ScreenActionIntent.APPROVE,
                                ScreenActionEmphasis.PRIMARY, ScreenConfirmationMode.ACKNOWLEDGEMENT),
                        actionDefinition(REJECT_REQUEST, ScreenActionIntent.REJECT,
                                ScreenActionEmphasis.DESTRUCTIVE, ScreenConfirmationMode.REASON_REQUIRED),
                        actionDefinition(CANCEL_REQUEST, ScreenActionIntent.CANCEL,
                                ScreenActionEmphasis.DESTRUCTIVE, ScreenConfirmationMode.REASON_REQUIRED),
                        actionDefinition(CLONE_REQUEST, ScreenActionIntent.CREATE,
                                ScreenActionEmphasis.SECONDARY, ScreenConfirmationMode.ACKNOWLEDGEMENT)));
    }

    public static ScreenDefinition ordersDefinition() {
        List<ScreenElementDefinition> elements = List.of(
                field(ORDER_SEARCH_TEXT, ScreenElementType.TEXT_INPUT, "filters", 10, false, "orders"),
                field(ORDER_SEARCH_STATE, ScreenElementType.SELECT, "filters", 20, false, "orders"),
                action(ORDER_SEARCH, "filters", 30, "orders"),
                content(ORDER_RESULTS, ScreenElementType.DATA_TABLE, "work_items", 10, "orders"),
                action(SELECT_ORDER, "work_items", 20, "orders"),
                field(ORDER_NUMBER, ScreenElementType.TEXT_INPUT, "header", 10, true, "orders"),
                field(ORDER_SUPPLIER, ScreenElementType.SELECT, "header", 20, true, "orders"),
                field(ORDER_CURRENCY, ScreenElementType.SELECT, "header", 30, true, "orders"),
                field(ORDER_JUSTIFICATION, ScreenElementType.TEXT_INPUT, "header", 40, false, "orders"),
                field(ORDER_KIND, ScreenElementType.SELECT, "lines", 10, true, "orders"),
                field(ORDER_ITEM, ScreenElementType.SELECT, "lines", 20, false, "orders"),
                field(ORDER_DESCRIPTION, ScreenElementType.TEXT_INPUT, "lines", 30, true, "orders"),
                field(ORDER_UNIT, ScreenElementType.TEXT_INPUT, "lines", 40, true, "orders"),
                field(ORDER_QUANTITY, ScreenElementType.TEXT_INPUT, "lines", 50, true, "orders"),
                field(ORDER_PRICE, ScreenElementType.TEXT_INPUT, "lines", 60, true, "orders"),
                field(ORDER_REQUEST, ScreenElementType.SELECT, "lines", 70, false, "orders"),
                field(ORDER_REQUEST_LINE, ScreenElementType.SELECT, "lines", 80, false, "orders"),
                field(ORDER_ALLOCATION_QUANTITY, ScreenElementType.TEXT_INPUT, "lines", 90, false, "orders"),
                content(ORDER_LINES, ScreenElementType.DATA_TABLE, "lines", 100, "orders"),
                field(ORDER_ADD_KIND, ScreenElementType.SELECT, "lines", 110, true, "orders"),
                field(ORDER_ADD_ITEM, ScreenElementType.SELECT, "lines", 120, false, "orders"),
                field(ORDER_ADD_DESCRIPTION, ScreenElementType.TEXT_INPUT, "lines", 130, true, "orders"),
                field(ORDER_ADD_UNIT, ScreenElementType.TEXT_INPUT, "lines", 140, true, "orders"),
                field(ORDER_ADD_QUANTITY, ScreenElementType.TEXT_INPUT, "lines", 150, true, "orders"),
                field(ORDER_ADD_PRICE, ScreenElementType.TEXT_INPUT, "lines", 160, true, "orders"),
                content(ORDER_SUMMARY, ScreenElementType.DISPLAY_TEXT, "summary", 10, "orders"),
                field(ORDER_REASON, ScreenElementType.TEXT_INPUT, "summary", 20, false, "orders"),
                action(CREATE_ORDER, "actions", 10, "orders"),
                action(ADD_ORDER_LINE, "actions", 20, "orders"),
                action(ISSUE_ORDER, "actions", 30, "orders"),
                action(CANCEL_ORDER, "actions", 40, "orders"),
                action(CLOSE_ORDER_SHORT, "actions", 50, "orders"));
        return v2(ORDERS, ScreenPurpose.TRANSACTION_EDITOR, elements,
                List.of(
                        region("filters", ScreenRegionRole.FILTERS, 10),
                        region("work_items", ScreenRegionRole.WORK_ITEMS, 20),
                        region("header", ScreenRegionRole.HEADER, 30),
                        region("lines", ScreenRegionRole.LINES, 40),
                        region("summary", ScreenRegionRole.SUMMARY, 50),
                        region("actions", ScreenRegionRole.ACTIONS, 60)),
                Map.ofEntries(
                        semantic(ORDER_SEARCH_TEXT, ScreenSemanticType.TEXT),
                        semantic(ORDER_SEARCH_STATE, ScreenSemanticType.STATUS),
                        semantic(ORDER_RESULTS, ScreenSemanticType.SUMMARY),
                        semantic(ORDER_NUMBER, ScreenSemanticType.TEXT),
                        semantic(ORDER_SUPPLIER, ScreenSemanticType.SEARCHABLE_REFERENCE),
                        semantic(ORDER_CURRENCY, ScreenSemanticType.SEARCHABLE_REFERENCE),
                        semantic(ORDER_JUSTIFICATION, ScreenSemanticType.TEXT),
                        semantic(ORDER_KIND, ScreenSemanticType.STATUS),
                        semantic(ORDER_ITEM, ScreenSemanticType.SEARCHABLE_REFERENCE),
                        semantic(ORDER_DESCRIPTION, ScreenSemanticType.TEXT),
                        semantic(ORDER_UNIT, ScreenSemanticType.TEXT),
                        semantic(ORDER_QUANTITY, ScreenSemanticType.QUANTITY),
                        semantic(ORDER_PRICE, ScreenSemanticType.MONEY),
                        semantic(ORDER_REQUEST, ScreenSemanticType.SEARCHABLE_REFERENCE),
                        semantic(ORDER_REQUEST_LINE, ScreenSemanticType.SEARCHABLE_REFERENCE),
                        semantic(ORDER_ALLOCATION_QUANTITY, ScreenSemanticType.QUANTITY),
                        semantic(ORDER_LINES, ScreenSemanticType.EDITABLE_LINES),
                        semantic(ORDER_ADD_KIND, ScreenSemanticType.STATUS),
                        semantic(ORDER_ADD_ITEM, ScreenSemanticType.SEARCHABLE_REFERENCE),
                        semantic(ORDER_ADD_DESCRIPTION, ScreenSemanticType.TEXT),
                        semantic(ORDER_ADD_UNIT, ScreenSemanticType.TEXT),
                        semantic(ORDER_ADD_QUANTITY, ScreenSemanticType.QUANTITY),
                        semantic(ORDER_ADD_PRICE, ScreenSemanticType.MONEY),
                        semantic(ORDER_SUMMARY, ScreenSemanticType.SUMMARY),
                        semantic(ORDER_REASON, ScreenSemanticType.TEXT)),
                List.of(
                        actionDefinition(ORDER_SEARCH, ScreenActionIntent.SEARCH,
                                ScreenActionEmphasis.SECONDARY, ScreenConfirmationMode.NONE),
                        actionDefinition(SELECT_ORDER, ScreenActionIntent.NAVIGATE,
                                ScreenActionEmphasis.SECONDARY, ScreenConfirmationMode.NONE),
                        actionDefinition(CREATE_ORDER, ScreenActionIntent.CREATE,
                                ScreenActionEmphasis.PRIMARY, ScreenConfirmationMode.ACKNOWLEDGEMENT),
                        actionDefinition(ADD_ORDER_LINE, ScreenActionIntent.ADD_LINE,
                                ScreenActionEmphasis.SECONDARY, ScreenConfirmationMode.ACKNOWLEDGEMENT),
                        actionDefinition(ISSUE_ORDER, ScreenActionIntent.SUBMIT,
                                ScreenActionEmphasis.PRIMARY, ScreenConfirmationMode.ACKNOWLEDGEMENT),
                        actionDefinition(CANCEL_ORDER, ScreenActionIntent.CANCEL,
                                ScreenActionEmphasis.DESTRUCTIVE, ScreenConfirmationMode.REASON_REQUIRED),
                        actionDefinition(CLOSE_ORDER_SHORT, ScreenActionIntent.CLOSE,
                                ScreenActionEmphasis.DESTRUCTIVE, ScreenConfirmationMode.REASON_REQUIRED)));
    }

    public static ScreenDefinition receiptsDefinition() {
        List<ScreenElementDefinition> elements = List.of(
                field(RECEIPT_SEARCH_TEXT, ScreenElementType.TEXT_INPUT, "filters", 10, false, "receipts"),
                field(RECEIPT_SEARCH_STATE, ScreenElementType.SELECT, "filters", 20, false, "receipts"),
                action(RECEIPT_SEARCH, "filters", 30, "receipts"),
                content(RECEIPT_RESULTS, ScreenElementType.DATA_TABLE, "context", 10, "receipts"),
                action(SELECT_RECEIPT, "context", 20, "receipts"),
                field(RECEIPT_NUMBER, ScreenElementType.TEXT_INPUT, "context", 30, true, "receipts"),
                field(RECEIPT_ORDER, ScreenElementType.SELECT, "context", 40, true, "receipts"),
                field(RECEIPT_ORDER_LINE, ScreenElementType.SELECT, "context", 50, true, "receipts"),
                field(RECEIPT_QUANTITY, ScreenElementType.TEXT_INPUT, "content", 10, true, "receipts"),
                field(RECEIPT_WAREHOUSE, ScreenElementType.SELECT, "content", 20, false, "receipts"),
                field(RECEIPT_LOCATION, ScreenElementType.SELECT, "content", 30, false, "receipts"),
                field(RECEIPT_LOT, ScreenElementType.TEXT_INPUT, "content", 40, false, "receipts"),
                field(RECEIPT_SERIAL, ScreenElementType.TEXT_INPUT, "content", 50, false, "receipts"),
                field(RECEIPT_EXPIRY, ScreenElementType.TEXT_INPUT, "content", 60, false, "receipts"),
                field(RECEIPT_CONDITION, ScreenElementType.SELECT, "content", 70, false, "receipts"),
                content(RECEIPT_GUIDANCE, ScreenElementType.DISPLAY_TEXT, "guidance", 10, "receipts"),
                content(RECEIPT_SUMMARY, ScreenElementType.DISPLAY_TEXT, "summary", 10, "receipts"),
                action(CREATE_RECEIPT, "actions", 10, "receipts"),
                action(CONFIRM_RECEIPT, "actions", 20, "receipts"));
        return v2(RECEIPTS, ScreenPurpose.GUIDED_OPERATION, elements,
                List.of(
                        region("filters", ScreenRegionRole.FILTERS, 10),
                        region("context", ScreenRegionRole.CONTEXT, 20),
                        region("content", ScreenRegionRole.CONTENT, 30),
                        region("guidance", ScreenRegionRole.GUIDANCE, 40),
                        region("summary", ScreenRegionRole.SUMMARY, 50),
                        region("actions", ScreenRegionRole.ACTIONS, 60)),
                Map.ofEntries(
                        semantic(RECEIPT_SEARCH_TEXT, ScreenSemanticType.TEXT),
                        semantic(RECEIPT_SEARCH_STATE, ScreenSemanticType.STATUS),
                        semantic(RECEIPT_RESULTS, ScreenSemanticType.SUMMARY),
                        semantic(RECEIPT_NUMBER, ScreenSemanticType.TEXT),
                        semantic(RECEIPT_ORDER, ScreenSemanticType.SEARCHABLE_REFERENCE),
                        semantic(RECEIPT_ORDER_LINE, ScreenSemanticType.SEARCHABLE_REFERENCE),
                        semantic(RECEIPT_QUANTITY, ScreenSemanticType.QUANTITY),
                        semantic(RECEIPT_WAREHOUSE, ScreenSemanticType.SEARCHABLE_REFERENCE),
                        semantic(RECEIPT_LOCATION, ScreenSemanticType.SEARCHABLE_REFERENCE),
                        semantic(RECEIPT_LOT, ScreenSemanticType.TEXT),
                        semantic(RECEIPT_SERIAL, ScreenSemanticType.TEXT),
                        semantic(RECEIPT_EXPIRY, ScreenSemanticType.DATE),
                        semantic(RECEIPT_CONDITION, ScreenSemanticType.STATUS),
                        semantic(RECEIPT_GUIDANCE, ScreenSemanticType.SUMMARY),
                        semantic(RECEIPT_SUMMARY, ScreenSemanticType.SUMMARY)),
                List.of(
                        actionDefinition(RECEIPT_SEARCH, ScreenActionIntent.SEARCH,
                                ScreenActionEmphasis.SECONDARY, ScreenConfirmationMode.NONE),
                        actionDefinition(SELECT_RECEIPT, ScreenActionIntent.NAVIGATE,
                                ScreenActionEmphasis.SECONDARY, ScreenConfirmationMode.NONE),
                        actionDefinition(CREATE_RECEIPT, ScreenActionIntent.CREATE,
                                ScreenActionEmphasis.PRIMARY, ScreenConfirmationMode.ACKNOWLEDGEMENT),
                        actionDefinition(CONFIRM_RECEIPT, ScreenActionIntent.CONFIRM,
                                ScreenActionEmphasis.PRIMARY, ScreenConfirmationMode.ACKNOWLEDGEMENT)));
    }

    public static ScreenDefinition returnsDefinition() {
        List<ScreenElementDefinition> elements = List.of(
                field(RETURN_SEARCH_TEXT, ScreenElementType.TEXT_INPUT, "filters", 10, false, "returns"),
                field(RETURN_SEARCH_STATE, ScreenElementType.SELECT, "filters", 20, false, "returns"),
                action(RETURN_SEARCH, "filters", 30, "returns"),
                content(RETURN_RESULTS, ScreenElementType.DATA_TABLE, "context", 10, "returns"),
                action(SELECT_RETURN, "context", 20, "returns"),
                field(RETURN_NUMBER, ScreenElementType.TEXT_INPUT, "context", 30, true, "returns"),
                field(RETURN_ORDER, ScreenElementType.SELECT, "context", 40, true, "returns"),
                field(RETURN_RECEIPT, ScreenElementType.SELECT, "context", 50, true, "returns"),
                field(RETURN_RECEIPT_LINE, ScreenElementType.SELECT, "context", 60, true, "returns"),
                field(RETURN_QUANTITY, ScreenElementType.TEXT_INPUT, "content", 10, true, "returns"),
                field(RETURN_REASON, ScreenElementType.TEXT_INPUT, "content", 20, true, "returns"),
                content(RETURN_GUIDANCE, ScreenElementType.DISPLAY_TEXT, "guidance", 10, "returns"),
                content(RETURN_SUMMARY, ScreenElementType.DISPLAY_TEXT, "summary", 10, "returns"),
                action(CREATE_RETURN, "actions", 10, "returns"),
                action(CONFIRM_RETURN, "actions", 20, "returns"));
        return v2(RETURNS, ScreenPurpose.GUIDED_OPERATION, elements,
                List.of(
                        region("filters", ScreenRegionRole.FILTERS, 10),
                        region("context", ScreenRegionRole.CONTEXT, 20),
                        region("content", ScreenRegionRole.CONTENT, 30),
                        region("guidance", ScreenRegionRole.GUIDANCE, 40),
                        region("summary", ScreenRegionRole.SUMMARY, 50),
                        region("actions", ScreenRegionRole.ACTIONS, 60)),
                Map.ofEntries(
                        semantic(RETURN_SEARCH_TEXT, ScreenSemanticType.TEXT),
                        semantic(RETURN_SEARCH_STATE, ScreenSemanticType.STATUS),
                        semantic(RETURN_RESULTS, ScreenSemanticType.SUMMARY),
                        semantic(RETURN_NUMBER, ScreenSemanticType.TEXT),
                        semantic(RETURN_ORDER, ScreenSemanticType.SEARCHABLE_REFERENCE),
                        semantic(RETURN_RECEIPT, ScreenSemanticType.SEARCHABLE_REFERENCE),
                        semantic(RETURN_RECEIPT_LINE, ScreenSemanticType.SEARCHABLE_REFERENCE),
                        semantic(RETURN_QUANTITY, ScreenSemanticType.QUANTITY),
                        semantic(RETURN_REASON, ScreenSemanticType.TEXT),
                        semantic(RETURN_GUIDANCE, ScreenSemanticType.SUMMARY),
                        semantic(RETURN_SUMMARY, ScreenSemanticType.SUMMARY)),
                List.of(
                        actionDefinition(RETURN_SEARCH, ScreenActionIntent.SEARCH,
                                ScreenActionEmphasis.SECONDARY, ScreenConfirmationMode.NONE),
                        actionDefinition(SELECT_RETURN, ScreenActionIntent.NAVIGATE,
                                ScreenActionEmphasis.SECONDARY, ScreenConfirmationMode.NONE),
                        actionDefinition(CREATE_RETURN, ScreenActionIntent.CREATE,
                                ScreenActionEmphasis.PRIMARY, ScreenConfirmationMode.ACKNOWLEDGEMENT),
                        actionDefinition(CONFIRM_RETURN, ScreenActionIntent.CONFIRM,
                                ScreenActionEmphasis.PRIMARY, ScreenConfirmationMode.ACKNOWLEDGEMENT)));
    }

    public static ScreenDefinition trackingDefinition() {
        List<ScreenElementDefinition> elements = List.of(
                field(TRACKING_SEARCH_TEXT, ScreenElementType.TEXT_INPUT, "filters", 10, false, "tracking"),
                field(TRACKING_SEARCH_STATE, ScreenElementType.SELECT, "filters", 20, false, "tracking"),
                action(TRACKING_SEARCH, "filters", 30, "tracking"),
                content(TRACKING_RESULTS, ScreenElementType.DATA_TABLE, "content", 10, "tracking"),
                content(TRACKING_SUMMARY, ScreenElementType.DISPLAY_TEXT, "content", 20, "tracking"),
                action(SELECT_TRACKING_ORDER, "actions", 20, "tracking"));
        return v2(TRACKING, ScreenPurpose.INQUIRY, elements,
                List.of(
                        region("filters", ScreenRegionRole.FILTERS, 10),
                        region("content", ScreenRegionRole.CONTENT, 20),
                        region("actions", ScreenRegionRole.ACTIONS, 30)),
                Map.ofEntries(
                        semantic(TRACKING_SEARCH_TEXT, ScreenSemanticType.TEXT),
                        semantic(TRACKING_SEARCH_STATE, ScreenSemanticType.STATUS),
                        semantic(TRACKING_RESULTS, ScreenSemanticType.SUMMARY),
                        semantic(TRACKING_SUMMARY, ScreenSemanticType.SUMMARY)),
                List.of(
                        actionDefinition(TRACKING_SEARCH, ScreenActionIntent.SEARCH,
                                ScreenActionEmphasis.SECONDARY, ScreenConfirmationMode.NONE),
                        actionDefinition(SELECT_TRACKING_ORDER, ScreenActionIntent.NAVIGATE,
                                ScreenActionEmphasis.SECONDARY, ScreenConfirmationMode.NONE)));
    }

    private static ScreenDefinition v2(
            ScreenId id,
            ScreenPurpose purpose,
            List<ScreenElementDefinition> elements,
            List<ScreenRegionDefinition> regions,
            Map<ScreenElementId, ScreenSemanticType> semantics,
            List<ScreenActionDefinition> actions) {
        return new ScreenDefinition(
                id,
                SemanticVersion.parse("2.0.0"),
                elements,
                List.of(),
                Optional.of(new ScreenExperienceDefinition(
                        purpose, regions, semantics, actions)));
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
            ScreenElementId id, ScreenElementType type, String region, int order, String screen) {
        return element(id, type, region, order, false, CONTENT_CHANGES, screen);
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
