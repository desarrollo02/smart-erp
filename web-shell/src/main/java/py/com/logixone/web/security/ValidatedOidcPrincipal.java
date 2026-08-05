package py.com.logixone.web.security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Optional;
import py.com.logixone.kernel.domain.security.ExternalIdentity;

/** Converts only a container-validated OIDC principal into a neutral identity. */
@RequestScoped
public class ValidatedOidcPrincipal {

    private static final String OIDC_AUTH_TYPE = "OIDC";
    private static final String PROVIDER_URL_VARIABLE = "LOGIXONE_OIDC_PROVIDER_URL";

    @Inject
    HttpServletRequest request;

    public Optional<ExternalIdentity> currentIdentity() {
        Principal principal = request.getUserPrincipal();
        if (principal == null || !OIDC_AUTH_TYPE.equalsIgnoreCase(request.getAuthType())) {
            return Optional.empty();
        }

        String issuer = System.getenv(PROVIDER_URL_VARIABLE);
        if (issuer == null || issuer.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(new ExternalIdentity(issuer, principal.getName()));
        } catch (IllegalArgumentException | NullPointerException invalidIdentity) {
            return Optional.empty();
        }
    }
}
