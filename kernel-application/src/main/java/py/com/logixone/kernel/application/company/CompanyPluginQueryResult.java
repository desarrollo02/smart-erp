package py.com.logixone.kernel.application.company;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.domain.company.CompanyPluginDiagnosticCode;
import py.com.logixone.kernel.domain.company.CompanyPluginResolution;

public record CompanyPluginQueryResult(
        CompanyId companyId,
        Optional<CompanyPluginResolution> resolution,
        Optional<CompanyPluginDiagnosticCode> failure) {

    public CompanyPluginQueryResult {
        Objects.requireNonNull(companyId, "companyId");
        resolution = Objects.requireNonNull(resolution, "resolution");
        failure = Objects.requireNonNull(failure, "failure");
        if (resolution.isPresent() == failure.isPresent()) {
            throw new IllegalArgumentException("exactly one of resolution or failure must be present");
        }
    }

    public static CompanyPluginQueryResult found(CompanyPluginResolution resolution) {
        Objects.requireNonNull(resolution, "resolution");
        return new CompanyPluginQueryResult(
                resolution.companyId(), Optional.of(resolution), Optional.empty());
    }

    public static CompanyPluginQueryResult notFound(CompanyId companyId) {
        return new CompanyPluginQueryResult(
                companyId,
                Optional.empty(),
                Optional.of(CompanyPluginDiagnosticCode.COMPANY_NOT_FOUND));
    }

    public boolean isFound() {
        return resolution.isPresent();
    }
}
