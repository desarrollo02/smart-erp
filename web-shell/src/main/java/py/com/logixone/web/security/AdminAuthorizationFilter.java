package py.com.logixone.web.security;

import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import py.com.logixone.kernel.api.security.SystemPermission;

/** Enforces current global authority before any `/admin/*` resource executes. */
@WebFilter(
        filterName = "AdminAuthorizationFilter",
        urlPatterns = {"/admin/*", "/faces/admin/*"})
public class AdminAuthorizationFilter implements Filter {

    private static final Logger LOGGER =
            System.getLogger(AdminAuthorizationFilter.class.getName());
    private static final String DENIED_DOCUMENT = """
            <!doctype html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width,initial-scale=1">
              <title>Acceso no disponible</title>
              <style>
                body{margin:0;min-height:100vh;display:grid;place-items:center;padding:1.5rem;
                box-sizing:border-box;font-family:system-ui,sans-serif;color:#171d1c;background:#e9efed}
                main{max-width:34rem;padding:2.5rem;border:1px solid #bec9c6;border-radius:1.75rem;
                background:#f6fbf9;box-shadow:0 8px 20px #171d1c26;text-align:center}
                h1{margin:.5rem 0 1rem;font-size:clamp(1.8rem,6vw,2.6rem)}
                p{margin:0;color:#3f4947;line-height:1.6}
              </style>
            </head>
            <body><main><small>Logixone</small><h1>Acceso no disponible</h1>
            <p>No podemos abrir esta zona para la sesión actual.</p></main></body>
            </html>
            """;

    @Inject
    TrustedAdminWebAccess access;

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        applySecurityHeaders(httpResponse);
        try {
            SystemPermission permission = requiredPermission((HttpServletRequest) request);
            if (permission == null) {
                access.requireAny();
            } else {
                access.require(permission);
            }
        } catch (TrustedWebAccessException denied) {
            writeDenied(httpResponse, denied.status());
            return;
        } catch (RuntimeException failure) {
            LOGGER.log(Level.ERROR,
                    "event=admin_authorization_failed type={0}",
                    failure.getClass().getName());
            writeDenied(httpResponse, HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        chain.doFilter(request, response);
    }

    private static SystemPermission requiredPermission(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.endsWith("/admin/companies.xhtml")) {
            return SystemPermission.COMPANY_MANAGE;
        }
        if (uri.endsWith("/admin/plugins.xhtml")) {
            return SystemPermission.PLUGIN_MANAGE;
        }
        if (uri.endsWith("/admin/security.xhtml")) {
            return SystemPermission.SECURITY_MANAGE;
        }
        if (uri.endsWith("/admin/system-authority.xhtml")) {
            return SystemPermission.SYSTEM_ADMINISTRATION_MANAGE;
        }
        if (uri.endsWith("/admin/audit.xhtml")) {
            return SystemPermission.AUDIT_VIEW;
        }
        return null;
    }

    private static void applySecurityHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        response.setHeader(
                "Content-Security-Policy",
                "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self'; "
                        + "img-src 'self' data:; form-action 'self'; base-uri 'none'; "
                        + "frame-ancestors 'none'; object-src 'none'");
    }

    private static void writeDenied(HttpServletResponse response, int status) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.reset();
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html");
        applySecurityHeaders(response);
        response.setHeader(
                "Content-Security-Policy",
                "default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; frame-ancestors 'none'");
        if (status == HttpServletResponse.SC_UNAUTHORIZED) {
            response.setHeader("WWW-Authenticate", "OIDC");
        }
        response.getWriter().write(DENIED_DOCUMENT);
    }
}
