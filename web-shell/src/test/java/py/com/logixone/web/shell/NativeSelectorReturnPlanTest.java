package py.com.logixone.web.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import py.com.logixone.web.selector.NativeSelectorSourceCatalog;

class NativeSelectorReturnPlanTest {

    @Test
    void coversExactlyEveryManageableNativeUsageWithAClosedDraftWhitelist() {
        var plans = NativeSelectorReturnPlan.all();

        assertEquals(11, plans.size());
        assertEquals(
                NativeSelectorSourceCatalog.all().entrySet().stream()
                        .filter(entry -> entry.getValue().manageable())
                        .map(java.util.Map.Entry::getKey)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()),
                plans.keySet());
        assertTrue(plans.values().stream().allMatch(plan -> !plan.draftInputIds().isEmpty()));
        assertTrue(plans.values().stream().flatMap(plan -> plan.draftInputIds().stream())
                .allMatch(input -> input.matches("[a-z][a-z0-9_]{0,63}")));
        assertFalse(NativeSelectorReturnPlan.find(
                NativeSelectorSourceCatalog.SECURITY_GRANT_PERMISSION).isPresent());
    }

    @Test
    void samePageManagementStillKeepsAnExplicitOriginAndTarget() {
        var plan = NativeSelectorReturnPlan.find(
                NativeSelectorSourceCatalog.SECURITY_ASSIGNMENT_ROLE).orElseThrow();

        assertEquals("/admin/security.xhtml", plan.originRoute());
        assertEquals("/admin/security.xhtml", plan.targetRoute());
        assertEquals(
                java.util.Set.of("company_id", "assignment_user_id", "assignment_role_id"),
                plan.draftInputIds());
    }
}
