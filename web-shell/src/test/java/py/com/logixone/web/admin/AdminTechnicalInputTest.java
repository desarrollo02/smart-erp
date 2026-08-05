package py.com.logixone.web.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.security.SystemPermission;

class AdminTechnicalInputTest {

    @Test
    void parsesCanonicalTechnicalIdentifiersWithoutNormalizingBrowserInput() {
        assertEquals(
                "00000000-0000-0000-0000-000000000001",
                AdminTechnicalInput.companyId(
                        "00000000-0000-0000-0000-000000000001").toString());
        assertEquals("inventory", AdminTechnicalInput.pluginId("inventory").toString());
        assertEquals(
                SystemPermission.AUDIT_VIEW,
                AdminTechnicalInput.systemPermission("kernel.audit.view"));
        assertEquals("Texto visible", AdminTechnicalInput.requiredText("Texto visible", "text"));
        assertEquals(7L, AdminTechnicalInput.version("7"));
    }

    @Test
    void rejectsBlankMalformedAndUnknownCandidatesFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> AdminTechnicalInput.companyId(" "));
        assertThrows(IllegalArgumentException.class, () -> AdminTechnicalInput.userId("not-a-uuid"));
        assertThrows(IllegalArgumentException.class, () -> AdminTechnicalInput.roleId(null));
        assertThrows(IllegalArgumentException.class, () -> AdminTechnicalInput.pluginId("Inventory"));
        assertThrows(IllegalArgumentException.class, () -> AdminTechnicalInput.version("-1"));
        assertThrows(IllegalArgumentException.class, () -> AdminTechnicalInput.version("current"));
        assertThrows(
                IllegalArgumentException.class,
                () -> AdminTechnicalInput.systemPermission("kernel.future.manage"));
        assertThrows(
                IllegalArgumentException.class,
                () -> AdminTechnicalInput.requiredText("", "display name"));
    }
}
