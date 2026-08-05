package py.com.logixone.kernel.application.security.access;

import java.util.List;
import java.util.Objects;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;

/** Request-scoped projection for the web shell; never store it in HTTP session. */
public record TrustedNavigationView(
        AuthenticatedCompanyContext context,
        String actorDisplayName,
        List<TrustedCompanyOption> companies,
        List<TrustedMenuItem> menuItems) {

    public TrustedNavigationView {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(actorDisplayName, "actorDisplayName");
        if (actorDisplayName.isBlank()
                || !actorDisplayName.equals(actorDisplayName.strip())
                || actorDisplayName.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("actorDisplayName must be safe display text");
        }
        companies = List.copyOf(Objects.requireNonNull(companies, "companies"));
        menuItems = List.copyOf(Objects.requireNonNull(menuItems, "menuItems"));
        if (companies.stream().noneMatch(option ->
                option.companyId().equals(context.companyId()))) {
            throw new IllegalArgumentException("companies must include the current company");
        }
    }
}
