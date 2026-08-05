package py.com.logixone.kernel.application.company;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.application.company.audit.CompanyAuditActor;
import py.com.logixone.kernel.application.company.audit.CompanyAuditContext;
import py.com.logixone.kernel.application.company.audit.CompanyAuditOperation;
import py.com.logixone.kernel.application.company.audit.CompanyAuditOutcome;
import py.com.logixone.kernel.application.company.command.ChangeCompanyStatusCommand;
import py.com.logixone.kernel.application.company.command.RegisterCompanyCommand;
import py.com.logixone.kernel.application.company.command.ReplaceCustomizationCommand;
import py.com.logixone.kernel.application.company.port.CompanyAuditPort;
import py.com.logixone.kernel.application.company.port.CompanyIdGenerator;
import py.com.logixone.kernel.application.company.port.CompanyRepository;
import py.com.logixone.kernel.application.company.port.PersistenceConflictCode;
import py.com.logixone.kernel.application.company.port.PersistenceConflictException;
import py.com.logixone.kernel.application.company.port.PluginActivationRepository;
import py.com.logixone.kernel.application.plugin.PluginRegistry;
import py.com.logixone.kernel.domain.company.Company;
import py.com.logixone.kernel.domain.company.CompanyPluginResolution;
import py.com.logixone.kernel.domain.company.CompanyPluginResolver;
import py.com.logixone.kernel.domain.company.CompanyStatus;
import py.com.logixone.kernel.domain.company.PluginActivationDecision;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;

/** Neutral administrative use cases; a Jakarta adapter supplies the transaction boundary. */
public final class CompanyAdministrationService {

    private final CompanyRepository companyRepository;
    private final PluginActivationRepository activationRepository;
    private final CompanyIdGenerator idGenerator;
    private final PluginRegistry pluginRegistry;
    private final CompanyPluginResolver resolver;
    private final CompanyAuditRecorder audit;

    public CompanyAdministrationService(
            CompanyRepository companyRepository,
            PluginActivationRepository activationRepository,
            CompanyIdGenerator idGenerator,
            PluginRegistry pluginRegistry,
            CompanyPluginResolver resolver,
            CompanyAuditPort auditPort,
            Clock clock,
            CompanyAuditActor actor) {
        this(
                companyRepository,
                activationRepository,
                idGenerator,
                pluginRegistry,
                resolver,
                auditPort,
                clock,
                CompanyAuditContext.legacy(actor));
    }

    public CompanyAdministrationService(
            CompanyRepository companyRepository,
            PluginActivationRepository activationRepository,
            CompanyIdGenerator idGenerator,
            PluginRegistry pluginRegistry,
            CompanyPluginResolver resolver,
            CompanyAuditPort auditPort,
            Clock clock,
            CompanyAuditContext auditContext) {
        this.companyRepository = Objects.requireNonNull(companyRepository, "companyRepository");
        this.activationRepository = Objects.requireNonNull(activationRepository, "activationRepository");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.pluginRegistry = Objects.requireNonNull(pluginRegistry, "pluginRegistry");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.audit = new CompanyAuditRecorder(auditPort, clock, auditContext);
    }

    public CompanyOperationResult<Company> register(RegisterCompanyCommand command) {
        Objects.requireNonNull(command, "command");
        CompanyId companyId = idGenerator.nextId();
        PluginId customizationId = command.customizationPluginId();
        if (companyRepository.findById(companyId).isPresent()) {
            return rejected(
                    companyId,
                    CompanyAuditOperation.REGISTER_COMPANY,
                    customizationId,
                    CompanyOperationCode.COMPANY_ALREADY_EXISTS,
                    null,
                    null);
        }
        Optional<CompanyOperationCode> validation = validateCustomization(
                customizationId, companyId);
        if (validation.isPresent()) {
            return rejected(
                    companyId,
                    CompanyAuditOperation.REGISTER_COMPANY,
                    customizationId,
                    validation.orElseThrow(),
                    null,
                    null);
        }

        Company company = new Company(companyId, CompanyStatus.INACTIVE, customizationId, 0);
        try {
            Company stored = companyRepository.save(company);
            audit.record(
                    stored.id(),
                    CompanyAuditOperation.REGISTER_COMPANY,
                    CompanyAuditOutcome.CHANGED,
                    customizationId,
                    null,
                    null,
                    stored.version());
            return CompanyOperationResult.changed(stored);
        } catch (PersistenceConflictException failure) {
            return rejected(
                    companyId,
                    CompanyAuditOperation.REGISTER_COMPANY,
                    customizationId,
                    mapCompanyConflict(failure.code(), false),
                    null,
                    null);
        }
    }

    public CompanyOperationResult<Company> changeStatus(ChangeCompanyStatusCommand command) {
        Objects.requireNonNull(command, "command");
        Company current = companyRepository.findById(command.companyId()).orElse(null);
        if (current == null) {
            return rejected(
                    command.companyId(),
                    CompanyAuditOperation.CHANGE_COMPANY_STATUS,
                    null,
                    CompanyOperationCode.COMPANY_NOT_FOUND,
                    null,
                    null);
        }
        if (current.status() == command.desiredStatus()) {
            audit.record(
                    current.id(),
                    CompanyAuditOperation.CHANGE_COMPANY_STATUS,
                    CompanyAuditOutcome.UNCHANGED,
                    null,
                    null,
                    current.version(),
                    current.version());
            return CompanyOperationResult.unchanged(current);
        }
        if (current.version() != command.expectedVersion()) {
            return rejected(
                    current.id(),
                    CompanyAuditOperation.CHANGE_COMPANY_STATUS,
                    null,
                    CompanyOperationCode.COMPANY_VERSION_CONFLICT,
                    current.version(),
                    null);
        }
        if (command.desiredStatus() == CompanyStatus.ACTIVE) {
            Company preview = new Company(
                    current.id(), CompanyStatus.ACTIVE, current.customizationPluginId(), current.version());
            CompanyPluginResolution resolution = resolve(preview);
            if (!resolution.operational()) {
                return rejected(
                        current.id(),
                        CompanyAuditOperation.CHANGE_COMPANY_STATUS,
                        current.customizationPluginId(),
                        domainCode(resolution.diagnostics().getFirst().code()),
                        current.version(),
                        null);
            }
        }

        Company desired = new Company(
                current.id(), command.desiredStatus(), current.customizationPluginId(), current.version());
        try {
            Company stored = companyRepository.save(desired);
            audit.record(
                    stored.id(),
                    CompanyAuditOperation.CHANGE_COMPANY_STATUS,
                    CompanyAuditOutcome.CHANGED,
                    null,
                    null,
                    current.version(),
                    stored.version());
            return CompanyOperationResult.changed(stored);
        } catch (PersistenceConflictException failure) {
            return rejected(
                    current.id(),
                    CompanyAuditOperation.CHANGE_COMPANY_STATUS,
                    null,
                    mapCompanyConflict(failure.code(), false),
                    current.version(),
                    null);
        }
    }

    public CompanyOperationResult<Company> replaceCustomization(ReplaceCustomizationCommand command) {
        Objects.requireNonNull(command, "command");
        Company current = companyRepository.findById(command.companyId()).orElse(null);
        if (current == null) {
            return rejected(
                    command.companyId(),
                    CompanyAuditOperation.REPLACE_CUSTOMIZATION,
                    command.newCustomizationPluginId(),
                    CompanyOperationCode.COMPANY_NOT_FOUND,
                    null,
                    null);
        }
        if (current.customizationPluginId().equals(command.newCustomizationPluginId())) {
            audit.record(
                    current.id(),
                    CompanyAuditOperation.REPLACE_CUSTOMIZATION,
                    CompanyAuditOutcome.UNCHANGED,
                    current.customizationPluginId(),
                    null,
                    current.version(),
                    current.version());
            return CompanyOperationResult.unchanged(current);
        }
        if (current.version() != command.expectedVersion()) {
            return rejected(
                    current.id(),
                    CompanyAuditOperation.REPLACE_CUSTOMIZATION,
                    command.newCustomizationPluginId(),
                    CompanyOperationCode.CUSTOMIZATION_VERSION_CONFLICT,
                    current.version(),
                    null);
        }
        Optional<CompanyOperationCode> validation = validateCustomization(
                command.newCustomizationPluginId(), current.id());
        if (validation.isPresent()) {
            return rejected(
                    current.id(),
                    CompanyAuditOperation.REPLACE_CUSTOMIZATION,
                    command.newCustomizationPluginId(),
                    validation.orElseThrow(),
                    current.version(),
                    null);
        }

        Company desired = new Company(
                current.id(), current.status(), command.newCustomizationPluginId(), current.version());
        if (desired.isActive()) {
            CompanyPluginResolution resolution = resolve(desired);
            if (!resolution.operational()) {
                return rejected(
                        current.id(),
                        CompanyAuditOperation.REPLACE_CUSTOMIZATION,
                        command.newCustomizationPluginId(),
                        domainCode(resolution.diagnostics().getFirst().code()),
                        current.version(),
                        null);
            }
        }
        try {
            Company stored = companyRepository.save(desired);
            audit.record(
                    stored.id(),
                    CompanyAuditOperation.REPLACE_CUSTOMIZATION,
                    CompanyAuditOutcome.CHANGED,
                    stored.customizationPluginId(),
                    null,
                    current.version(),
                    stored.version());
            return CompanyOperationResult.changed(stored);
        } catch (PersistenceConflictException failure) {
            return rejected(
                    current.id(),
                    CompanyAuditOperation.REPLACE_CUSTOMIZATION,
                    command.newCustomizationPluginId(),
                    mapCompanyConflict(failure.code(), true),
                    current.version(),
                    null);
        }
    }

    private CompanyPluginResolution resolve(Company company) {
        List<PluginActivationDecision> decisions = activationRepository.findByCompanyId(company.id());
        return resolver.resolve(
                company,
                companyRepository.isCustomizationAssignedToAnotherCompany(
                        company.customizationPluginId(), company.id()),
                decisions,
                pluginRegistry.orderedPlugins());
    }

    private Optional<CompanyOperationCode> validateCustomization(
            PluginId customizationId,
            CompanyId companyId) {
        PluginDescriptor descriptor = pluginRegistry.find(customizationId).orElse(null);
        if (descriptor == null) {
            return Optional.of(CompanyOperationCode.CUSTOMIZATION_NOT_PRESENT);
        }
        if (descriptor.kind() != PluginKind.CUSTOMIZATION) {
            return Optional.of(CompanyOperationCode.CUSTOMIZATION_WRONG_KIND);
        }
        if (companyRepository.isCustomizationAssignedToAnotherCompany(customizationId, companyId)) {
            return Optional.of(CompanyOperationCode.CUSTOMIZATION_ALREADY_ASSIGNED);
        }
        return Optional.empty();
    }

    private <T> CompanyOperationResult<T> rejected(
            CompanyId companyId,
            CompanyAuditOperation operation,
            PluginId pluginId,
            CompanyOperationCode code,
            Long previousVersion,
            Long resultingVersion) {
        audit.record(
                companyId,
                operation,
                CompanyAuditOutcome.REJECTED,
                pluginId,
                code,
                previousVersion,
                resultingVersion);
        return CompanyOperationResult.rejected(code);
    }

    private static CompanyOperationCode mapCompanyConflict(
            PersistenceConflictCode code,
            boolean customizationReplacement) {
        return switch (code) {
            case COMPANY_ALREADY_EXISTS -> CompanyOperationCode.COMPANY_ALREADY_EXISTS;
            case COMPANY_NOT_FOUND -> CompanyOperationCode.COMPANY_NOT_FOUND;
            case COMPANY_VERSION_CONFLICT -> customizationReplacement
                    ? CompanyOperationCode.CUSTOMIZATION_VERSION_CONFLICT
                    : CompanyOperationCode.COMPANY_VERSION_CONFLICT;
            case CUSTOMIZATION_ALREADY_ASSIGNED ->
                    CompanyOperationCode.CUSTOMIZATION_ALREADY_ASSIGNED;
            default -> CompanyOperationCode.PERSISTENCE_CONFLICT;
        };
    }

    private static CompanyOperationCode domainCode(
            py.com.logixone.kernel.domain.company.CompanyPluginDiagnosticCode code) {
        return CompanyOperationCode.valueOf(code.name());
    }
}
