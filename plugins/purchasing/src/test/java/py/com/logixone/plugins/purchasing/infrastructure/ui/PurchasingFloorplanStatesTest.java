package py.com.logixone.plugins.purchasing.infrastructure.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugins.purchasing.PurchasingScreenContract;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptState;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderState;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestState;
import py.com.logixone.plugins.purchasing.api.SupplierReturnState;

class PurchasingFloorplanStatesTest {

    @Test
    void requestApprovalRequiresSubmittedWorkAndAnotherActor() {
        var ownSubmitted = PurchasingFloorplanStates.requests(
                Optional.of(PurchaseRequestState.SUBMITTED), true);
        var otherSubmitted = PurchasingFloorplanStates.requests(
                Optional.of(PurchaseRequestState.SUBMITTED), false);

        assertFalse(ownSubmitted.get(PurchasingScreenContract.APPROVE_REQUEST).enabled());
        assertTrue(ownSubmitted.get(PurchasingScreenContract.APPROVE_REQUEST)
                .unavailableReason().isPresent());
        assertTrue(otherSubmitted.get(PurchasingScreenContract.APPROVE_REQUEST).enabled());
        assertFalse(otherSubmitted.get(PurchasingScreenContract.ADD_REQUEST_LINE).visible());
    }

    @Test
    void orderActionsFollowLifecycleAndConfirmedReceipts() {
        var draft = PurchasingFloorplanStates.orders(
                Optional.of(PurchaseOrderState.DRAFT), false, true);
        var received = PurchasingFloorplanStates.orders(
                Optional.of(PurchaseOrderState.ISSUED), true, true);

        assertTrue(draft.get(PurchasingScreenContract.ADD_ORDER_LINE).visible());
        assertTrue(draft.get(PurchasingScreenContract.ISSUE_ORDER).enabled());
        assertFalse(draft.get(PurchasingScreenContract.CLOSE_ORDER_SHORT).visible());
        assertFalse(received.get(PurchasingScreenContract.CANCEL_ORDER).enabled());
        assertTrue(received.get(PurchasingScreenContract.CLOSE_ORDER_SHORT).visible());
    }

    @Test
    void receiptShowsInventoryDestinationOnlyForStockLines() {
        var stock = PurchasingFloorplanStates.receipts(Optional.empty(), true, true);
        var service = PurchasingFloorplanStates.receipts(Optional.empty(), false, true);
        var confirmed = PurchasingFloorplanStates.receipts(
                Optional.of(GoodsReceiptState.CONFIRMED), true, true);

        assertTrue(stock.get(PurchasingScreenContract.RECEIPT_WAREHOUSE).required());
        assertFalse(service.get(PurchasingScreenContract.RECEIPT_WAREHOUSE).visible());
        assertFalse(confirmed.get(PurchasingScreenContract.CONFIRM_RECEIPT).visible());
    }

    @Test
    void returnConfirmationIsAvailableOnlyForDraft() {
        var draft = PurchasingFloorplanStates.returns(
                Optional.of(SupplierReturnState.DRAFT), true);
        var confirmed = PurchasingFloorplanStates.returns(
                Optional.of(SupplierReturnState.CONFIRMED), true);

        assertTrue(draft.get(PurchasingScreenContract.CONFIRM_RETURN).enabled());
        assertFalse(confirmed.get(PurchasingScreenContract.CONFIRM_RETURN).visible());
    }
}
