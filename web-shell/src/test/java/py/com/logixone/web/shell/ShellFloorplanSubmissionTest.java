package py.com.logixone.web.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ShellFloorplanSubmissionTest {

    @Test
    void overlaysSemanticFloorplanInputsWithoutTrustingGeneratedComponentNames() {
        Map<String, String> merged = ShellViewBean.mergeFloorplanSubmittedInputs(
                Map.of("stock_new_catalog_item", "default-item", "stock_task", "MOVEMENT"),
                Map.of(
                        "j_idt82:j_idt84:floorplan-select", new String[] {"ignored"},
                        "floorplanInput.stock_task", new String[] {"ITEM_ADMIN"},
                        "floorplanInput.stock_new_catalog_item", new String[] {"selected-item"}));

        assertEquals("ITEM_ADMIN", merged.get("stock_task"));
        assertEquals("selected-item", merged.get("stock_new_catalog_item"));
    }

    @Test
    void rejectsInvalidOrAmbiguousSemanticInputTransport() {
        assertThrows(IllegalArgumentException.class,
                () -> ShellViewBean.mergeFloorplanSubmittedInputs(
                        Map.of(), Map.of("floorplanInput.Invalid", new String[] {"value"})));
        assertThrows(IllegalArgumentException.class,
                () -> ShellViewBean.mergeFloorplanSubmittedInputs(
                        Map.of(), Map.of("floorplanInput.stock_task", new String[] {"one", "two"})));
    }

    @Test
    void submittedValuesOverrideRefreshedDefaultsWhileKeepingTechnicalState() {
        Map<String, String> actionInputs = ShellViewBean.mergeFloorplanActionInputs(
                Map.of(
                        "stock_new_catalog_item", "default-item",
                        "movement_idempotency", "refreshed-token"),
                Map.of(
                        "stock_new_catalog_item", "selected-item",
                        "stock_task", "ITEM_ADMIN"));

        assertEquals("selected-item", actionInputs.get("stock_new_catalog_item"));
        assertEquals("ITEM_ADMIN", actionInputs.get("stock_task"));
        assertEquals("refreshed-token", actionInputs.get("movement_idempotency"));
    }
}
