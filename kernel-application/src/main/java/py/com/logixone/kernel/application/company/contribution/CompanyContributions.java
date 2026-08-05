package py.com.logixone.kernel.application.company.contribution;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.domain.company.CompanyPluginDiagnostic;
import py.com.logixone.kernel.domain.company.CompanyPluginDiagnosticCode;
import py.com.logixone.kernel.domain.company.CompanyPluginResolution;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.MenuContribution;
import py.com.logixone.plugin.api.PluginKind;

/** Immutable and deterministic contribution view for exactly one company. */
public final class CompanyContributions {

    private final CompanyId companyId;
    private final boolean operational;
    private final List<PluginContributions> plugins;
    private final List<ContributionId> capabilities;
    private final List<ContributionId> permissions;
    private final List<MenuContribution> menuContributions;
    private final List<CompanyPluginDiagnostic> diagnostics;
    private final Optional<CompanyPluginDiagnosticCode> failure;

    private CompanyContributions(
            CompanyId companyId,
            boolean operational,
            List<PluginContributions> plugins,
            List<CompanyPluginDiagnostic> diagnostics,
            Optional<CompanyPluginDiagnosticCode> failure) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.operational = operational;
        this.plugins = List.copyOf(plugins);
        this.diagnostics = List.copyOf(diagnostics);
        this.failure = Objects.requireNonNull(failure, "failure");
        validateState();
        this.capabilities = this.plugins.stream()
                .flatMap(plugin -> plugin.capabilities().stream())
                .toList();
        this.permissions = this.plugins.stream()
                .flatMap(plugin -> plugin.permissions().stream())
                .toList();
        this.menuContributions = this.plugins.stream()
                .flatMap(plugin -> plugin.menuContributions().stream())
                .toList();
    }

    static CompanyContributions fromResolution(CompanyPluginResolution resolution) {
        Objects.requireNonNull(resolution, "resolution");
        List<PluginContributions> plugins = resolution.operational()
                ? resolution.orderedPlugins().stream()
                        .map(PluginContributions::fromDescriptor)
                        .toList()
                : List.of();
        return new CompanyContributions(
                resolution.companyId(),
                resolution.operational(),
                plugins,
                resolution.diagnostics(),
                Optional.empty());
    }

    static CompanyContributions notFound(
            CompanyId companyId,
            CompanyPluginDiagnosticCode failure) {
        return new CompanyContributions(
                companyId,
                false,
                List.of(),
                List.of(),
                Optional.of(Objects.requireNonNull(failure, "failure")));
    }

    private void validateState() {
        if (!operational && !plugins.isEmpty()) {
            throw new IllegalArgumentException("a non-operational company cannot expose contributions");
        }
        if (operational && failure.isPresent()) {
            throw new IllegalArgumentException("an operational company cannot have a query failure");
        }
        if (operational) {
            if (plugins.isEmpty()
                    || plugins.getLast().pluginKind() != PluginKind.CUSTOMIZATION
                    || plugins.subList(0, plugins.size() - 1).stream()
                            .anyMatch(plugin -> plugin.pluginKind() != PluginKind.FUNCTIONAL)) {
                throw new IllegalArgumentException(
                        "operational contributions require functional plugins followed by one customization");
            }
        }
    }

    public CompanyId companyId() {
        return companyId;
    }

    public boolean operational() {
        return operational;
    }

    public List<PluginContributions> plugins() {
        return plugins;
    }

    public List<ContributionId> capabilities() {
        return capabilities;
    }

    public List<ContributionId> permissions() {
        return permissions;
    }

    public List<MenuContribution> menuContributions() {
        return menuContributions;
    }

    public List<CompanyPluginDiagnostic> diagnostics() {
        return diagnostics;
    }

    public Optional<CompanyPluginDiagnosticCode> failure() {
        return failure;
    }
}
