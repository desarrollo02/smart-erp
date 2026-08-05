package py.com.logixone.kernel.application.company;

import java.time.Clock;
import java.util.Objects;
import java.util.function.Supplier;
import py.com.logixone.kernel.api.company.CompanyContext;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.application.company.audit.CompanyAuditActor;
import py.com.logixone.kernel.application.company.audit.CompanyAuditOperation;
import py.com.logixone.kernel.application.company.audit.CompanyAuditOutcome;
import py.com.logixone.kernel.application.company.port.CompanyAuditPort;
import py.com.logixone.kernel.domain.company.CompanyPluginResolution;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;

/** Server-side guard that validates the trusted company context before invoking a callback. */
public final class PluginOperationGuard {

    private final CompanyContext companyContext;
    private final CompanyPluginQueryService queryService;
    private final CompanyAuditRecorder audit;

    public PluginOperationGuard(
            CompanyContext companyContext,
            CompanyPluginQueryService queryService,
            CompanyAuditPort auditPort,
            Clock clock,
            CompanyAuditActor actor) {
        this.companyContext = Objects.requireNonNull(companyContext, "companyContext");
        this.queryService = Objects.requireNonNull(queryService, "queryService");
        this.audit = new CompanyAuditRecorder(auditPort, clock, actor);
    }

    public <T> T execute(PluginId requiredPluginId, Supplier<T> operation) {
        Objects.requireNonNull(requiredPluginId, "requiredPluginId");
        Objects.requireNonNull(operation, "operation");
        CompanyId companyId = Objects.requireNonNull(
                companyContext.requiredCompanyId(), "requiredCompanyId");
        CompanyPluginQueryResult query = queryService.resolve(companyId);
        CompanyOperationCode denial = denialCode(query, requiredPluginId);
        if (denial != null) {
            audit.record(
                    companyId,
                    CompanyAuditOperation.VERIFY_PLUGIN_ACCESS,
                    CompanyAuditOutcome.DENIED,
                    requiredPluginId,
                    denial,
                    null,
                    null);
            throw new PluginOperationDeniedException();
        }
        audit.record(
                companyId,
                CompanyAuditOperation.VERIFY_PLUGIN_ACCESS,
                CompanyAuditOutcome.ALLOWED,
                requiredPluginId,
                null,
                null,
                null);
        return operation.get();
    }

    private static CompanyOperationCode denialCode(
            CompanyPluginQueryResult query,
            PluginId requiredPluginId) {
        if (!query.isFound()) {
            return query.failure()
                    .map(code -> CompanyOperationCode.valueOf(code.name()))
                    .orElse(CompanyOperationCode.COMPANY_NOT_FOUND);
        }
        CompanyPluginResolution resolution = query.resolution().orElseThrow();
        if (!resolution.operational()) {
            return resolution.diagnostics().isEmpty()
                    ? CompanyOperationCode.COMPANY_INACTIVE
                    : CompanyOperationCode.valueOf(resolution.diagnostics().getFirst().code().name());
        }
        boolean effectiveFunctional = resolution.orderedPlugins().stream()
                .anyMatch(descriptor -> descriptor.id().equals(requiredPluginId)
                        && descriptor.kind() == PluginKind.FUNCTIONAL);
        return effectiveFunctional ? null : CompanyOperationCode.PLUGIN_DISABLED;
    }
}
