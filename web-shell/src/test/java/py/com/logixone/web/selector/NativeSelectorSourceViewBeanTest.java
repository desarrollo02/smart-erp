package py.com.logixone.web.selector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.system.access.SystemAuthorityContext;
import py.com.logixone.web.security.TrustedAdminWebAccess;
import py.com.logixone.web.security.TrustedWebAccessException;

class NativeSelectorSourceViewBeanTest {

    @Test
    void exposesOnlyRoutesAuthorizedByTheCurrentSystemContext() {
        var bean = bean(Set.of(SystemPermission.COMPANY_MANAGE));

        var company = bean.getSources().get(NativeSelectorSourceCatalog.PLUGINS_COMPANY);
        var user = bean.getSources().get(NativeSelectorSourceCatalog.SYSTEM_ASSIGNMENT_USER);
        var audit = bean.getSources().get(NativeSelectorSourceCatalog.AUDIT_CATEGORY);

        assertEquals(18, bean.getSources().size());
        assertEquals(NativeSelectorSourceCatalog.PLUGINS_COMPANY, company.getUsageId());
        assertTrue(company.isManagementAvailable());
        assertEquals("/admin/companies.xhtml", company.getManagementRoute());
        assertFalse(user.isManagementAvailable());
        assertEquals("", user.getManagementRoute());
        assertFalse(audit.isManagementAvailable());
        assertTrue(audit.getHelpText().contains("opciones cerradas"));
    }

    @Test
    void keepsAllManagementRoutesHiddenForANonAdministrativeIdentity() {
        var bean = new NativeSelectorSourceViewBean();
        bean.access = new DeniedAccess();

        bean.initialize();

        assertTrue(bean.getSources().values().stream()
                .noneMatch(NativeSelectorSourceView::isManagementAvailable));
        assertTrue(bean.getSources().values().stream()
                .allMatch(source -> !source.getHelpText().isBlank()));
    }

    private static NativeSelectorSourceViewBean bean(Set<SystemPermission> permissions) {
        var bean = new NativeSelectorSourceViewBean();
        bean.access = new AllowedAccess(new SystemAuthorityContext(
                new AppUserId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                permissions));
        bean.initialize();
        return bean;
    }

    private static final class AllowedAccess extends TrustedAdminWebAccess {
        private final SystemAuthorityContext context;

        private AllowedAccess(SystemAuthorityContext context) {
            this.context = context;
        }

        @Override
        public SystemAuthorityContext requireAny() {
            return context;
        }
    }

    private static final class DeniedAccess extends TrustedAdminWebAccess {
        @Override
        public SystemAuthorityContext requireAny() {
            throw TrustedWebAccessException.forbidden();
        }
    }
}
