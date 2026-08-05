package py.com.logixone.kernel.application.security.port;

import java.util.List;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.domain.security.CompanyMembership;

public interface CompanyMembershipRepository {

    List<CompanyMembership> findByUserId(AppUserId userId);

    List<CompanyMembership> findByCompanyId(CompanyId companyId);

    Optional<CompanyMembership> findByUserAndCompany(
            AppUserId userId,
            CompanyId companyId);

    /** Persists a new membership or an idempotent/versioned replacement. */
    CompanyMembership save(CompanyMembership membership);
}
