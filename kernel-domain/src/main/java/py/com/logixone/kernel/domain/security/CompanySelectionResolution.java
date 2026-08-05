package py.com.logixone.kernel.domain.security;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;

/** Deterministic result for zero, one or multiple active company memberships. */
public record CompanySelectionResolution(
        CompanySelectionStatus status,
        Optional<CompanyId> selectedCompanyId,
        List<CompanyId> availableCompanyIds,
        Optional<SecurityDiagnosticCode> failure) {

    public CompanySelectionResolution {
        Objects.requireNonNull(status, "status");
        selectedCompanyId = Objects.requireNonNull(selectedCompanyId, "selectedCompanyId");
        availableCompanyIds = Objects.requireNonNull(availableCompanyIds, "availableCompanyIds")
                .stream()
                .map(companyId -> Objects.requireNonNull(companyId, "available company id"))
                .distinct()
                .sorted()
                .toList();
        failure = Objects.requireNonNull(failure, "failure");

        switch (status) {
            case SELECTED -> {
                if (selectedCompanyId.isEmpty()
                        || failure.isPresent()
                        || !availableCompanyIds.contains(selectedCompanyId.orElseThrow())) {
                    throw new IllegalArgumentException(
                            "selected result requires an available company and no failure");
                }
            }
            case SELECTION_REQUIRED -> {
                if (selectedCompanyId.isPresent()
                        || availableCompanyIds.size() < 2
                        || failure.orElse(null) != SecurityDiagnosticCode.COMPANY_SELECTION_REQUIRED) {
                    throw new IllegalArgumentException(
                            "selection-required result requires multiple companies and its diagnostic");
                }
            }
            case DENIED -> {
                if (selectedCompanyId.isPresent()
                        || !availableCompanyIds.isEmpty()
                        || failure.isEmpty()
                        || failure.orElseThrow() == SecurityDiagnosticCode.COMPANY_SELECTION_REQUIRED) {
                    throw new IllegalArgumentException(
                            "denied result requires only a non-selection failure");
                }
            }
        }
    }

    public static CompanySelectionResolution selected(
            CompanyId selectedCompanyId,
            List<CompanyId> availableCompanyIds) {
        return new CompanySelectionResolution(
                CompanySelectionStatus.SELECTED,
                Optional.of(Objects.requireNonNull(selectedCompanyId, "selectedCompanyId")),
                availableCompanyIds,
                Optional.empty());
    }

    public static CompanySelectionResolution selectionRequired(List<CompanyId> availableCompanyIds) {
        return new CompanySelectionResolution(
                CompanySelectionStatus.SELECTION_REQUIRED,
                Optional.empty(),
                availableCompanyIds,
                Optional.of(SecurityDiagnosticCode.COMPANY_SELECTION_REQUIRED));
    }

    public static CompanySelectionResolution denied(SecurityDiagnosticCode failure) {
        return new CompanySelectionResolution(
                CompanySelectionStatus.DENIED,
                Optional.empty(),
                List.of(),
                Optional.of(Objects.requireNonNull(failure, "failure")));
    }
}
