package py.com.logixone.kernel.application.company;

import java.util.List;
import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.application.company.port.CompanyRepository;
import py.com.logixone.kernel.application.company.port.PluginActivationRepository;
import py.com.logixone.kernel.application.plugin.PluginRegistry;
import py.com.logixone.kernel.domain.company.Company;
import py.com.logixone.kernel.domain.company.CompanyPluginResolution;
import py.com.logixone.kernel.domain.company.CompanyPluginResolver;
import py.com.logixone.kernel.domain.company.PluginActivationDecision;

/** Neutral read use case; transaction and persistence are supplied by future adapters. */
public final class CompanyPluginQueryService {

    private final CompanyRepository companyRepository;
    private final PluginActivationRepository activationRepository;
    private final PluginRegistry pluginRegistry;
    private final CompanyPluginResolver resolver;

    public CompanyPluginQueryService(
            CompanyRepository companyRepository,
            PluginActivationRepository activationRepository,
            PluginRegistry pluginRegistry,
            CompanyPluginResolver resolver) {
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository");
        this.activationRepository = Objects.requireNonNull(activationRepository, "activationRepository");
        this.pluginRegistry = Objects.requireNonNull(pluginRegistry, "pluginRegistry");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    public CompanyPluginQueryResult resolve(CompanyId companyId) {
        Objects.requireNonNull(companyId, "companyId");
        Company company = companyRepository.findById(companyId).orElse(null);
        if (company == null) {
            return CompanyPluginQueryResult.notFound(companyId);
        }
        boolean assignedElsewhere = companyRepository.isCustomizationAssignedToAnotherCompany(
                company.customizationPluginId(), company.id());
        List<PluginActivationDecision> decisions = List.copyOf(
                activationRepository.findByCompanyId(company.id()));
        CompanyPluginResolution resolution = resolver.resolve(
                company,
                assignedElsewhere,
                decisions,
                pluginRegistry.orderedPlugins());
        return CompanyPluginQueryResult.found(resolution);
    }
}
