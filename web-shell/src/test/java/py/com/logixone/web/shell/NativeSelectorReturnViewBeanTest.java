package py.com.logixone.web.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.system.access.SystemAuthorityContext;
import py.com.logixone.web.security.TrustedAdminWebAccess;
import py.com.logixone.web.security.TrustedCompanySession;
import py.com.logixone.web.security.TrustedWebAccessException;
import py.com.logixone.web.selector.NativeSelectorSourceCatalog;

class NativeSelectorReturnViewBeanTest {

    private static final AppUserId USER = new AppUserId(
            UUID.fromString("00000000-0000-0000-0000-000000000001"));

    @Test
    void postNavigationKeepsBusinessValuesServerSideAndConsumesThemAtTheOrigin() {
        var store = new NativeSelectorReturnContextStore();
        var session = new TrustedCompanySession();
        var access = new AllowedAccess(Set.of(SystemPermission.COMPANY_MANAGE));
        var origin = bean(
                "/logixone/faces/admin/plugins.xhtml",
                Map.of("selectorDraft", "company_id=company-1&forged=ignored"),
                store, session, access);

        String targetOutcome = origin.open(NativeSelectorSourceCatalog.PLUGINS_COMPANY);

        assertTrue(targetOutcome.startsWith(
                "/admin/companies.xhtml?faces-redirect=true&selectorContext="));
        assertFalse(targetOutcome.contains("company-1"));
        String token = targetOutcome.substring(targetOutcome.lastIndexOf('=') + 1);

        var target = bean(
                "/logixone/faces/admin/companies.xhtml",
                Map.of("selectorContext", token), store, session, access);
        assertTrue(target.isReturnAvailable());
        assertEquals("Volver a configuración de plugins", target.getReturnLabel());
        assertEquals(
                "/admin/plugins.xhtml?faces-redirect=true&selectorReturn=" + token,
                target.returnToOrigin());
        assertTrue(target.preserve("/admin/companies.xhtml?faces-redirect=true")
                .endsWith("&selectorContext=" + token));

        var restoredOrigin = bean(
                "/logixone/faces/admin/plugins.xhtml",
                Map.of("selectorReturn", token), store, session, access);
        NativeSelectorReturnRestoration restoration = restoredOrigin
                .restore("/admin/plugins.xhtml").orElseThrow();
        assertEquals(Map.of("company_id", "company-1"), restoration.inputs());
        assertTrue(restoredOrigin.isRestored());
        assertTrue(restoredOrigin.restore("/admin/plugins.xhtml").isEmpty());
    }

    @Test
    void rejectsAUsageOpenedFromAnUnexpectedRouteOrWithoutCurrentPermission() {
        var store = new NativeSelectorReturnContextStore();
        var session = new TrustedCompanySession();
        var wrongRoute = bean(
                "/logixone/faces/admin/security.xhtml",
                Map.of("selectorDraft", "company_id=company-1"),
                store, session, new AllowedAccess(Set.of(SystemPermission.COMPANY_MANAGE)));
        assertNull(wrongRoute.open(NativeSelectorSourceCatalog.PLUGINS_COMPANY));

        var denied = bean(
                "/logixone/faces/admin/plugins.xhtml",
                Map.of("selectorDraft", "company_id=company-1"),
                store, session, new AllowedAccess(Set.of(SystemPermission.AUDIT_VIEW)));
        assertNull(denied.open(NativeSelectorSourceCatalog.PLUGINS_COMPANY));
    }

    @Test
    void normalizesOnlyApplicationLocalFacesRoutes() {
        assertEquals("/admin/security.xhtml", NativeSelectorReturnViewBean.applicationRoute(
                "/logixone/faces/admin/security.xhtml", "/logixone"));
        assertEquals("/admin/security.xhtml", NativeSelectorReturnViewBean.applicationRoute(
                "/logixone/admin/security.xhtml", "/logixone"));
        assertEquals("", NativeSelectorReturnViewBean.applicationRoute("//outside", ""));
    }

    @Test
    void acceptsOnlyCanonicalUuidViewParameterValues() {
        var bean = new NativeSelectorReturnViewBean();
        String token = "00000000-0000-0000-0000-000000000001";

        bean.setSelectorContextId(token);
        assertEquals(token, bean.getSelectorContextId());

        bean.setSelectorContextId("not-a-token");
        assertNull(bean.getSelectorContextId());
    }

    private static NativeSelectorReturnViewBean bean(
            String uri,
            Map<String, String> parameters,
            NativeSelectorReturnContextStore store,
            TrustedCompanySession session,
            TrustedAdminWebAccess access) {
        var bean = new NativeSelectorReturnViewBean();
        bean.contexts = store;
        bean.companySession = session;
        bean.access = access;
        bean.request = request(uri, parameters);
        bean.initialize();
        return bean;
    }

    private static HttpServletRequest request(String uri, Map<String, String> parameters) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getParameter" -> parameters.get((String) args[0]);
                    case "getRequestURI" -> uri;
                    case "getContextPath" -> "/logixone";
                    case "toString" -> "NativeSelectorReturnRequest";
                    default -> method.getReturnType().isPrimitive()
                            ? primitiveDefault(method.getReturnType()) : null;
                });
    }

    private static Object primitiveDefault(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }

    private static final class AllowedAccess extends TrustedAdminWebAccess {
        private final Set<SystemPermission> permissions;
        private final SystemAuthorityContext context;

        private AllowedAccess(Set<SystemPermission> permissions) {
            this.permissions = permissions;
            context = new SystemAuthorityContext(USER, permissions);
        }

        @Override
        public SystemAuthorityContext requireAny() {
            return context;
        }

        @Override
        public SystemAuthorityContext require(SystemPermission permission) {
            if (!permissions.contains(permission)) {
                throw TrustedWebAccessException.forbidden();
            }
            return context;
        }
    }
}
