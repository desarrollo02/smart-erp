package py.com.logixone.kernel.application.security.access;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;

/** Presentation-safe option for a company already authorized for the actor. */
public record TrustedCompanyOption(
        CompanyId companyId,
        String presentationLabel) {

    public TrustedCompanyOption {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(presentationLabel, "presentationLabel");
        if (presentationLabel.isBlank()
                || !presentationLabel.equals(presentationLabel.strip())
                || presentationLabel.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("presentationLabel must be safe display text");
        }
    }
}
