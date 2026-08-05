package py.com.logixone.kernel.application.company.screen;

import java.util.List;
import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;

/** Atomic and immutable screen result for exactly one company. */
public record CompanyScreenComposition(
        CompanyId companyId,
        boolean operational,
        List<ComposedScreen> screens,
        List<ScreenCompositionDiagnostic> diagnostics) {

    public CompanyScreenComposition {
        Objects.requireNonNull(companyId, "companyId");
        screens = List.copyOf(screens);
        diagnostics = List.copyOf(diagnostics);
        if (operational && !diagnostics.isEmpty()) {
            throw new IllegalArgumentException("an operational screen composition cannot have diagnostics");
        }
        if (!operational && !screens.isEmpty()) {
            throw new IllegalArgumentException("a rejected screen composition cannot expose partial screens");
        }
    }
}
