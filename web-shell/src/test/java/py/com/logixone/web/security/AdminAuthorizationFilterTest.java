package py.com.logixone.web.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.SystemPermission;
import py.com.logixone.kernel.application.security.system.access.SystemAuthorityContext;

class AdminAuthorizationFilterTest {

    @Test
    void requiresExactPermissionAndAppliesHeadersBeforeAllowedAuditRoute() throws Exception {
        StubAccess access = new StubAccess();
        ResponseState response = new ResponseState();
        boolean[] chained = {false};

        filter(access).doFilter(
                request("/logixone/faces/admin/audit.xhtml"),
                response.proxy(),
                chain(chained));

        assertTrue(chained[0]);
        assertEquals(SystemPermission.AUDIT_VIEW, access.requiredPermission);
        assertFalse(access.anyRequired);
        assertSecurityHeaders(response.headers);
        assertEquals("no-store, max-age=0", response.headers.get("Cache-Control"));
    }

    @Test
    void supportsFacesAndDirectMappingsAndUsesAnyPermissionOnlyForLanding() throws Exception {
        StubAccess companies = new StubAccess();
        filter(companies).doFilter(
                request("/logixone/admin/companies.xhtml"),
                new ResponseState().proxy(),
                chain(new boolean[] {false}));
        assertEquals(SystemPermission.COMPANY_MANAGE, companies.requiredPermission);

        StubAccess landing = new StubAccess();
        filter(landing).doFilter(
                request("/logixone/faces/admin/index.xhtml"),
                new ResponseState().proxy(),
                chain(new boolean[] {false}));
        assertTrue(landing.anyRequired);
    }

    @Test
    void deniedResponseIsGenericHardenedAndDoesNotContinueTheChain() throws Exception {
        StubAccess access = new StubAccess();
        access.failure = TrustedWebAccessException.forbidden();
        ResponseState response = new ResponseState();
        boolean[] chained = {false};

        filter(access).doFilter(
                request("/logixone/admin/security.xhtml"),
                response.proxy(),
                chain(chained));

        assertFalse(chained[0]);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.status);
        assertEquals("UTF-8", response.characterEncoding);
        assertEquals("text/html", response.contentType);
        assertTrue(response.body.toString().contains("Acceso no disponible"));
        assertFalse(response.body.toString().contains("forbidden"));
        assertSecurityHeaders(response.headers);
        assertTrue(response.headers.get("Content-Security-Policy").contains("default-src 'none'"));
    }

    @Test
    void anonymousDenialAddsOidcChallengeAndUnexpectedFailureReturns503() throws Exception {
        StubAccess unauthorized = new StubAccess();
        unauthorized.failure = TrustedWebAccessException.unauthorized();
        ResponseState unauthorizedResponse = new ResponseState();
        filter(unauthorized).doFilter(
                request("/logixone/admin/index.xhtml"),
                unauthorizedResponse.proxy(),
                chain(new boolean[] {false}));
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, unauthorizedResponse.status);
        assertEquals("OIDC", unauthorizedResponse.headers.get("WWW-Authenticate"));

        StubAccess unavailable = new StubAccess();
        unavailable.failure = new IllegalStateException("sensitive detail");
        ResponseState unavailableResponse = new ResponseState();
        filter(unavailable).doFilter(
                request("/logixone/admin/plugins.xhtml"),
                unavailableResponse.proxy(),
                chain(new boolean[] {false}));
        assertEquals(HttpServletResponse.SC_SERVICE_UNAVAILABLE, unavailableResponse.status);
        assertFalse(unavailableResponse.body.toString().contains("sensitive detail"));
    }

    private static AdminAuthorizationFilter filter(StubAccess access) {
        AdminAuthorizationFilter filter = new AdminAuthorizationFilter();
        filter.access = access;
        return filter;
    }

    private static HttpServletRequest request(String uri) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                AdminAuthorizationFilterTest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getRequestURI")) {
                        return uri;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static FilterChain chain(boolean[] called) {
        return (request, response) -> called[0] = true;
    }

    private static void assertSecurityHeaders(Map<String, String> headers) {
        assertEquals("nosniff", headers.get("X-Content-Type-Options"));
        assertEquals("DENY", headers.get("X-Frame-Options"));
        assertEquals("no-referrer", headers.get("Referrer-Policy"));
        assertEquals("no-cache", headers.get("Pragma"));
        assertTrue(headers.get("Permissions-Policy").contains("camera=()"));
        assertTrue(headers.get("Content-Security-Policy").contains("frame-ancestors 'none'"));
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }

    private static final class StubAccess extends TrustedAdminWebAccess {
        private static final SystemAuthorityContext CONTEXT = new SystemAuthorityContext(
                new AppUserId(UUID.fromString("00000000-0000-0000-0000-000000000201")),
                Set.of(SystemPermission.AUDIT_VIEW));

        private boolean anyRequired;
        private SystemPermission requiredPermission;
        private RuntimeException failure;

        @Override
        public SystemAuthorityContext requireAny() {
            anyRequired = true;
            failIfConfigured();
            return CONTEXT;
        }

        @Override
        public SystemAuthorityContext require(SystemPermission permission) {
            requiredPermission = permission;
            failIfConfigured();
            return CONTEXT;
        }

        private void failIfConfigured() {
            Optional.ofNullable(failure).ifPresent(value -> {
                throw value;
            });
        }
    }

    private static final class ResponseState {
        private final Map<String, String> headers = new LinkedHashMap<>();
        private final StringWriter body = new StringWriter();
        private final PrintWriter writer = new PrintWriter(body);
        private int status = HttpServletResponse.SC_OK;
        private String characterEncoding;
        private String contentType;

        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    AdminAuthorizationFilterTest.class.getClassLoader(),
                    new Class<?>[] {HttpServletResponse.class},
                    (proxy, method, arguments) -> switch (method.getName()) {
                        case "setHeader" -> {
                            headers.put((String) arguments[0], (String) arguments[1]);
                            yield null;
                        }
                        case "setStatus" -> {
                            status = (Integer) arguments[0];
                            yield null;
                        }
                        case "setCharacterEncoding" -> {
                            characterEncoding = (String) arguments[0];
                            yield null;
                        }
                        case "setContentType" -> {
                            contentType = (String) arguments[0];
                            yield null;
                        }
                        case "getWriter" -> writer;
                        case "isCommitted" -> false;
                        case "reset" -> {
                            headers.clear();
                            status = HttpServletResponse.SC_OK;
                            yield null;
                        }
                        default -> defaultValue(method.getReturnType());
                    });
        }
    }
}
