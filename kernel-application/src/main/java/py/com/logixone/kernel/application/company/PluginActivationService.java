package py.com.logixone.kernel.application.company;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.application.company.audit.CompanyAuditActor;
import py.com.logixone.kernel.application.company.audit.CompanyAuditContext;
import py.com.logixone.kernel.application.company.audit.CompanyAuditOperation;
import py.com.logixone.kernel.application.company.audit.CompanyAuditOutcome;
import py.com.logixone.kernel.application.company.command.ChangePluginActivationCommand;
import py.com.logixone.kernel.application.company.port.CompanyAuditPort;
import py.com.logixone.kernel.application.company.port.CompanyRepository;
import py.com.logixone.kernel.application.company.port.PersistenceConflictCode;
import py.com.logixone.kernel.application.company.port.PersistenceConflictException;
import py.com.logixone.kernel.application.company.port.PluginActivationRepository;
import py.com.logixone.kernel.application.plugin.PluginRegistry;
import py.com.logixone.kernel.domain.company.Company;
import py.com.logixone.kernel.domain.company.PluginActivationChangeResult;
import py.com.logixone.kernel.domain.company.PluginActivationDecision;
import py.com.logixone.kernel.domain.company.PluginActivationPolicy;
import py.com.logixone.kernel.domain.company.PluginActivationState;

/** Neutral desired-activation use case; physical activation is resolved per company. */
public final class PluginActivationService {

    private final CompanyRepository companyRepository;
    private final PluginActivationRepository activationRepository;
    private final PluginRegistry pluginRegistry;
    private final PluginActivationPolicy policy;
    private final CompanyAuditRecorder audit;

    public PluginActivationService(
            CompanyRepository companyRepository,
            PluginActivationRepository activationRepository,
            PluginRegistry pluginRegistry,
            PluginActivationPolicy policy,
            CompanyAuditPort auditPort,
            Clock clock,
            CompanyAuditActor actor) {
        this(
                companyRepository,
                activationRepository,
                pluginRegistry,
                policy,
                auditPort,
                clock,
                CompanyAuditContext.legacy(actor));
    }

    public PluginActivationService(
            CompanyRepository companyRepository,
            PluginActivationRepository activationRepository,
            PluginRegistry pluginRegistry,
            PluginActivationPolicy policy,
            CompanyAuditPort auditPort,
            Clock clock,
            CompanyAuditContext auditContext) {
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository");
        this.activationRepository = Objects.requireNonNull(activationRepository, "activationRepository");
        this.pluginRegistry = Objects.requireNonNull(pluginRegistry, "pluginRegistry");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.audit = new CompanyAuditRecorder(auditPort, clock, auditContext);
    }

    public CompanyOperationResult<PluginActivationDecision> change(
            ChangePluginActivationCommand command) {
        Objects.requireNonNull(command, "command");
        Company company = companyRepository.findById(command.companyId()).orElse(null);
        if (company == null) {
            return rejected(command, CompanyOperationCode.COMPANY_NOT_FOUND, null);
        }
        if (company.customizationPluginId().equals(command.pluginId())) {
            return rejected(command, CompanyOperationCode.CUSTOMIZATION_REQUIRED, null);
        }

        Optional<PluginActivationDecision> persisted = activationRepository.findByCompanyAndPlugin(
                company.id(), command.pluginId());
        PluginActivationDecision current = persisted.orElseGet(() -> new PluginActivationDecision(
                company.id(), command.pluginId(), PluginActivationState.DISABLED, 0));
        if (current.desiredState() == command.desiredState()) {
            audit.record(
                    company.id(),
                    CompanyAuditOperation.CHANGE_PLUGIN_ACTIVATION,
                    CompanyAuditOutcome.UNCHANGED,
                    command.pluginId(),
                    null,
                    current.version(),
                    current.version());
            return CompanyOperationResult.unchanged(current);
        }
        if (current.version() != command.expectedVersion()) {
            return rejected(
                    command,
                    CompanyOperationCode.ACTIVATION_VERSION_CONFLICT,
                    current.version());
        }

        List<PluginActivationDecision> decisions = activationRepository.findByCompanyId(company.id());
        PluginActivationChangeResult evaluation = policy.evaluate(
                company,
                command.pluginId(),
                command.desiredState(),
                decisions,
                pluginRegistry.orderedPlugins());
        if (!evaluation.allowed()) {
            return rejected(
                    command,
                    CompanyOperationCode.valueOf(evaluation.diagnostics().getFirst().code().name()),
                    current.version());
        }

        PluginActivationDecision desired = new PluginActivationDecision(
                company.id(), command.pluginId(), command.desiredState(), current.version());
        try {
            PluginActivationDecision stored = activationRepository.save(desired);
            audit.record(
                    company.id(),
                    CompanyAuditOperation.CHANGE_PLUGIN_ACTIVATION,
                    CompanyAuditOutcome.CHANGED,
                    command.pluginId(),
                    null,
                    current.version(),
                    stored.version());
            return CompanyOperationResult.changed(stored);
        } catch (PersistenceConflictException failure) {
            return rejected(command, mapConflict(failure.code()), current.version());
        }
    }

    private CompanyOperationResult<PluginActivationDecision> rejected(
            ChangePluginActivationCommand command,
            CompanyOperationCode code,
            Long previousVersion) {
        audit.record(
                command.companyId(),
                CompanyAuditOperation.CHANGE_PLUGIN_ACTIVATION,
                CompanyAuditOutcome.REJECTED,
                command.pluginId(),
                code,
                previousVersion,
                null);
        return CompanyOperationResult.rejected(code);
    }

    private static CompanyOperationCode mapConflict(PersistenceConflictCode code) {
        return switch (code) {
            case COMPANY_NOT_FOUND -> CompanyOperationCode.COMPANY_NOT_FOUND;
            case ACTIVATION_ALREADY_EXISTS, ACTIVATION_NOT_FOUND, ACTIVATION_VERSION_CONFLICT ->
                    CompanyOperationCode.ACTIVATION_VERSION_CONFLICT;
            default -> CompanyOperationCode.PERSISTENCE_CONFLICT;
        };
    }
}
