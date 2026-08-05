package py.com.logixone.kernel.domain.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;

/** Stable OIDC identity composed from the validated issuer and subject claims. */
public record ExternalIdentity(String issuer, String subject)
        implements Comparable<ExternalIdentity> {

    private static final int MAX_ISSUER_LENGTH = 2048;
    private static final int MAX_SUBJECT_LENGTH = 255;

    public ExternalIdentity {
        issuer = validateIssuer(issuer);
        subject = validateSubject(subject);
    }

    private static String validateIssuer(String value) {
        Objects.requireNonNull(value, "issuer");
        if (value.isBlank()
                || value.length() > MAX_ISSUER_LENGTH
                || !value.equals(value.strip())) {
            throw new IllegalArgumentException("issuer must be a canonical OIDC URL");
        }

        URI parsed;
        try {
            parsed = new URI(value);
        } catch (URISyntaxException failure) {
            throw new IllegalArgumentException("issuer must be a canonical OIDC URL", failure);
        }

        String scheme = parsed.getScheme();
        String host = parsed.getHost();
        if (!parsed.isAbsolute()
                || scheme == null
                || host == null
                || !(scheme.equals("https") || scheme.equals("http"))
                || !scheme.equals(scheme.toLowerCase(Locale.ROOT))
                || !host.equals(host.toLowerCase(Locale.ROOT))
                || parsed.getUserInfo() != null
                || parsed.getQuery() != null
                || parsed.getFragment() != null
                || !parsed.normalize().toString().equals(value)) {
            throw new IllegalArgumentException("issuer must be a canonical OIDC URL");
        }
        return value;
    }

    private static String validateSubject(String value) {
        Objects.requireNonNull(value, "subject");
        if (value.isBlank()
                || value.length() > MAX_SUBJECT_LENGTH
                || !value.equals(value.strip())
                || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("subject must contain a valid OIDC subject");
        }
        return value;
    }

    @Override
    public int compareTo(ExternalIdentity other) {
        Objects.requireNonNull(other, "other");
        int issuerComparison = issuer.compareTo(other.issuer);
        return issuerComparison != 0 ? issuerComparison : subject.compareTo(other.subject);
    }
}
