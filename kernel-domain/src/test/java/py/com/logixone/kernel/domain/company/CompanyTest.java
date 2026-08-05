package py.com.logixone.kernel.domain.company;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugin.api.PluginId;

class CompanyTest {

    private static final CompanyId COMPANY_ID = new CompanyId(
            UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final PluginId CUSTOMIZATION_ID = new PluginId("company_one_customization");

    @Test
    void exposesOnlyThePersistedLifecycleAndRequiresCustomization() {
        Company inactive = new Company(COMPANY_ID, CompanyStatus.INACTIVE, CUSTOMIZATION_ID, 0);
        Company active = new Company(COMPANY_ID, CompanyStatus.ACTIVE, CUSTOMIZATION_ID, 1);

        assertFalse(inactive.isActive());
        assertTrue(active.isActive());
        assertThrows(
                NullPointerException.class,
                () -> new Company(COMPANY_ID, CompanyStatus.ACTIVE, null, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Company(COMPANY_ID, CompanyStatus.ACTIVE, CUSTOMIZATION_ID, -1));
    }

    @Test
    void activationDecisionDistinguishesDesiredStateAndRejectsNegativeVersion() {
        PluginActivationDecision enabled = new PluginActivationDecision(
                COMPANY_ID, new PluginId("sales"), PluginActivationState.ENABLED, 2);
        PluginActivationDecision disabled = new PluginActivationDecision(
                COMPANY_ID, new PluginId("sales"), PluginActivationState.DISABLED, 3);

        assertTrue(enabled.isEnabled());
        assertFalse(disabled.isEnabled());
        assertThrows(
                IllegalArgumentException.class,
                () -> new PluginActivationDecision(
                        COMPANY_ID, new PluginId("sales"), PluginActivationState.ENABLED, -1));
    }
}
