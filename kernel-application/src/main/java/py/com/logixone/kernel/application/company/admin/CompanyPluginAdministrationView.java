package py.com.logixone.kernel.application.company.admin;

import java.util.List;
import java.util.Objects;
import py.com.logixone.kernel.domain.company.CompanyPluginDiagnosticCode;

/** Complete read model needed by the per-company plugin administration screen. */
public record CompanyPluginAdministrationView(
        CompanySummaryView company,
        boolean operational,
        List<CompanyPluginActivationView> functionalPlugins,
        List<CompanyPluginDiagnosticCode> diagnostics) {

    public CompanyPluginAdministrationView {
        Objects.requireNonNull(company, "company");
        functionalPlugins = List.copyOf(Objects.requireNonNull(functionalPlugins, "functionalPlugins"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }
}
