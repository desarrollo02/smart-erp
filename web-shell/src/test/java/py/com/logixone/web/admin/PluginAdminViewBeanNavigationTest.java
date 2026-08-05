package py.com.logixone.web.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;

class PluginAdminViewBeanNavigationTest {

    @Test
    void redirectsWithinTheCurrentAdminDirectoryWithoutDuplicatingIt() {
        CompanyId companyId = new CompanyId(
                UUID.fromString("10000000-0000-4000-8000-000000000001"));

        assertEquals(
                "plugins.xhtml?faces-redirect=true&company=" + companyId,
                PluginAdminViewBean.pluginRedirect(companyId));
    }
}
