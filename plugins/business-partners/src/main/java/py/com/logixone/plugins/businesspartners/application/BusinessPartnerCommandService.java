package py.com.logixone.plugins.businesspartners.application;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.kernel.api.audit.TechnicalAuditEvent;
import py.com.logixone.kernel.api.audit.TechnicalAuditOutcome;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;
import py.com.logixone.plugins.businesspartners.application.command.BusinessPartnerCommands;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerCodeSequenceRepository;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerDefinitionRepository;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerIdGenerator;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerPersistenceCode;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerPersistenceException;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerRepository;
import py.com.logixone.plugins.businesspartners.application.port.CountryReferencePolicy;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartner;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAttributeCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionKind;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerSnapshot;
import py.com.logixone.plugins.businesspartners.domain.ConcurrentBusinessPartnerChangeException;

/** Framework-free orchestration of all mutating business-partner use cases. */
public final class BusinessPartnerCommandService {

    private static final String RESOURCE_TYPE = "business_partner";
    private static final String GENERAL_SEQUENCE = "general";

    private final BusinessPartnerRepository repository;
    private final BusinessPartnerDefinitionRepository definitions;
    private final BusinessPartnerCodeSequenceRepository sequences;
    private final BusinessPartnerIdGenerator idGenerator;
    private final CountryReferencePolicy countries;
    private final TechnicalAudit audit;
    private final Clock clock;

    public BusinessPartnerCommandService(
            BusinessPartnerRepository repository,
            BusinessPartnerDefinitionRepository definitions,
            BusinessPartnerCodeSequenceRepository sequences,
            BusinessPartnerIdGenerator idGenerator,
            CountryReferencePolicy countries,
            TechnicalAudit audit,
            Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.definitions = Objects.requireNonNull(definitions, "definitions");
        this.sequences = Objects.requireNonNull(sequences, "sequences");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.countries = Objects.requireNonNull(countries, "countries");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> register(
            BusinessPartnerOperationContext context,
            BusinessPartnerCommands.Register command) {
        Objects.requireNonNull(command, "command");
        if (!authorized(context, BusinessPartnerPermissions.MANAGE)) {
            return rejected(context, BusinessPartnerPermissions.MANAGE,
                    "REGISTER_BUSINESS_PARTNER", Optional.empty(), Optional.empty(),
                    BusinessPartnerResultCode.ACCESS_DENIED);
        }
        BusinessPartnerId id = idGenerator.nextId();
        BusinessPartnerCode code = command.code().orElseGet(() -> new BusinessPartnerCode(
                "BP-%08d".formatted(sequences.nextValue(
                        context.companyContext().companyId(), GENERAL_SEQUENCE))));
        BusinessPartner partner = BusinessPartner.create(
                context.companyContext().companyId(),
                id,
                code,
                command.kind(),
                command.displayName(),
                command.legalName(),
                command.tradeName());
        try {
            BusinessPartner inserted = repository.insert(partner);
            audit(context, BusinessPartnerPermissions.MANAGE, "REGISTER_BUSINESS_PARTNER",
                    Optional.of(id), TechnicalAuditOutcome.CHANGED,
                    BusinessPartnerResultCode.SUCCESS, Optional.empty(),
                    Optional.of(inserted.version()));
            return BusinessPartnerOperationResult.success(inserted.snapshot(), List.of());
        } catch (BusinessPartnerPersistenceException failure) {
            BusinessPartnerResultCode codeResult = map(failure.code());
            return rejected(context, BusinessPartnerPermissions.MANAGE,
                    "REGISTER_BUSINESS_PARTNER", Optional.of(id), Optional.empty(), codeResult);
        }
    }

    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> rename(
            BusinessPartnerOperationContext context,
            BusinessPartnerCommands.Rename command) {
        Objects.requireNonNull(command, "command");
        return mutate(context, BusinessPartnerPermissions.MANAGE, "RENAME_BUSINESS_PARTNER",
                command.id(), command.expectedVersion(), partner -> partner.rename(
                        command.expectedVersion(), command.displayName(),
                        command.legalName(), command.tradeName()), List.of());
    }

    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> changeCode(
            BusinessPartnerOperationContext context,
            BusinessPartnerCommands.ChangeCode command) {
        Objects.requireNonNull(command, "command");
        return mutate(context, BusinessPartnerPermissions.MANAGE, "CHANGE_BUSINESS_PARTNER_CODE",
                command.id(), command.expectedVersion(),
                partner -> partner.changeCode(command.expectedVersion(), command.code()), List.of());
    }

    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> addIdentification(
            BusinessPartnerOperationContext context,
            BusinessPartnerCommands.AddIdentification command) {
        Objects.requireNonNull(command, "command");
        if (!authorized(context, BusinessPartnerPermissions.MANAGE)) {
            return rejected(context, BusinessPartnerPermissions.MANAGE,
                    "ADD_BUSINESS_PARTNER_IDENTIFICATION", Optional.of(command.id()),
                    Optional.empty(), BusinessPartnerResultCode.ACCESS_DENIED);
        }
        if (!activeDefinition(
                context,
                BusinessPartnerDefinitionKind.IDENTIFICATION_TYPE,
                command.identification().type())) {
            return rejected(context, BusinessPartnerPermissions.MANAGE,
                    "ADD_BUSINESS_PARTNER_IDENTIFICATION", Optional.of(command.id()),
                    Optional.empty(), BusinessPartnerResultCode.INVALID_OPERATION);
        }
        if (command.identification().countryCode()
                .filter(code -> !countries.isEnabled(
                        context.companyContext().companyId(), code))
                .isPresent()) {
            return rejected(context, BusinessPartnerPermissions.MANAGE,
                    "ADD_BUSINESS_PARTNER_IDENTIFICATION", Optional.of(command.id()),
                    Optional.empty(), BusinessPartnerResultCode.INVALID_OPERATION);
        }
        List<BusinessPartnerId> candidates = repository.findIdentificationCandidates(
                        context.companyContext().companyId(),
                        command.identification().duplicateCandidateKey())
                .stream()
                .filter(candidate -> !candidate.equals(command.id()))
                .distinct()
                .toList();
        List<BusinessPartnerWarning> warnings = candidates.isEmpty()
                ? List.of()
                : List.of(new BusinessPartnerWarning(
                        BusinessPartnerWarning.Code.POTENTIAL_DUPLICATE_IDENTIFICATION,
                        candidates));
        return mutateAuthorized(context, BusinessPartnerPermissions.MANAGE,
                "ADD_BUSINESS_PARTNER_IDENTIFICATION", command.id(), command.expectedVersion(),
                partner -> partner.addIdentification(
                        command.expectedVersion(), command.identification()), warnings);
    }

    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> addAddress(
            BusinessPartnerOperationContext context,
            BusinessPartnerCommands.AddAddress command) {
        Objects.requireNonNull(command, "command");
        if (!authorized(context, BusinessPartnerPermissions.MANAGE)) {
            return rejected(context, BusinessPartnerPermissions.MANAGE,
                    "ADD_BUSINESS_PARTNER_ADDRESS", Optional.of(command.id()), Optional.empty(),
                    BusinessPartnerResultCode.ACCESS_DENIED);
        }
        if (!activeDefinition(context, BusinessPartnerDefinitionKind.ADDRESS_TYPE,
                        command.address().type())
                || !activeDefinition(context, BusinessPartnerDefinitionKind.ADDRESS_PURPOSE,
                        command.address().purpose())) {
            return rejected(context, BusinessPartnerPermissions.MANAGE,
                    "ADD_BUSINESS_PARTNER_ADDRESS", Optional.of(command.id()), Optional.empty(),
                    BusinessPartnerResultCode.INVALID_OPERATION);
        }
        return mutateAuthorized(context, BusinessPartnerPermissions.MANAGE,
                "ADD_BUSINESS_PARTNER_ADDRESS", command.id(), command.expectedVersion(),
                partner -> partner.addAddress(command.expectedVersion(), command.address()), List.of());
    }

    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> addChannel(
            BusinessPartnerOperationContext context,
            BusinessPartnerCommands.AddChannel command) {
        Objects.requireNonNull(command, "command");
        if (!authorized(context, BusinessPartnerPermissions.MANAGE)) {
            return rejected(context, BusinessPartnerPermissions.MANAGE,
                    "ADD_BUSINESS_PARTNER_CHANNEL", Optional.of(command.id()), Optional.empty(),
                    BusinessPartnerResultCode.ACCESS_DENIED);
        }
        if (!activeDefinition(context, BusinessPartnerDefinitionKind.CHANNEL_KIND,
                command.channel().kind())) {
            return rejected(context, BusinessPartnerPermissions.MANAGE,
                    "ADD_BUSINESS_PARTNER_CHANNEL", Optional.of(command.id()), Optional.empty(),
                    BusinessPartnerResultCode.INVALID_OPERATION);
        }
        return mutateAuthorized(context, BusinessPartnerPermissions.MANAGE,
                "ADD_BUSINESS_PARTNER_CHANNEL", command.id(), command.expectedVersion(),
                partner -> partner.addContactChannel(
                        command.expectedVersion(), command.channel()), List.of());
    }

    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> addContact(
            BusinessPartnerOperationContext context,
            BusinessPartnerCommands.AddContact command) {
        Objects.requireNonNull(command, "command");
        return mutate(context, BusinessPartnerPermissions.MANAGE, "ADD_BUSINESS_PARTNER_CONTACT",
                command.id(), command.expectedVersion(),
                partner -> partner.addContact(command.expectedVersion(), command.contact()), List.of());
    }

    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> assignRole(
            BusinessPartnerOperationContext context,
            BusinessPartnerCommands.AssignRole command) {
        Objects.requireNonNull(command, "command");
        return mutate(context, BusinessPartnerPermissions.ROLES_MANAGE,
                "ASSIGN_BUSINESS_PARTNER_ROLE", command.id(), command.expectedVersion(),
                partner -> partner.assignRole(
                        command.expectedVersion(), command.role(), command.roleCode()), List.of());
    }

    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> changeRoleState(
            BusinessPartnerOperationContext context,
            BusinessPartnerCommands.ChangeRoleState command) {
        Objects.requireNonNull(command, "command");
        return mutate(context, BusinessPartnerPermissions.ROLES_MANAGE,
                "CHANGE_BUSINESS_PARTNER_ROLE_STATE", command.id(), command.expectedVersion(),
                partner -> partner.changeRoleState(
                        command.expectedVersion(), command.role(), command.state()), List.of());
    }

    public BusinessPartnerOperationResult<BusinessPartnerSnapshot> changeLifecycle(
            BusinessPartnerOperationContext context,
            BusinessPartnerCommands.ChangeLifecycle command) {
        Objects.requireNonNull(command, "command");
        Consumer<BusinessPartner> change = command.state() == BusinessPartnerState.ACTIVE
                ? partner -> partner.reactivate(command.expectedVersion())
                : partner -> partner.inactivate(command.expectedVersion());
        return mutate(context, BusinessPartnerPermissions.LIFECYCLE_MANAGE,
                "CHANGE_BUSINESS_PARTNER_LIFECYCLE", command.id(), command.expectedVersion(),
                change, List.of());
    }

    private BusinessPartnerOperationResult<BusinessPartnerSnapshot> mutate(
            BusinessPartnerOperationContext context,
            ContributionId permission,
            String operation,
            BusinessPartnerId id,
            long expectedVersion,
            Consumer<BusinessPartner> mutation,
            List<BusinessPartnerWarning> warnings) {
        if (!authorized(context, permission)) {
            return rejected(context, permission, operation, Optional.of(id), Optional.empty(),
                    BusinessPartnerResultCode.ACCESS_DENIED);
        }
        return mutateAuthorized(context, permission, operation, id, expectedVersion, mutation, warnings);
    }

    private BusinessPartnerOperationResult<BusinessPartnerSnapshot> mutateAuthorized(
            BusinessPartnerOperationContext context,
            ContributionId permission,
            String operation,
            BusinessPartnerId id,
            long expectedVersion,
            Consumer<BusinessPartner> mutation,
            List<BusinessPartnerWarning> warnings) {
        Optional<BusinessPartner> found = repository.findById(
                context.companyContext().companyId(), id);
        if (found.isEmpty()) {
            return rejected(context, permission, operation, Optional.of(id), Optional.empty(),
                    BusinessPartnerResultCode.NOT_FOUND);
        }
        BusinessPartner partner = found.orElseThrow();
        long previousVersion = partner.version();
        try {
            mutation.accept(partner);
            BusinessPartner saved = partner.version() == previousVersion
                    ? partner
                    : repository.update(partner, previousVersion);
            TechnicalAuditOutcome outcome = saved.version() == previousVersion
                    ? TechnicalAuditOutcome.UNCHANGED
                    : TechnicalAuditOutcome.CHANGED;
            audit(context, permission, operation, Optional.of(id), outcome,
                    BusinessPartnerResultCode.SUCCESS, Optional.of(previousVersion),
                    Optional.of(saved.version()));
            return BusinessPartnerOperationResult.success(saved.snapshot(), warnings);
        } catch (ConcurrentBusinessPartnerChangeException failure) {
            return rejected(context, permission, operation, Optional.of(id),
                    Optional.of(previousVersion), BusinessPartnerResultCode.VERSION_CONFLICT);
        } catch (BusinessPartnerPersistenceException failure) {
            return rejected(context, permission, operation, Optional.of(id),
                    Optional.of(previousVersion), map(failure.code()));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return rejected(context, permission, operation, Optional.of(id),
                    Optional.of(previousVersion), BusinessPartnerResultCode.INVALID_OPERATION);
        }
    }

    private <T> BusinessPartnerOperationResult<T> rejected(
            BusinessPartnerOperationContext context,
            ContributionId permission,
            String operation,
            Optional<BusinessPartnerId> id,
            Optional<Long> previousVersion,
            BusinessPartnerResultCode code) {
        audit(context, permission, operation, id, TechnicalAuditOutcome.REJECTED,
                code, previousVersion, Optional.empty());
        return BusinessPartnerOperationResult.failure(code);
    }

    private boolean activeDefinition(
            BusinessPartnerOperationContext context,
            BusinessPartnerDefinitionKind kind,
            BusinessPartnerAttributeCode code) {
        return definitions.findByCodeForReference(
                        context.companyContext().companyId(), kind, code)
                .filter(definition -> definition.state() == BusinessPartnerState.ACTIVE)
                .isPresent();
    }

    private void audit(
            BusinessPartnerOperationContext context,
            ContributionId permission,
            String operation,
            Optional<BusinessPartnerId> id,
            TechnicalAuditOutcome outcome,
            BusinessPartnerResultCode code,
            Optional<Long> previousVersion,
            Optional<Long> resultingVersion) {
        audit.record(new TechnicalAuditEvent(
                operation,
                outcome,
                context.companyContext().actor().userId(),
                context.companyContext().companyId(),
                BusinessPartnersIdentity.PLUGIN_ID.value(),
                permission.value(),
                RESOURCE_TYPE,
                id.map(value -> value.value().toString()),
                code.name(),
                previousVersion,
                resultingVersion,
                context.correlationId(),
                clock.instant()));
    }

    private static boolean authorized(
            BusinessPartnerOperationContext context, ContributionId permission) {
        return Objects.requireNonNull(context, "context").authorizes(permission);
    }

    private static BusinessPartnerResultCode map(BusinessPartnerPersistenceCode code) {
        return switch (code) {
            case PARTNER_NOT_FOUND, DEFINITION_NOT_FOUND -> BusinessPartnerResultCode.NOT_FOUND;
            case PARTNER_ALREADY_EXISTS, GENERAL_CODE_ALREADY_EXISTS ->
                    BusinessPartnerResultCode.GENERAL_CODE_CONFLICT;
            case ROLE_CODE_ALREADY_EXISTS -> BusinessPartnerResultCode.ROLE_CODE_CONFLICT;
            case VERSION_CONFLICT -> BusinessPartnerResultCode.VERSION_CONFLICT;
            case INVALID_PERSISTED_STATE -> BusinessPartnerResultCode.INVALID_OPERATION;
        };
    }
}
