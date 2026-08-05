package py.com.logixone.kernel.domain.company;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import py.com.logixone.kernel.domain.plugin.PluginCatalogResolution;
import py.com.logixone.kernel.domain.plugin.PluginCatalogResolver;
import py.com.logixone.plugin.api.DependencyKind;
import py.com.logixone.plugin.api.PluginDependency;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;

/** Pure policy for changing desired activation without performing persistence. */
public final class PluginActivationPolicy {

    private final PluginCatalogResolver catalogResolver = new PluginCatalogResolver();

    public PluginActivationChangeResult evaluate(
            Company company,
            PluginId pluginId,
            PluginActivationState desiredState,
            Collection<PluginActivationDecision> activationDecisions,
            Collection<PluginDescriptor> physicalCatalog) {
        Objects.requireNonNull(company, "company");
        Objects.requireNonNull(pluginId, "pluginId");
        Objects.requireNonNull(desiredState, "desiredState");
        Objects.requireNonNull(activationDecisions, "activationDecisions");
        Objects.requireNonNull(physicalCatalog, "physicalCatalog");

        PluginCatalogResolution catalog = catalogResolver.resolve(physicalCatalog);
        if (!catalog.isValid()) {
            throw new IllegalArgumentException("physicalCatalog must be globally valid");
        }
        Map<PluginId, PluginDescriptor> descriptors = new HashMap<>();
        catalog.orderedPlugins().forEach(descriptor -> descriptors.put(descriptor.id(), descriptor));
        List<CompanyPluginDiagnostic> diagnostics = new ArrayList<>();
        PluginDescriptor target = descriptors.get(pluginId);
        if (target == null) {
            diagnostics.add(diagnostic(
                    CompanyPluginDiagnosticCode.PLUGIN_NOT_PRESENT, pluginId, pluginId.value()));
            return result(pluginId, desiredState, diagnostics);
        }
        if (target.kind() != PluginKind.FUNCTIONAL) {
            diagnostics.add(diagnostic(
                    CompanyPluginDiagnosticCode.PLUGIN_NOT_FUNCTIONAL,
                    pluginId,
                    target.kind().name()));
            return result(pluginId, desiredState, diagnostics);
        }

        Map<PluginId, PluginActivationDecision> decisions = decisionsFor(company, activationDecisions);
        if (desiredState == PluginActivationState.ENABLED) {
            target.dependencies().stream()
                    .filter(dependency -> dependency.kind() == DependencyKind.REQUIRED)
                    .map(PluginDependency::pluginId)
                    .filter(dependencyId -> !isDesiredEnabled(decisions, dependencyId))
                    .sorted()
                    .forEach(dependencyId -> diagnostics.add(diagnostic(
                            CompanyPluginDiagnosticCode.REQUIRED_DEPENDENCY_NOT_EFFECTIVE,
                            pluginId,
                            dependencyId.value())));
        } else {
            catalog.orderedPlugins().stream()
                    .filter(descriptor -> !descriptor.id().equals(pluginId))
                    .filter(descriptor -> isDesiredConsumer(company, descriptor, decisions))
                    .filter(descriptor -> hasRequiredDependency(descriptor, pluginId))
                    .forEach(descriptor -> diagnostics.add(diagnostic(
                            CompanyPluginDiagnosticCode.ACTIVE_DEPENDENT_EXISTS,
                            pluginId,
                            descriptor.id().value())));
        }
        return result(pluginId, desiredState, diagnostics);
    }

    private static boolean isDesiredConsumer(
            Company company,
            PluginDescriptor descriptor,
            Map<PluginId, PluginActivationDecision> decisions) {
        if (descriptor.kind() == PluginKind.CUSTOMIZATION) {
            return descriptor.id().equals(company.customizationPluginId());
        }
        return isDesiredEnabled(decisions, descriptor.id());
    }

    private static boolean hasRequiredDependency(PluginDescriptor descriptor, PluginId pluginId) {
        return descriptor.dependencies().stream()
                .anyMatch(dependency -> dependency.kind() == DependencyKind.REQUIRED
                        && dependency.pluginId().equals(pluginId));
    }

    private static boolean isDesiredEnabled(
            Map<PluginId, PluginActivationDecision> decisions,
            PluginId pluginId) {
        PluginActivationDecision decision = decisions.get(pluginId);
        return decision != null && decision.isEnabled();
    }

    private static Map<PluginId, PluginActivationDecision> decisionsFor(
            Company company,
            Collection<PluginActivationDecision> activationDecisions) {
        Map<PluginId, PluginActivationDecision> result = new HashMap<>();
        activationDecisions.stream()
                .map(decision -> Objects.requireNonNull(decision, "activationDecision"))
                .filter(decision -> decision.companyId().equals(company.id()))
                .forEach(decision -> {
                    if (result.putIfAbsent(decision.pluginId(), decision) != null) {
                        throw new IllegalArgumentException(
                                "duplicate activation decision for " + decision.pluginId());
                    }
                });
        return result;
    }

    private static PluginActivationChangeResult result(
            PluginId pluginId,
            PluginActivationState desiredState,
            List<CompanyPluginDiagnostic> diagnostics) {
        diagnostics.sort(null);
        return new PluginActivationChangeResult(
                pluginId, desiredState, diagnostics.isEmpty(), diagnostics);
    }

    private static CompanyPluginDiagnostic diagnostic(
            CompanyPluginDiagnosticCode code,
            PluginId pluginId,
            String subject) {
        return new CompanyPluginDiagnostic(code, pluginId, subject);
    }
}
