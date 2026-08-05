package py.com.logixone.kernel.application.security.access;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AuthenticatedActor;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;

/** Current server-side resolution for one validated external identity. */
public record TrustedCompanyAccess(
        TrustedCompanyAccessStatus status,
        Optional<AuthenticatedActor> actor,
        Optional<AuthenticatedCompanyContext> context,
        List<CompanyId> availableCompanyIds,
        Optional<TrustedAccessCode> failure) {

    public TrustedCompanyAccess {
        Objects.requireNonNull(status, "status");
        actor = Objects.requireNonNull(actor, "actor");
        context = Objects.requireNonNull(context, "context");
        availableCompanyIds = Objects.requireNonNull(availableCompanyIds, "availableCompanyIds")
                .stream()
                .map(companyId -> Objects.requireNonNull(companyId, "available company id"))
                .distinct()
                .sorted()
                .toList();
        failure = Objects.requireNonNull(failure, "failure");

        switch (status) {
            case SELECTED -> {
                if (actor.isEmpty()
                        || context.isEmpty()
                        || failure.isPresent()
                        || !context.orElseThrow().actor().equals(actor.orElseThrow())
                        || !availableCompanyIds.contains(context.orElseThrow().companyId())) {
                    throw new IllegalArgumentException(
                            "selected access requires actor, current context and available company");
                }
            }
            case SELECTION_REQUIRED -> {
                if (actor.isEmpty()
                        || context.isPresent()
                        || availableCompanyIds.size() < 2
                        || failure.orElse(null) != TrustedAccessCode.COMPANY_SELECTION_REQUIRED) {
                    throw new IllegalArgumentException(
                            "selection-required access needs actor and multiple available companies");
                }
            }
            case FORBIDDEN -> {
                if (context.isPresent() || !availableCompanyIds.isEmpty() || failure.isEmpty()) {
                    throw new IllegalArgumentException(
                            "forbidden access cannot expose context or company options");
                }
            }
        }
    }

    public static TrustedCompanyAccess selected(
            AuthenticatedActor actor,
            CompanyId selectedCompanyId,
            List<CompanyId> availableCompanyIds) {
        Objects.requireNonNull(actor, "actor");
        return new TrustedCompanyAccess(
                TrustedCompanyAccessStatus.SELECTED,
                Optional.of(actor),
                Optional.of(new AuthenticatedCompanyContext(actor, selectedCompanyId)),
                availableCompanyIds,
                Optional.empty());
    }

    public static TrustedCompanyAccess selectionRequired(
            AuthenticatedActor actor,
            List<CompanyId> availableCompanyIds) {
        return new TrustedCompanyAccess(
                TrustedCompanyAccessStatus.SELECTION_REQUIRED,
                Optional.of(Objects.requireNonNull(actor, "actor")),
                Optional.empty(),
                availableCompanyIds,
                Optional.of(TrustedAccessCode.COMPANY_SELECTION_REQUIRED));
    }

    public static TrustedCompanyAccess forbidden(
            Optional<AuthenticatedActor> actor,
            TrustedAccessCode failure) {
        return new TrustedCompanyAccess(
                TrustedCompanyAccessStatus.FORBIDDEN,
                Objects.requireNonNull(actor, "actor"),
                Optional.empty(),
                List.of(),
                Optional.of(Objects.requireNonNull(failure, "failure")));
    }
}
