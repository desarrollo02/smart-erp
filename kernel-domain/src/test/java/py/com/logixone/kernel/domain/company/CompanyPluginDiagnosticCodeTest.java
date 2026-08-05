package py.com.logixone.kernel.domain.company;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class CompanyPluginDiagnosticCodeTest {

    @Test
    void exposesTheStableMinimumDefinedByTheAcceptedArchitectureDecision() {
        assertEquals(
                EnumSet.of(
                        CompanyPluginDiagnosticCode.COMPANY_NOT_FOUND,
                        CompanyPluginDiagnosticCode.COMPANY_INACTIVE,
                        CompanyPluginDiagnosticCode.COMPANY_VERSION_CONFLICT,
                        CompanyPluginDiagnosticCode.PLUGIN_NOT_PRESENT,
                        CompanyPluginDiagnosticCode.PLUGIN_NOT_FUNCTIONAL,
                        CompanyPluginDiagnosticCode.PLUGIN_DISABLED,
                        CompanyPluginDiagnosticCode.REQUIRED_DEPENDENCY_NOT_EFFECTIVE,
                        CompanyPluginDiagnosticCode.ACTIVE_DEPENDENT_EXISTS,
                        CompanyPluginDiagnosticCode.CUSTOMIZATION_REQUIRED,
                        CompanyPluginDiagnosticCode.CUSTOMIZATION_NOT_PRESENT,
                        CompanyPluginDiagnosticCode.CUSTOMIZATION_WRONG_KIND,
                        CompanyPluginDiagnosticCode.CUSTOMIZATION_ALREADY_ASSIGNED,
                        CompanyPluginDiagnosticCode.CUSTOMIZATION_INCOMPATIBLE,
                        CompanyPluginDiagnosticCode.CUSTOMIZATION_CONTRACT_INVALID,
                        CompanyPluginDiagnosticCode.CUSTOMIZATION_VERSION_CONFLICT),
                EnumSet.allOf(CompanyPluginDiagnosticCode.class));
    }
}
