package py.com.logixone.kernel.application.company.admin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.application.company.port.CompanyRepository;
import py.com.logixone.kernel.application.company.port.PluginActivationRepository;
import py.com.logixone.kernel.application.plugin.PluginRegistry;
import py.com.logixone.kernel.domain.company.Company;
import py.com.logixone.kernel.domain.company.CompanyPluginResolution;
import py.com.logixone.kernel.domain.company.CompanyPluginResolver;
import py.com.logixone.kernel.domain.company.PluginActivationDecision;
import py.com.logixone.kernel.domain.company.PluginActivationState;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;

/** Neutral read service for the global company and plugin administration UI. */
public final class CompanyAdministrationQueryService {

    private final CompanyRepository companyRepository;
    private final PluginActivationRepository activationRepository;
    private final PluginRegistry pluginRegistry;
    private final CompanyPluginResolver resolver;

    public CompanyAdministrationQueryService(
            CompanyRepository companyRepository,
            PluginActivationRepository activationRepository,
            PluginRegistry pluginRegistry,
            CompanyPluginResolver resolver) {
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository");
        this.activationRepository = Objects.requireNonNull(activationRepository, "activationRepository");
        this.pluginRegistry = Objects.requireNonNull(pluginRegistry, "pluginRegistry");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    public CompanyAdministrationSnapshot snapshot() {
        List<CompanySummaryView> companies = companyRepository.findAll().stream()
                .sorted((left, right) -> left.id().compareTo(right.id()))
                .map(CompanySummaryView::from)
                .toList();
        List<PluginCatalogView> plugins = pluginRegistry.orderedPlugins().stream()
                .map(PluginCatalogView::from)
                .toList();
        return new CompanyAdministrationSnapshot(companies, plugins);
    }

    public Optional<CompanyPluginAdministrationView> findCompany(CompanyId companyId) {
        Objects.requireNonNull(companyId, "companyId");
        Company company = companyRepository.findById(companyId).orElse(null);
        if (company == null) {
            return Optional.empty();
        }

        List<PluginActivationDecision> decisions = List.copyOf(
                activationRepository.findByCompanyId(company.id()));
        CompanyPluginResolution resolution = resolver.resolve(
                company,
                companyRepository.isCustomizationAssignedToAnotherCompany(
                        company.customizationPluginId(), company.id()),
                decisions,
                pluginRegistry.orderedPlugins());

        Map<PluginId, PluginActivationDecision> decisionsByPlugin = new HashMap<>();
        decisions.forEach(decision -> decisionsByPlugin.put(decision.pluginId(), decision));
        Set<PluginId> effectiveIds = new HashSet<>();
        if (resolution.operational()) {
            resolution.orderedPlugins().stream()
                    .map(PluginDescriptor::id)
                    .forEach(effectiveIds::add);
        }

        List<CompanyPluginActivationView> functionalPlugins = pluginRegistry.orderedPlugins().stream()
                .filter(descriptor -> descriptor.kind() == PluginKind.FUNCTIONAL)
                .map(descriptor -> activationView(descriptor, decisionsByPlugin, effectiveIds))
                .toList();
        return Optional.of(new CompanyPluginAdministrationView(
                CompanySummaryView.from(company),
                resolution.operational(),
                functionalPlugins,
                resolution.diagnostics().stream().map(diagnostic -> diagnostic.code()).distinct().toList()));
    }

    private static CompanyPluginActivationView activationView(
            PluginDescriptor descriptor,
            Map<PluginId, PluginActivationDecision> decisions,
            Set<PluginId> effectiveIds) {
        PluginActivationDecision decision = decisions.get(descriptor.id());
        PluginActivationState state = decision == null
                ? PluginActivationState.DISABLED
                : decision.desiredState();
        long version = decision == null ? 0 : decision.version();
        return new CompanyPluginActivationView(
                descriptor.id(),
                descriptor.displayName(),
                descriptor.version().toString(),
                state,
                version,
                effectiveIds.contains(descriptor.id()));
    }
}
