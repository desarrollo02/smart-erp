package py.com.logixone.kernel.infrastructure.jakarta.health;

import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.regex.Pattern;
import py.com.logixone.kernel.application.health.HealthStatus;
import py.com.logixone.kernel.application.health.ReadinessCheck;

/** Local-only OIDC configuration check. It never calls the identity provider. */
@ApplicationScoped
public class OidcConfigurationReadinessCheck implements ReadinessCheck {

    private static final Pattern CLIENT_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");

    @Override
    public String name() {
        return "oidc-configuration";
    }

    @Override
    public HealthStatus check() {
        return isValid(System.getenv()) ? HealthStatus.UP : HealthStatus.DOWN;
    }

    static boolean isValid(Map<String, String> environment) {
        String providerUrl = environment.get("LOGIXONE_OIDC_PROVIDER_URL");
        String clientId = environment.get("LOGIXONE_OIDC_CLIENT_ID");
        String clientSecret = environment.get("LOGIXONE_OIDC_CLIENT_SECRET");
        String postLogoutRedirect = environment.get("LOGIXONE_OIDC_POST_LOGOUT_REDIRECT_URI");

        return isOidcUrl(providerUrl)
                && clientId != null
                && CLIENT_ID.matcher(clientId).matches()
                && isSecret(clientSecret)
                && isRedirectUrl(postLogoutRedirect);
    }

    private static boolean isOidcUrl(String value) {
        URI uri = parseHttpUrl(value);
        return uri != null && ("https".equals(uri.getScheme()) || isLocalHost(uri.getHost()));
    }

    private static boolean isRedirectUrl(String value) {
        URI uri = parseHttpUrl(value);
        return uri != null && ("https".equals(uri.getScheme()) || isLocalHost(uri.getHost()));
    }

    private static URI parseHttpUrl(String value) {
        if (value == null || value.isBlank() || !value.equals(value.strip())) {
            return null;
        }
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (!uri.isAbsolute()
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null
                    || !("http".equals(scheme) || "https".equals(scheme))
                    || !uri.normalize().equals(uri)) {
                return null;
            }
            return uri;
        } catch (URISyntaxException failure) {
            return null;
        }
    }

    private static boolean isLocalHost(String host) {
        return "localhost".equals(host)
                || "127.0.0.1".equals(host)
                || host.endsWith(".localhost");
    }

    private static boolean isSecret(String value) {
        return value != null
                && !value.isBlank()
                && value.length() <= 4096
                && value.codePoints().noneMatch(Character::isISOControl);
    }
}
