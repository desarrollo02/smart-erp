package py.com.logixone.kernel.application.company;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.application.company.port.CompanyRepository;
import py.com.logixone.kernel.application.company.port.PluginActivationRepository;
import py.com.logixone.kernel.application.plugin.PluginRegistry;
import py.com.logixone.kernel.domain.company.Company;
import py.com.logixone.kernel.domain.company.CompanyPluginDiagnosticCode;
import py.com.logixone.kernel.domain.company.CompanyPluginResolver;
import py.com.logixone.kernel.domain.company.CompanyStatus;
import py.com.logixone.kernel.domain.company.PluginActivationDecision;
import py.com.logixone.kernel.domain.company.PluginActivationState;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;

class CompanyPluginQueryServiceTest {

    private static final CompanyId COMPANY_ID = new CompanyId(new UUID(0, 1));
    private static final PluginId CUSTOMIZATION_ID = new PluginId("custom_a");

    @Test
    void returnsAnExplicitNotFoundResultWithoutQueryingActivations() {
        RecordingActivationRepository activations = new RecordingActivationRepository(List.of());
        CompanyPluginQueryService service = new CompanyPluginQueryService(
                new FixedCompanyRepository(Optional.empty(), false),
                activations,
                PluginRegistry.create(List.of()),
                new CompanyPluginResolver());

        CompanyPluginQueryResult result = service.resolve(COMPANY_ID);

        assertFalse(result.isFound());
        assertEquals(
                CompanyPluginDiagnosticCode.COMPANY_NOT_FOUND,
                result.failure().orElseThrow());
        assertEquals(0, activations.calls);
    }

    @Test
    void resolvesOnlyTheRequestedCompanyThroughNeutralPorts() {
        Company company = new Company(
                COMPANY_ID, CompanyStatus.ACTIVE, CUSTOMIZATION_ID, 0);
        PluginDescriptor functional = descriptor("sales", PluginKind.FUNCTIONAL);
        PluginDescriptor customization = descriptor("custom_a", PluginKind.CUSTOMIZATION);
        RecordingActivationRepository activations = new RecordingActivationRepository(List.of(
                new PluginActivationDecision(
                        COMPANY_ID,
                        functional.id(),
                        PluginActivationState.ENABLED,
                        0)));
        CompanyPluginQueryService service = new CompanyPluginQueryService(
                new FixedCompanyRepository(Optional.of(company), false),
                activations,
                PluginRegistry.create(List.of(definition(customization), definition(functional))),
                new CompanyPluginResolver());

        CompanyPluginQueryResult result = service.resolve(COMPANY_ID);

        assertTrue(result.isFound());
        assertTrue(result.failure().isEmpty());
        assertEquals(
                List.of("sales", "custom_a"),
                result.resolution().orElseThrow().orderedPlugins().stream()
                        .map(plugin -> plugin.id().value())
                        .toList());
        assertEquals(COMPANY_ID, activations.lastCompanyId);
        assertEquals(1, activations.calls);
    }

    private static PluginDescriptor descriptor(String id, PluginKind kind) {
        return new PluginDescriptor(
                new PluginId(id),
                kind,
                SemanticVersion.parse("1.0.0"),
                new VersionRange(
                        SemanticVersion.parse("0.4.0"),
                        SemanticVersion.parse("0.5.0")),
                id,
                List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private static PluginDefinition definition(PluginDescriptor descriptor) {
        return () -> descriptor;
    }

    private record FixedCompanyRepository(
            Optional<Company> company,
            boolean assignedElsewhere) implements CompanyRepository {

        @Override
        public List<Company> findAll() {
            return company.stream().toList();
        }

        @Override
        public Optional<Company> findById(CompanyId companyId) {
            return company.filter(candidate -> candidate.id().equals(companyId));
        }

        @Override
        public Company save(Company company) {
            throw new UnsupportedOperationException("not used by query test");
        }

        @Override
        public boolean isCustomizationAssignedToAnotherCompany(
                PluginId customizationPluginId,
                CompanyId companyId) {
            return assignedElsewhere;
        }
    }

    private static final class RecordingActivationRepository implements PluginActivationRepository {
        private final List<PluginActivationDecision> decisions;
        private int calls;
        private CompanyId lastCompanyId;

        private RecordingActivationRepository(List<PluginActivationDecision> decisions) {
            this.decisions = List.copyOf(decisions);
        }

        @Override
        public List<PluginActivationDecision> findByCompanyId(CompanyId companyId) {
            calls++;
            lastCompanyId = companyId;
            return decisions;
        }

        @Override
        public Optional<PluginActivationDecision> findByCompanyAndPlugin(
                CompanyId companyId,
                PluginId pluginId) {
            return decisions.stream()
                    .filter(decision -> decision.companyId().equals(companyId)
                            && decision.pluginId().equals(pluginId))
                    .findFirst();
        }

        @Override
        public PluginActivationDecision save(PluginActivationDecision decision) {
            throw new UnsupportedOperationException("not used by query test");
        }
    }
}
