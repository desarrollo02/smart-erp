package py.com.logixone.plugins.purchasing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import py.com.logixone.plugin.api.ScreenActionEmphasis;
import py.com.logixone.plugin.api.ScreenConfirmationMode;
import py.com.logixone.plugin.api.ScreenPurpose;
import py.com.logixone.plugin.api.ScreenRegionRole;
import py.com.logixone.plugin.api.ScreenSemanticType;

class PurchasingFloorplanContractTest {

    @Test
    void purchasingScreensMigrateInPlaceToClosedVersionTwoPurposes() {
        assertPurpose(PurchasingScreenContract.requestsDefinition(), ScreenPurpose.WORKLIST);
        assertPurpose(PurchasingScreenContract.ordersDefinition(), ScreenPurpose.TRANSACTION_EDITOR);
        assertPurpose(PurchasingScreenContract.receiptsDefinition(), ScreenPurpose.GUIDED_OPERATION);
        assertPurpose(PurchasingScreenContract.returnsDefinition(), ScreenPurpose.GUIDED_OPERATION);
        assertPurpose(PurchasingScreenContract.trackingDefinition(), ScreenPurpose.INQUIRY);
    }

    @Test
    void requestWorklistSeparatesWorkItemsFromLifecycleDecisions() {
        var experience = PurchasingScreenContract.requestsDefinition().experience().orElseThrow();

        assertEquals(List.of(
                        ScreenRegionRole.FILTERS,
                        ScreenRegionRole.WORK_ITEMS,
                        ScreenRegionRole.HEADER,
                        ScreenRegionRole.LINES,
                        ScreenRegionRole.SUMMARY,
                        ScreenRegionRole.ACTIONS),
                experience.regions().stream().map(region -> region.role()).toList());
        assertEquals(ScreenSemanticType.EDITABLE_LINES,
                experience.elementSemantics().get(PurchasingScreenContract.REQUEST_LINES));
        assertAction(experience, PurchasingScreenContract.APPROVE_REQUEST,
                ScreenActionEmphasis.PRIMARY, ScreenConfirmationMode.ACKNOWLEDGEMENT);
        assertAction(experience, PurchasingScreenContract.REJECT_REQUEST,
                ScreenActionEmphasis.DESTRUCTIVE, ScreenConfirmationMode.REASON_REQUIRED);
    }

    @Test
    void orderEditorPublishesHeaderLinesSummaryAndRiskProportionalActions() {
        var experience = PurchasingScreenContract.ordersDefinition().experience().orElseThrow();

        assertTrue(experience.regions().stream().anyMatch(
                region -> region.role() == ScreenRegionRole.HEADER));
        assertTrue(experience.regions().stream().anyMatch(
                region -> region.role() == ScreenRegionRole.LINES));
        assertTrue(experience.regions().stream().anyMatch(
                region -> region.role() == ScreenRegionRole.SUMMARY));
        assertEquals(ScreenSemanticType.EDITABLE_LINES,
                experience.elementSemantics().get(PurchasingScreenContract.ORDER_LINES));
        assertEquals(ScreenSemanticType.MONEY,
                experience.elementSemantics().get(PurchasingScreenContract.ORDER_PRICE));
        assertAction(experience, PurchasingScreenContract.ISSUE_ORDER,
                ScreenActionEmphasis.PRIMARY, ScreenConfirmationMode.ACKNOWLEDGEMENT);
        assertAction(experience, PurchasingScreenContract.CLOSE_ORDER_SHORT,
                ScreenActionEmphasis.DESTRUCTIVE, ScreenConfirmationMode.REASON_REQUIRED);
    }

    @Test
    void guidedReceiptAndReturnExposeReviewBeforeConfirmation() {
        var receipt = PurchasingScreenContract.receiptsDefinition().experience().orElseThrow();
        var supplierReturn = PurchasingScreenContract.returnsDefinition().experience().orElseThrow();

        assertEquals(ScreenSemanticType.SUMMARY,
                receipt.elementSemantics().get(PurchasingScreenContract.RECEIPT_SUMMARY));
        assertEquals(ScreenSemanticType.SUMMARY,
                supplierReturn.elementSemantics().get(PurchasingScreenContract.RETURN_SUMMARY));
        assertAction(receipt, PurchasingScreenContract.CONFIRM_RECEIPT,
                ScreenActionEmphasis.PRIMARY, ScreenConfirmationMode.ACKNOWLEDGEMENT);
        assertAction(supplierReturn, PurchasingScreenContract.CONFIRM_RETURN,
                ScreenActionEmphasis.PRIMARY, ScreenConfirmationMode.ACKNOWLEDGEMENT);
    }

    private static void assertPurpose(
            py.com.logixone.plugin.api.ScreenDefinition definition,
            ScreenPurpose purpose) {
        assertEquals("2.0.0", definition.contractVersion().toString());
        assertTrue(definition.slots().isEmpty());
        assertEquals(purpose, definition.experience().orElseThrow().purpose());
    }

    private static void assertAction(
            py.com.logixone.plugin.api.ScreenExperienceDefinition experience,
            py.com.logixone.plugin.api.ScreenElementId actionId,
            ScreenActionEmphasis emphasis,
            ScreenConfirmationMode confirmation) {
        var action = experience.actions().stream()
                .filter(candidate -> candidate.elementId().equals(actionId))
                .findFirst()
                .orElseThrow();
        assertEquals(emphasis, action.emphasis());
        assertEquals(confirmation, action.confirmationMode());
    }
}
