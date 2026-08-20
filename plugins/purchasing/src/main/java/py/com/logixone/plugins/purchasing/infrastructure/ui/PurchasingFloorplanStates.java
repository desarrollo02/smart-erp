package py.com.logixone.plugins.purchasing.infrastructure.ui;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenInteraction;
import py.com.logixone.plugins.purchasing.PurchasingScreenContract;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptState;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderState;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestState;
import py.com.logixone.plugins.purchasing.api.SupplierReturnState;

/** Pure runtime visibility policy for the closed purchasing floorplans. */
final class PurchasingFloorplanStates {

    private PurchasingFloorplanStates() {
    }

    static Map<ScreenElementId, ScreenInteraction.ElementState> requests(
            Optional<PurchaseRequestState> selectedState,
            boolean currentActorIsRequester) {
        Map<ScreenElementId, ScreenInteraction.ElementState> states = new LinkedHashMap<>();
        boolean selected = selectedState.isPresent();
        group(states, !selected,
                PurchasingScreenContract.REQUEST_RESULTS,
                PurchasingScreenContract.REQUEST_NUMBER,
                PurchasingScreenContract.REQUEST_DATE,
                PurchasingScreenContract.REQUEST_KIND,
                PurchasingScreenContract.REQUEST_ITEM,
                PurchasingScreenContract.REQUEST_DESCRIPTION,
                PurchasingScreenContract.REQUEST_UNIT,
                PurchasingScreenContract.REQUEST_QUANTITY,
                PurchasingScreenContract.REQUEST_EXPECTED_PRICE,
                PurchasingScreenContract.REQUEST_CURRENCY,
                PurchasingScreenContract.CREATE_REQUEST);
        group(states, selected,
                PurchasingScreenContract.REQUEST_LINES,
                PurchasingScreenContract.REQUEST_SUMMARY,
                PurchasingScreenContract.REQUEST_CLONE_NUMBER,
                PurchasingScreenContract.REQUEST_CLONE_DATE,
                PurchasingScreenContract.CLONE_REQUEST);

        boolean editable = selectedState.filter(state -> state == PurchaseRequestState.DRAFT)
                .isPresent() && currentActorIsRequester;
        group(states, editable,
                PurchasingScreenContract.REQUEST_ADD_KIND,
                PurchasingScreenContract.REQUEST_ADD_ITEM,
                PurchasingScreenContract.REQUEST_ADD_DESCRIPTION,
                PurchasingScreenContract.REQUEST_ADD_UNIT,
                PurchasingScreenContract.REQUEST_ADD_QUANTITY,
                PurchasingScreenContract.REQUEST_ADD_EXPECTED_PRICE,
                PurchasingScreenContract.REQUEST_ADD_CURRENCY,
                PurchasingScreenContract.ADD_REQUEST_LINE,
                PurchasingScreenContract.SUBMIT_REQUEST);

        boolean pending = selectedState.filter(state -> state == PurchaseRequestState.SUBMITTED)
                .isPresent();
        group(states, pending,
                PurchasingScreenContract.REQUEST_REASON,
                PurchasingScreenContract.APPROVE_REQUEST,
                PurchasingScreenContract.REJECT_REQUEST);
        if (pending && currentActorIsRequester) {
            String reason = "La solicitud debe ser decidida por otra persona.";
            states.put(PurchasingScreenContract.APPROVE_REQUEST,
                    ScreenInteraction.ElementState.blocked(reason));
            states.put(PurchasingScreenContract.REJECT_REQUEST,
                    ScreenInteraction.ElementState.blocked(reason));
        }

        boolean cancellable = selectedState
                .filter(state -> state == PurchaseRequestState.DRAFT
                        || state == PurchaseRequestState.SUBMITTED)
                .isPresent();
        states.put(PurchasingScreenContract.CANCEL_REQUEST,
                cancellable && currentActorIsRequester
                        ? ScreenInteraction.ElementState.shown()
                        : ScreenInteraction.ElementState.hidden());
        if (cancellable) {
            states.put(PurchasingScreenContract.REQUEST_REASON,
                    ScreenInteraction.ElementState.shown());
        }
        return Map.copyOf(states);
    }

    static Map<ScreenElementId, ScreenInteraction.ElementState> orders(
            Optional<PurchaseOrderState> selectedState,
            boolean hasConfirmedReceipts,
            boolean hasPendingQuantity) {
        Map<ScreenElementId, ScreenInteraction.ElementState> states = new LinkedHashMap<>();
        boolean selected = selectedState.isPresent();
        group(states, !selected,
                PurchasingScreenContract.ORDER_RESULTS,
                PurchasingScreenContract.ORDER_NUMBER,
                PurchasingScreenContract.ORDER_SUPPLIER,
                PurchasingScreenContract.ORDER_CURRENCY,
                PurchasingScreenContract.ORDER_JUSTIFICATION,
                PurchasingScreenContract.ORDER_KIND,
                PurchasingScreenContract.ORDER_ITEM,
                PurchasingScreenContract.ORDER_DESCRIPTION,
                PurchasingScreenContract.ORDER_UNIT,
                PurchasingScreenContract.ORDER_QUANTITY,
                PurchasingScreenContract.ORDER_PRICE,
                PurchasingScreenContract.ORDER_REQUEST,
                PurchasingScreenContract.ORDER_REQUEST_LINE,
                PurchasingScreenContract.ORDER_ALLOCATION_QUANTITY,
                PurchasingScreenContract.CREATE_ORDER);
        group(states, selected,
                PurchasingScreenContract.ORDER_LINES,
                PurchasingScreenContract.ORDER_SUMMARY);

        boolean draft = selectedState.filter(state -> state == PurchaseOrderState.DRAFT).isPresent();
        group(states, draft,
                PurchasingScreenContract.ORDER_ADD_KIND,
                PurchasingScreenContract.ORDER_ADD_ITEM,
                PurchasingScreenContract.ORDER_ADD_DESCRIPTION,
                PurchasingScreenContract.ORDER_ADD_UNIT,
                PurchasingScreenContract.ORDER_ADD_QUANTITY,
                PurchasingScreenContract.ORDER_ADD_PRICE,
                PurchasingScreenContract.ADD_ORDER_LINE,
                PurchasingScreenContract.ISSUE_ORDER);

        boolean issued = selectedState.filter(state -> state == PurchaseOrderState.ISSUED).isPresent();
        group(states, draft || issued,
                PurchasingScreenContract.ORDER_REASON,
                PurchasingScreenContract.CANCEL_ORDER);
        if (issued && hasConfirmedReceipts) {
            states.put(PurchasingScreenContract.CANCEL_ORDER,
                    ScreenInteraction.ElementState.blocked(
                            "Una orden con recepciones confirmadas no puede cancelarse."));
        }
        states.put(PurchasingScreenContract.CLOSE_ORDER_SHORT,
                issued && hasPendingQuantity
                        ? ScreenInteraction.ElementState.shown()
                        : ScreenInteraction.ElementState.hidden());
        return Map.copyOf(states);
    }

    static Map<ScreenElementId, ScreenInteraction.ElementState> receipts(
            Optional<GoodsReceiptState> selectedState,
            boolean stockLine,
            boolean creationReady) {
        Map<ScreenElementId, ScreenInteraction.ElementState> states = new LinkedHashMap<>();
        boolean selected = selectedState.isPresent();
        group(states, !selected,
                PurchasingScreenContract.RECEIPT_RESULTS,
                PurchasingScreenContract.RECEIPT_NUMBER,
                PurchasingScreenContract.RECEIPT_ORDER,
                PurchasingScreenContract.RECEIPT_ORDER_LINE,
                PurchasingScreenContract.RECEIPT_QUANTITY,
                PurchasingScreenContract.CREATE_RECEIPT);
        group(states, selected,
                PurchasingScreenContract.RECEIPT_SUMMARY);
        group(states, true, PurchasingScreenContract.RECEIPT_GUIDANCE);
        group(states, !selected && stockLine,
                PurchasingScreenContract.RECEIPT_WAREHOUSE,
                PurchasingScreenContract.RECEIPT_LOCATION,
                PurchasingScreenContract.RECEIPT_LOT,
                PurchasingScreenContract.RECEIPT_SERIAL,
                PurchasingScreenContract.RECEIPT_EXPIRY,
                PurchasingScreenContract.RECEIPT_CONDITION);
        if (!selected && stockLine) {
            states.put(PurchasingScreenContract.RECEIPT_WAREHOUSE,
                    ScreenInteraction.ElementState.requiredInput());
            states.put(PurchasingScreenContract.RECEIPT_LOCATION,
                    ScreenInteraction.ElementState.requiredInput());
            states.put(PurchasingScreenContract.RECEIPT_CONDITION,
                    ScreenInteraction.ElementState.requiredInput());
        }
        if (!selected && !creationReady) {
            states.put(PurchasingScreenContract.CREATE_RECEIPT,
                    ScreenInteraction.ElementState.blocked(
                            "Selecciona una orden emitida y una línea con cantidad pendiente."));
        }
        states.put(PurchasingScreenContract.CONFIRM_RECEIPT,
                selectedState.filter(state -> state == GoodsReceiptState.DRAFT).isPresent()
                        ? ScreenInteraction.ElementState.shown()
                        : ScreenInteraction.ElementState.hidden());
        return Map.copyOf(states);
    }

    static Map<ScreenElementId, ScreenInteraction.ElementState> returns(
            Optional<SupplierReturnState> selectedState,
            boolean creationReady) {
        Map<ScreenElementId, ScreenInteraction.ElementState> states = new LinkedHashMap<>();
        boolean selected = selectedState.isPresent();
        group(states, !selected,
                PurchasingScreenContract.RETURN_RESULTS,
                PurchasingScreenContract.RETURN_NUMBER,
                PurchasingScreenContract.RETURN_ORDER,
                PurchasingScreenContract.RETURN_RECEIPT,
                PurchasingScreenContract.RETURN_RECEIPT_LINE,
                PurchasingScreenContract.RETURN_QUANTITY,
                PurchasingScreenContract.RETURN_REASON,
                PurchasingScreenContract.CREATE_RETURN);
        group(states, selected, PurchasingScreenContract.RETURN_SUMMARY);
        group(states, true, PurchasingScreenContract.RETURN_GUIDANCE);
        if (!selected && !creationReady) {
            states.put(PurchasingScreenContract.CREATE_RETURN,
                    ScreenInteraction.ElementState.blocked(
                            "Selecciona una recepción confirmada y una línea retornable."));
        }
        states.put(PurchasingScreenContract.CONFIRM_RETURN,
                selectedState.filter(state -> state == SupplierReturnState.DRAFT).isPresent()
                        ? ScreenInteraction.ElementState.shown()
                        : ScreenInteraction.ElementState.hidden());
        return Map.copyOf(states);
    }

    static Map<ScreenElementId, ScreenInteraction.ElementState> tracking(boolean selected) {
        return Map.of(
                PurchasingScreenContract.TRACKING_RESULTS,
                selected ? ScreenInteraction.ElementState.hidden()
                        : ScreenInteraction.ElementState.shown(),
                PurchasingScreenContract.TRACKING_SUMMARY,
                selected ? ScreenInteraction.ElementState.shown()
                        : ScreenInteraction.ElementState.hidden());
    }

    private static void group(
            Map<ScreenElementId, ScreenInteraction.ElementState> states,
            boolean visible,
            ScreenElementId... elements) {
        ScreenInteraction.ElementState state = visible
                ? ScreenInteraction.ElementState.shown()
                : ScreenInteraction.ElementState.hidden();
        for (ScreenElementId element : elements) {
            states.put(element, state);
        }
    }
}
