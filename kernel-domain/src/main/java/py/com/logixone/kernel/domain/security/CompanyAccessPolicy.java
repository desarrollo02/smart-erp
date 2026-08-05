package py.com.logixone.kernel.domain.security;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;

/** Pure policy that never trusts a requested company without an active local membership. */
public final class CompanyAccessPolicy {

    public CompanySelectionResolution resolve(
            AppUser user,
            Collection<CompanyMembership> memberships,
            Optional<CompanyId> requestedCompanyId) {
        Objects.requireNonNull(user, "user");
        List<CompanyMembership> membershipList = List.copyOf(
                Objects.requireNonNull(memberships, "memberships"));
        requestedCompanyId = Objects.requireNonNull(requestedCompanyId, "requestedCompanyId");

        if (!user.isActive()) {
            return CompanySelectionResolution.denied(SecurityDiagnosticCode.USER_INACTIVE);
        }
        if (membershipList.stream().anyMatch(membership -> !membership.userId().equals(user.id()))) {
            return CompanySelectionResolution.denied(
                    SecurityDiagnosticCode.MEMBERSHIP_CONTEXT_INVALID);
        }

        List<CompanyId> availableCompanyIds = membershipList.stream()
                .filter(CompanyMembership::isActive)
                .map(CompanyMembership::companyId)
                .distinct()
                .sorted()
                .toList();

        if (requestedCompanyId.isPresent()) {
            CompanyId requested = requestedCompanyId.orElseThrow();
            return availableCompanyIds.contains(requested)
                    ? CompanySelectionResolution.selected(requested, availableCompanyIds)
                    : CompanySelectionResolution.denied(
                            SecurityDiagnosticCode.COMPANY_ACCESS_DENIED);
        }
        if (availableCompanyIds.isEmpty()) {
            return CompanySelectionResolution.denied(SecurityDiagnosticCode.MEMBERSHIP_REQUIRED);
        }
        if (availableCompanyIds.size() == 1) {
            return CompanySelectionResolution.selected(
                    availableCompanyIds.getFirst(),
                    availableCompanyIds);
        }
        return CompanySelectionResolution.selectionRequired(availableCompanyIds);
    }
}
