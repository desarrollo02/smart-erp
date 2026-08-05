package py.com.logixone.kernel.domain.security.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;

class SystemRoleValueTest {

    private static final String ROLE_ID = "00000000-0000-0000-0000-000000000101";

    @Test
    void parsesCanonicalOpaqueRoleId() {
        SystemRoleId id = SystemRoleId.parse(ROLE_ID);

        assertEquals(UUID.fromString(ROLE_ID), id.value());
        assertEquals(ROLE_ID, id.toString());
    }

    @Test
    void rejectsNonCanonicalRoleIdsAndCodes() {
        assertThrows(NullPointerException.class, () -> SystemRoleId.parse(null));
        assertThrows(IllegalArgumentException.class, () -> SystemRoleId.parse("not-a-uuid"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SystemRoleId.parse("00000000-0000-0000-0000-00000000010A"));

        for (String invalid : new String[] {
            "", "Admin", "system-admin", " system_admin", "system_admin "
        }) {
            assertThrows(IllegalArgumentException.class, () -> new SystemRoleCode(invalid));
        }
        assertThrows(
                IllegalArgumentException.class,
                () -> new SystemRoleCode("a".repeat(129)));
    }

    @Test
    void roleValidatesPresentationNameAndVersion() {
        SystemRoleId id = SystemRoleId.parse(ROLE_ID);
        SystemRoleCode code = new SystemRoleCode("system.admin");

        assertEquals(
                "Administrador global",
                new SystemRole(id, code, "Administrador global", SystemRoleStatus.ACTIVE, 0)
                        .displayName());

        for (String invalid : new String[] {"", " ", " nombre", "nombre ", "línea\nnueva"}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new SystemRole(id, code, invalid, SystemRoleStatus.ACTIVE, 0));
        }
        assertThrows(
                IllegalArgumentException.class,
                () -> new SystemRole(id, code, "a".repeat(161), SystemRoleStatus.ACTIVE, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SystemRole(id, code, "Administrador", SystemRoleStatus.ACTIVE, -1));
    }

    @Test
    void assignmentAndGrantUseOnlyUserRoleAndTypedPermission() {
        AppUserId userId = new AppUserId(UUID.fromString(
                "00000000-0000-0000-0000-000000000201"));
        SystemRoleId roleId = SystemRoleId.parse(ROLE_ID);

        assertEquals(
                userId,
                new AppUserSystemRoleAssignment(userId, roleId).userId());
        assertEquals(
                SystemPermission.AUDIT_VIEW,
                new SystemRolePermissionGrant(roleId, SystemPermission.AUDIT_VIEW).permission());
        assertThrows(
                NullPointerException.class,
                () -> new AppUserSystemRoleAssignment(null, roleId));
        assertThrows(
                NullPointerException.class,
                () -> new SystemRolePermissionGrant(roleId, null));
    }
}
