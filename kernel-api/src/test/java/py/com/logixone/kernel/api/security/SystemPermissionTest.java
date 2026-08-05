package py.com.logixone.kernel.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SystemPermissionTest {

    @Test
    void exposesExactlyTheFiveInitialKernelPermissions() {
        assertEquals(
                Set.of(
                        SystemPermission.COMPANY_MANAGE,
                        SystemPermission.PLUGIN_MANAGE,
                        SystemPermission.SECURITY_MANAGE,
                        SystemPermission.AUDIT_VIEW,
                        SystemPermission.SYSTEM_ADMINISTRATION_MANAGE),
                SystemPermission.knownPermissions());
        assertEquals(5, SystemPermission.knownPermissions().size());
    }

    @Test
    void acceptsCanonicalExtensiblePermissionAndKeepsItsStableValue() {
        SystemPermission permission = new SystemPermission("kernel.report.monthly.view");

        assertEquals("kernel.report.monthly.view", permission.value());
        assertEquals("kernel.report.monthly.view", permission.toString());
        assertTrue(permission.compareTo(SystemPermission.SECURITY_MANAGE) < 0);
    }

    @Test
    void rejectsNullNonCanonicalAndOversizedValues() {
        assertThrows(NullPointerException.class, () -> new SystemPermission(null));
        for (String invalid : new String[] {
            "", "kernel", "kernel.company", "Kernel.company.manage",
            "kernel.company-manage", " kernel.company.manage", "kernel.company.manage "
        }) {
            assertThrows(IllegalArgumentException.class, () -> new SystemPermission(invalid));
        }
        assertThrows(
                IllegalArgumentException.class,
                () -> new SystemPermission("kernel.permission." + "a".repeat(111)));
    }
}
