package py.com.logixone.kernel.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;

class AuthorizedCompanyOperationTest {

    @Test
    void carriesOnlyTheRevalidatedTechnicalScope() {
        AuthorizedCompanyOperation authorization = authorization(
                "business_partners", "business_partners.manage", "request:1");

        assertEquals("business_partners", authorization.pluginId());
        assertEquals("business_partners.manage", authorization.permissionId());
    }

    @Test
    void rejectsUnqualifiedIdsAndUntrustedCorrelationSyntax() {
        assertThrows(IllegalArgumentException.class,
                () -> authorization("Business Partners", "business_partners.manage", "request:1"));
        assertThrows(IllegalArgumentException.class,
                () -> authorization("business_partners", "manage", "contains space"));
    }

    private static AuthorizedCompanyOperation authorization(
            String pluginId, String permissionId, String correlationId) {
        return new AuthorizedCompanyOperation(
                new AuthenticatedCompanyContext(
                        new AuthenticatedActor(new AppUserId(new UUID(0, 1))),
                        new CompanyId(new UUID(0, 2))),
                pluginId,
                permissionId,
                correlationId);
    }
}
