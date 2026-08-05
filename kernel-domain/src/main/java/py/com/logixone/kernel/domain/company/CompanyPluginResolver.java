package py.com.logixone.kernel.domain.company;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.domain.plugin.PluginCatalogResolution;
import py.com.logixone.kernel.domain.plugin.PluginCatalogResolver;
import py.com.logixone.plugin.api.DependencyKind;
import py.com.logixone.plugin.api.PluginDependency;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;

/** Resolves the effective per-company view over an already physical plugin catalog. */
public final class CompanyPluginResolver {

    private final PluginCatalogResolver catalogResolver;

    public CompanyPluginResolver() {
        this(new PluginCatalogResolver());
    }

    CompanyPluginResolver(PluginCatalogResolver catalogResolver) {
        this.catalogResolver = Objects.requireNonNull(catalogResolver, "catalogResolver");
    }

    public CompanyPluginResolution resolve(
            Company company,
            boolean customizationAssignedToAnotherCompany,
            Collection<PluginActivationDecision> activationDecisions,
            Collection<PluginDescriptor> physicalCatalog) {
        Objects.requireNonNull(company, "company");
        Objects.requireNonNull(activationDecisions, "activationDecisions");
        Objects.requireNonNull(physicalCatalog, "physicalCatalog");

        PluginCatalogResolution catalog = catalogResolver.resolve(physicalCatalog);
        if (!catalog.isValid()) {
            throw new IllegalArgumentException("physicalCatalog must be globally valid");
        }
        List<CompanyPluginDiagnostic> diagnostics = new ArrayList<>();
        if (!company.isActive()) {
            diagnostics.add(diagnostic(
                    CompanyPluginDiagnosticCode.COMPANY_INACTIVE,
                    company.customizationPluginId(),
                    company.id().toString()));
            return unavailable(company, diagnostics);
        }

        validateExclusiveCustomization(company, customizationAssignedToAnotherCompany, diagnostics);
        Map<PluginId, PluginDescriptor> descriptorsById = new HashMap<>();
        catalog.orderedPlugins().forEach(descriptor -> descriptorsById.put(descriptor.id(), descriptor));
        PluginDescriptor customization = descriptorsById.get(company.customizationPluginId());
        if (customization == null) {
            diagnostics.add(diagnostic(
                    CompanyPluginDiagnosticCode.CUSTOMIZATION_NOT_PRESENT,
                    company.customizationPluginId(),
                    company.customizationPluginId().value()));
            return unavailable(company, diagnostics);
        }
        if (customization.kind() != PluginKind.CUSTOMIZATION) {
            diagnostics.add(diagnostic(
                    CompanyPluginDiagnosticCode.CUSTOMIZATION_WRONG_KIND,
                    customization.id(),
                    customization.kind().name()));
            return unavailable(company, diagnostics);
        }
        if (!diagnostics.isEmpty()) {
            return unavailable(company, diagnostics);
        }

        Map<PluginId, PluginActivationDecision> decisionsByPlugin = decisionsFor(company.id(), activationDecisions);
        Set<PluginId> effectiveIds = new HashSet<>();
        List<PluginDescriptor> effectivePlugins = new ArrayList<>();
        for (PluginDescriptor descriptor : catalog.orderedPlugins()) {
            if (descriptor.kind() != PluginKind.FUNCTIONAL) {
                continue;
            }
            PluginActivationDecision decision = decisionsByPlugin.get(descriptor.id());
            if (decision == null || !decision.isEnabled()) {
                continue;
            }
            List<PluginId> missingDependencies = missingRequiredDependencies(descriptor, effectiveIds);
            if (!missingDependencies.isEmpty()) {
                for (PluginId missingDependency : missingDependencies) {
                    diagnostics.add(diagnostic(
                            CompanyPluginDiagnosticCode.REQUIRED_DEPENDENCY_NOT_EFFECTIVE,
                            descriptor.id(),
                            missingDependency.value()));
                }
                continue;
            }
            effectiveIds.add(descriptor.id());
            effectivePlugins.add(descriptor);
        }
        diagnoseDecisionsWithoutFunctionalPlugin(decisionsByPlugin, descriptorsById, diagnostics);

        List<PluginId> missingCustomizationDependencies =
                missingRequiredDependencies(customization, effectiveIds);
        if (!missingCustomizationDependencies.isEmpty()) {
            for (PluginId missingDependency : missingCustomizationDependencies) {
                diagnostics.add(diagnostic(
                        CompanyPluginDiagnosticCode.CUSTOMIZATION_INCOMPATIBLE,
                        customization.id(),
                        missingDependency.value()));
            }
            return unavailable(company, diagnostics);
        }

        effectivePlugins.add(customization);
        diagnostics.sort(null);
        return new CompanyPluginResolution(
                company.id(),
                company.customizationPluginId(),
                true,
                effectivePlugins,
                diagnostics);
    }

    private static void validateExclusiveCustomization(
            Company company,
            boolean customizationAssignedToAnotherCompany,
            List<CompanyPluginDiagnostic> diagnostics) {
        if (customizationAssignedToAnotherCompany) {
            diagnostics.add(diagnostic(
                    CompanyPluginDiagnosticCode.CUSTOMIZATION_ALREADY_ASSIGNED,
                    company.customizationPluginId(),
                    company.customizationPluginId().value()));
        }
    }

    private static Map<PluginId, PluginActivationDecision> decisionsFor(
            CompanyId companyId,
            Collection<PluginActivationDecision> activationDecisions) {
        Map<PluginId, PluginActivationDecision> decisionsByPlugin = new HashMap<>();
        activationDecisions.stream()
                .map(decision -> Objects.requireNonNull(decision, "activationDecision"))
                .filter(decision -> decision.companyId().equals(companyId))
                .forEach(decision -> {
                    if (decisionsByPlugin.putIfAbsent(decision.pluginId(), decision) != null) {
                        throw new IllegalArgumentException(
                                "duplicate activation decision for " + decision.pluginId());
                    }
                });
        return decisionsByPlugin;
    }

    private static void diagnoseDecisionsWithoutFunctionalPlugin(
            Map<PluginId, PluginActivationDecision> decisionsByPlugin,
            Map<PluginId, PluginDescriptor> descriptorsById,
            List<CompanyPluginDiagnostic> diagnostics) {
        decisionsByPlugin.values().stream()
                .filter(PluginActivationDecision::isEnabled)
                .sorted((first, second) -> first.pluginId().compareTo(second.pluginId()))
                .forEach(decision -> {
                    PluginDescriptor descriptor = descriptorsById.get(decision.pluginId());
                    if (descriptor == null) {
                        diagnostics.add(diagnostic(
                                CompanyPluginDiagnosticCode.PLUGIN_NOT_PRESENT,
                                decision.pluginId(),
                                decision.pluginId().value()));
                    } else if (descriptor.kind() != PluginKind.FUNCTIONAL) {
                        diagnostics.add(diagnostic(
                                CompanyPluginDiagnosticCode.PLUGIN_NOT_FUNCTIONAL,
                                decision.pluginId(),
                                descriptor.kind().name()));
                    }
                });
    }

    private static List<PluginId> missingRequiredDependencies(
            PluginDescriptor descriptor,
            Set<PluginId> effectiveIds) {
        return descriptor.dependencies().stream()
                .filter(dependency -> dependency.kind() == DependencyKind.REQUIRED)
                .map(PluginDependency::pluginId)
                .filter(dependencyId -> !effectiveIds.contains(dependencyId))
                .sorted()
                .toList();
    }

    private static CompanyPluginResolution unavailable(
            Company company,
            List<CompanyPluginDiagnostic> diagnostics) {
        diagnostics.sort(null);
        return new CompanyPluginResolution(
                company.id(),
                company.customizationPluginId(),
                false,
                List.of(),
                diagnostics);
    }

    private static CompanyPluginDiagnostic diagnostic(
            CompanyPluginDiagnosticCode code,
            PluginId pluginId,
            String subject) {
        return new CompanyPluginDiagnostic(code, pluginId, subject);
    }
}
