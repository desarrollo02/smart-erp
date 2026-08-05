package py.com.logixone.kernel.application.company.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.domain.company.CompanyStatus;
import py.com.logixone.kernel.domain.company.PluginActivationState;
import py.com.logixone.plugin.api.PluginId;

class CompanyCommandTest {

    private static final CompanyId COMPANY_ID =
            new CompanyId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final PluginId FUNCTIONAL_PLUGIN_ID = new PluginId("sales");
    private static final PluginId CUSTOMIZATION_PLUGIN_ID = new PluginId("custom_company_a");

    @Test
    void retainsExplicitCommandIntent() {
        RegisterCompanyCommand register = new RegisterCompanyCommand(CUSTOMIZATION_PLUGIN_ID);
        ChangeCompanyStatusCommand status =
                new ChangeCompanyStatusCommand(COMPANY_ID, CompanyStatus.ACTIVE, 2);
        ChangePluginActivationCommand activation = new ChangePluginActivationCommand(
                COMPANY_ID, FUNCTIONAL_PLUGIN_ID, PluginActivationState.ENABLED, 3);
        ReplaceCustomizationCommand replacement =
                new ReplaceCustomizationCommand(COMPANY_ID, CUSTOMIZATION_PLUGIN_ID, 4);

        assertEquals(CUSTOMIZATION_PLUGIN_ID, register.customizationPluginId());
        assertEquals(2, status.expectedVersion());
        assertEquals(PluginActivationState.ENABLED, activation.desiredState());
        assertEquals(CUSTOMIZATION_PLUGIN_ID, replacement.newCustomizationPluginId());
    }

    @Test
    void rejectsMissingRequiredValues() {
        assertThrows(NullPointerException.class, () -> new RegisterCompanyCommand(null));
        assertThrows(
                NullPointerException.class,
                () -> new ChangeCompanyStatusCommand(null, CompanyStatus.ACTIVE, 0));
        assertThrows(
                NullPointerException.class,
                () -> new ChangePluginActivationCommand(
                        COMPANY_ID, null, PluginActivationState.ENABLED, 0));
        assertThrows(
                NullPointerException.class,
                () -> new ReplaceCustomizationCommand(COMPANY_ID, null, 0));
    }

    @Test
    void rejectsNegativeExpectedVersions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ChangeCompanyStatusCommand(COMPANY_ID, CompanyStatus.ACTIVE, -1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ChangePluginActivationCommand(
                        COMPANY_ID, FUNCTIONAL_PLUGIN_ID, PluginActivationState.ENABLED, -1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ReplaceCustomizationCommand(COMPANY_ID, CUSTOMIZATION_PLUGIN_ID, -1));
    }
}
