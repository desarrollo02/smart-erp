package py.com.logixone.plugins.commercialcatalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.audit.TechnicalAuditEvent;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.kernel.api.security.AuthenticatedActor;
import py.com.logixone.kernel.api.security.AuthenticatedCompanyContext;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.PriceEntryId;
import py.com.logixone.plugins.commercialcatalog.api.PriceListId;
import py.com.logixone.plugins.commercialcatalog.application.command.CatalogDefinitionCommands;
import py.com.logixone.plugins.commercialcatalog.application.definition.CatalogDefinitions;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogDefinitionRepository;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogIdGenerator;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogPersistenceCode;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogPersistenceException;
import py.com.logixone.plugins.commercialcatalog.domain.BrandId;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogDetailId;
import py.com.logixone.plugins.commercialcatalog.domain.CategoryId;
import py.com.logixone.plugins.commercialcatalog.domain.TagId;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileId;
import py.com.logixone.plugins.commercialcatalog.domain.UnitCode;
import py.com.logixone.plugins.commercialcatalog.domain.VariantAttributeCode;
import py.com.logixone.plugins.commercialcatalog.domain.VariantFamilyId;
import py.com.logixone.plugins.commercialcatalog.domain.VariantValueType;

class CatalogDefinitionServiceTest {

    private static final CompanyId COMPANY = new CompanyId(new UUID(0, 1));
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-30T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void createsControlledDefinitionsWithDedicatedPermissionAndTechnicalAudit() {
        MemoryDefinitions repository = new MemoryDefinitions();
        CountingIds ids = new CountingIds();
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        CatalogDefinitionService service = new CatalogDefinitionService(
                repository, ids, audit::add, CLOCK);
        CatalogOperationContext context = context(
                CommercialCatalogPermissions.DEFINITIONS_MANAGE);

        var unit = service.registerUnit(context,
                new CatalogDefinitionCommands.RegisterUnit(
                        new UnitCode("UN"), "Unidad", 0));
        var category = service.registerCategory(context,
                new CatalogDefinitionCommands.RegisterCategory(
                        Optional.empty(), "SERV", "Servicios"));
        var brand = service.registerBrand(context,
                new CatalogDefinitionCommands.RegisterBrand("ACME", "Marca privada"));
        var tag = service.registerTag(context,
                new CatalogDefinitionCommands.RegisterTag("NUEVO", "Nuevo"));
        var tax = service.registerTaxProfile(context,
                new CatalogDefinitionCommands.RegisterTaxProfile(
                        "IVA10", "IVA diez", "TAXED", "Descripción tributaria",
                        CLOCK.instant(), Optional.empty()));
        var family = service.registerVariantFamily(context,
                new CatalogDefinitionCommands.RegisterVariantFamily(
                        "COLOR", "Color", List.of(new CatalogDefinitions.VariantAttribute(
                                new VariantAttributeCode("TONO"), "Tono",
                                VariantValueType.TEXT, true, 0))));

        assertTrue(unit.successful());
        assertTrue(category.successful());
        assertTrue(brand.successful());
        assertTrue(tag.successful());
        assertTrue(tax.successful());
        assertTrue(family.successful());
        assertEquals(5, ids.calls);
        assertEquals(6, audit.size());
        assertTrue(audit.stream().allMatch(event ->
                event.permissionId().equals(
                        CommercialCatalogPermissions.DEFINITIONS_MANAGE.value())));
        assertFalse(audit.toString().contains("Marca privada"));
        assertFalse(audit.toString().contains("Descripción tributaria"));
    }

    @Test
    void deniesDefinitionIdentityGenerationAndReadsWithoutExactPermission() {
        MemoryDefinitions repository = new MemoryDefinitions();
        CountingIds ids = new CountingIds();
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        CatalogDefinitionService service = new CatalogDefinitionService(
                repository, ids, audit::add, CLOCK);

        var deniedCreate = service.registerCategory(
                context(CommercialCatalogPermissions.ITEMS_MANAGE),
                new CatalogDefinitionCommands.RegisterCategory(
                        Optional.empty(), "DENIED", "No crear"));
        var deniedRead = service.available(
                context(CommercialCatalogPermissions.DEFINITIONS_MANAGE));
        var allowedRead = service.available(context(CommercialCatalogPermissions.VIEW));
        var allowedManagedRead = service.managed(
                context(CommercialCatalogPermissions.DEFINITIONS_MANAGE));
        var deniedManagedRead = service.managed(context(CommercialCatalogPermissions.VIEW));

        assertEquals(CatalogResultCode.ACCESS_DENIED, deniedCreate.code());
        assertEquals(CatalogResultCode.ACCESS_DENIED, deniedRead.code());
        assertTrue(allowedRead.successful());
        assertTrue(allowedManagedRead.successful());
        assertEquals(CatalogResultCode.ACCESS_DENIED, deniedManagedRead.code());
        assertEquals(0, ids.calls);
        assertEquals(0, repository.insertions);
        assertEquals(2, repository.reads);
    }

    @Test
    void inactivatesAndReactivatesASimpleDefinitionWithOptimisticVersionAndAudit() {
        MemoryDefinitions repository = new MemoryDefinitions();
        repository.units.add(new CatalogDefinitions.Unit(
                new UnitCode("UN"), "Unidad", 0,
                CatalogDefinitions.State.ACTIVE, 3));
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        CatalogDefinitionService service = new CatalogDefinitionService(
                repository, new CountingIds(), audit::add, CLOCK);
        CatalogOperationContext context = context(
                CommercialCatalogPermissions.DEFINITIONS_MANAGE);

        var inactive = service.changeSimpleState(context,
                new CatalogDefinitionCommands.ChangeSimpleState(
                        CatalogDefinitions.SimpleKind.UNIT, "UN",
                        CatalogDefinitions.State.INACTIVE, 3));
        var active = service.changeSimpleState(context,
                new CatalogDefinitionCommands.ChangeSimpleState(
                        CatalogDefinitions.SimpleKind.UNIT, "UN",
                        CatalogDefinitions.State.ACTIVE, 4));

        assertTrue(inactive.successful());
        assertEquals(4, inactive.value().orElseThrow().version());
        assertTrue(active.successful());
        assertEquals(5, active.value().orElseThrow().version());
        assertEquals(CatalogDefinitions.State.ACTIVE, repository.units.getFirst().state());
        assertEquals(List.of("INACTIVATE_CATALOG_DEFINITION", "REACTIVATE_CATALOG_DEFINITION"),
                audit.stream().map(TechnicalAuditEvent::operation).toList());
        assertEquals(Optional.of(3L), audit.getFirst().previousVersion());
        assertEquals(Optional.of(4L), audit.getFirst().resultingVersion());
    }

    @Test
    void rejectsStaleOrUnauthorizedSimpleDefinitionStateChanges() {
        MemoryDefinitions repository = new MemoryDefinitions();
        repository.units.add(new CatalogDefinitions.Unit(
                new UnitCode("UN"), "Unidad", 0,
                CatalogDefinitions.State.ACTIVE, 2));
        CatalogDefinitionService service = new CatalogDefinitionService(
                repository, new CountingIds(), event -> { }, CLOCK);
        CatalogDefinitionCommands.ChangeSimpleState command =
                new CatalogDefinitionCommands.ChangeSimpleState(
                        CatalogDefinitions.SimpleKind.UNIT, "UN",
                        CatalogDefinitions.State.INACTIVE, 1);

        var stale = service.changeSimpleState(
                context(CommercialCatalogPermissions.DEFINITIONS_MANAGE), command);
        var denied = service.changeSimpleState(
                context(CommercialCatalogPermissions.VIEW), command);

        assertEquals(CatalogResultCode.VERSION_CONFLICT, stale.code());
        assertEquals(CatalogResultCode.ACCESS_DENIED, denied.code());
        assertEquals(CatalogDefinitions.State.ACTIVE, repository.units.getFirst().state());
    }

    @Test
    void revisesAndReadsSimpleDefinitionHistoryWithoutChangingIdentity() {
        MemoryDefinitions repository = new MemoryDefinitions();
        repository.units.add(new CatalogDefinitions.Unit(
                new UnitCode("UN"), "Unidad", 0,
                CatalogDefinitions.State.ACTIVE, 2));
        repository.simpleHistory.add(new CatalogDefinitions.SimpleRevision(
                CatalogDefinitions.SimpleKind.UNIT, "UN", 2, "Unidad",
                Optional.of(0), Optional.empty(), CatalogDefinitions.State.ACTIVE, true));
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        CatalogDefinitionService service = new CatalogDefinitionService(
                repository, new CountingIds(), audit::add, CLOCK);
        CatalogOperationContext manager = context(
                CommercialCatalogPermissions.DEFINITIONS_MANAGE);

        var revised = service.reviseSimpleDefinition(manager,
                new CatalogDefinitionCommands.ReviseSimpleDefinition(
                        CatalogDefinitions.SimpleKind.UNIT, "UN", "Unidad medible",
                        Optional.of(3), Optional.empty(), 2));
        var history = service.simpleDefinitionHistory(
                manager, CatalogDefinitions.SimpleKind.UNIT, "UN");
        var denied = service.simpleDefinitionHistory(
                context(CommercialCatalogPermissions.VIEW),
                CatalogDefinitions.SimpleKind.UNIT, "UN");

        assertTrue(revised.successful());
        assertEquals("UN", revised.value().orElseThrow().identity());
        assertEquals("Unidad medible", repository.units.getFirst().displayName());
        assertEquals(3, repository.units.getFirst().decimalScale());
        assertEquals(3, repository.units.getFirst().version());
        assertEquals(List.of(3L, 2L), history.value().orElseThrow().stream()
                .map(CatalogDefinitions.SimpleRevision::version).toList());
        assertEquals(COMPANY, repository.lastSimpleHistoryCompany);
        assertEquals(CatalogResultCode.ACCESS_DENIED, denied.code());
        assertEquals("REVISE_CATALOG_DEFINITION", audit.getFirst().operation());
        assertEquals(Optional.of(2L), audit.getFirst().previousVersion());
        assertEquals(Optional.of(3L), audit.getFirst().resultingVersion());
        assertFalse(audit.toString().contains("Unidad medible"));

        var stale = service.reviseSimpleDefinition(manager,
                new CatalogDefinitionCommands.ReviseSimpleDefinition(
                        CatalogDefinitions.SimpleKind.UNIT, "UN", "Obsoleta",
                        Optional.of(2), Optional.empty(), 2));
        assertEquals(CatalogResultCode.VERSION_CONFLICT, stale.code());
    }

    @Test
    void replacesAnActiveDefinitionWithNewIdentityAndAuditsOnlyTheLinkAndVersions() {
        MemoryDefinitions repository = new MemoryDefinitions();
        BrandId previousId = new BrandId(new UUID(0, 40));
        repository.brands.add(new CatalogDefinitions.Brand(
                previousId, "OLD", "Marca anterior",
                CatalogDefinitions.State.ACTIVE, 4));
        CountingIds ids = new CountingIds();
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        CatalogDefinitionService service = new CatalogDefinitionService(
                repository, ids, audit::add, CLOCK);
        CatalogDefinitionCommands.ReplaceSimpleDefinition command =
                new CatalogDefinitionCommands.ReplaceSimpleDefinition(
                        CatalogDefinitions.SimpleKind.BRAND,
                        previousId.value().toString(),
                        "NEW", "Marca sucesora", Optional.empty(), Optional.empty(), 4);

        var replaced = service.replaceSimpleDefinition(
                context(CommercialCatalogPermissions.DEFINITIONS_MANAGE), command);

        assertTrue(replaced.successful());
        CatalogDefinitions.Replacement result = replaced.value().orElseThrow();
        assertEquals(previousId.value().toString(), result.previousIdentity());
        assertEquals(new UUID(0, 32).toString(), result.replacementIdentity());
        assertEquals(5, result.previousVersion());
        assertEquals(0, result.replacementVersion());
        assertEquals(CatalogDefinitions.State.INACTIVE, repository.brands.getFirst().state());
        assertEquals(CatalogDefinitions.State.ACTIVE, repository.brands.get(1).state());
        assertEquals(1, ids.calls);
        assertEquals("REPLACE_CATALOG_DEFINITION", audit.getFirst().operation());
        assertEquals(Optional.of(4L), audit.getFirst().previousVersion());
        assertEquals(Optional.of(5L), audit.getFirst().resultingVersion());
        assertFalse(audit.toString().contains("Marca anterior"));
        assertFalse(audit.toString().contains("Marca sucesora"));

        var denied = service.replaceSimpleDefinition(
                context(CommercialCatalogPermissions.VIEW), command);
        assertEquals(CatalogResultCode.ACCESS_DENIED, denied.code());
        assertEquals(1, ids.calls);
    }

    @Test
    void inactivatesAndReactivatesATaxProfileWithVersionedAudit() {
        MemoryDefinitions repository = new MemoryDefinitions();
        TaxProfileId id = new TaxProfileId(new UUID(0, 77));
        repository.taxes.add(new CatalogDefinitions.TaxProfile(
                id, "IVA10", "IVA diez", "TAXED", "Perfil de prueba",
                CLOCK.instant(), Optional.empty(), CatalogDefinitions.State.ACTIVE, 2));
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        CatalogDefinitionService service = new CatalogDefinitionService(
                repository, new CountingIds(), audit::add, CLOCK);
        CatalogOperationContext context = context(
                CommercialCatalogPermissions.DEFINITIONS_MANAGE);

        var inactive = service.changeTaxProfileState(context,
                new CatalogDefinitionCommands.ChangeTaxProfileState(
                        id, CatalogDefinitions.State.INACTIVE, 2));
        var active = service.changeTaxProfileState(context,
                new CatalogDefinitionCommands.ChangeTaxProfileState(
                        id, CatalogDefinitions.State.ACTIVE, 3));

        assertTrue(inactive.successful());
        assertEquals(3, inactive.value().orElseThrow().version());
        assertTrue(active.successful());
        assertEquals(4, active.value().orElseThrow().version());
        assertEquals(CatalogDefinitions.State.ACTIVE, repository.taxes.getFirst().state());
        assertEquals(List.of(
                        "INACTIVATE_CATALOG_TAX_PROFILE",
                        "REACTIVATE_CATALOG_TAX_PROFILE"),
                audit.stream().map(TechnicalAuditEvent::operation).toList());
        assertEquals(Optional.of(2L), audit.getFirst().previousVersion());
        assertEquals(Optional.of(3L), audit.getFirst().resultingVersion());
        assertFalse(audit.toString().contains("Perfil de prueba"));
    }

    @Test
    void inactivatesAndReactivatesAVariantFamilyWithoutChangingItsAttributes() {
        MemoryDefinitions repository = new MemoryDefinitions();
        VariantFamilyId id = new VariantFamilyId(new UUID(0, 80));
        List<CatalogDefinitions.VariantAttribute> attributes = List.of(
                new CatalogDefinitions.VariantAttribute(
                        new VariantAttributeCode("COLOR"), "Color",
                        VariantValueType.TEXT, true, 0));
        repository.families.add(new CatalogDefinitions.VariantFamily(
                id, "ROPA", "Ropa", attributes,
                CatalogDefinitions.State.ACTIVE, 5));
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        CatalogDefinitionService service = new CatalogDefinitionService(
                repository, new CountingIds(), audit::add, CLOCK);
        CatalogOperationContext manager = context(
                CommercialCatalogPermissions.DEFINITIONS_MANAGE);

        var inactive = service.changeVariantFamilyState(manager,
                new CatalogDefinitionCommands.ChangeVariantFamilyState(
                        id, CatalogDefinitions.State.INACTIVE, 5));
        var active = service.changeVariantFamilyState(manager,
                new CatalogDefinitionCommands.ChangeVariantFamilyState(
                        id, CatalogDefinitions.State.ACTIVE, 6));
        var stale = service.changeVariantFamilyState(manager,
                new CatalogDefinitionCommands.ChangeVariantFamilyState(
                        id, CatalogDefinitions.State.INACTIVE, 5));
        var denied = service.changeVariantFamilyState(
                context(CommercialCatalogPermissions.VIEW),
                new CatalogDefinitionCommands.ChangeVariantFamilyState(
                        id, CatalogDefinitions.State.INACTIVE, 7));

        assertTrue(inactive.successful());
        assertEquals(6, inactive.value().orElseThrow().version());
        assertTrue(active.successful());
        assertEquals(7, active.value().orElseThrow().version());
        assertEquals(attributes, repository.families.getFirst().attributes());
        assertEquals(CatalogDefinitions.State.ACTIVE, repository.families.getFirst().state());
        assertEquals(CatalogResultCode.VERSION_CONFLICT, stale.code());
        assertEquals(CatalogResultCode.ACCESS_DENIED, denied.code());
        assertEquals(List.of(
                        "INACTIVATE_CATALOG_VARIANT_FAMILY",
                        "REACTIVATE_CATALOG_VARIANT_FAMILY",
                        "INACTIVATE_CATALOG_VARIANT_FAMILY",
                        "INACTIVATE_CATALOG_VARIANT_FAMILY"),
                audit.stream().map(TechnicalAuditEvent::operation).toList());
        assertEquals(Optional.of(5L), audit.getFirst().previousVersion());
        assertEquals(Optional.of(6L), audit.getFirst().resultingVersion());
        assertFalse(audit.toString().contains("Color"));
    }

    @Test
    void revisesAndReadsVariantFamilyHistoryWithoutMutatingItsIdentityOrCode() {
        MemoryDefinitions repository = new MemoryDefinitions();
        VariantFamilyId id = new VariantFamilyId(new UUID(0, 81));
        List<CatalogDefinitions.VariantAttribute> original = List.of(
                new CatalogDefinitions.VariantAttribute(
                        new VariantAttributeCode("COLOR"), "Color",
                        VariantValueType.TEXT, true, 0));
        repository.families.add(new CatalogDefinitions.VariantFamily(
                id, "ROPA", "Ropa", original,
                CatalogDefinitions.State.ACTIVE, 2));
        repository.variantHistory.add(new CatalogDefinitions.VariantFamilyRevision(
                id, 2, "Ropa", original, CatalogDefinitions.State.ACTIVE, true));
        List<CatalogDefinitions.VariantAttribute> revisedAttributes = List.of(
                new CatalogDefinitions.VariantAttribute(
                        new VariantAttributeCode("TALLE"), "Talle",
                        VariantValueType.TEXT, true, 0),
                new CatalogDefinitions.VariantAttribute(
                        new VariantAttributeCode("TEMPORADA"), "Temporada",
                        VariantValueType.TEXT, false, 1));
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        CatalogDefinitionService service = new CatalogDefinitionService(
                repository, new CountingIds(), audit::add, CLOCK);
        CatalogOperationContext manager = context(
                CommercialCatalogPermissions.DEFINITIONS_MANAGE);

        var revised = service.reviseVariantFamily(manager,
                new CatalogDefinitionCommands.ReviseVariantFamily(
                        id, "Ropa por talle", revisedAttributes, 2));
        var history = service.variantFamilyHistory(manager, id);
        var stale = service.reviseVariantFamily(manager,
                new CatalogDefinitionCommands.ReviseVariantFamily(
                        id, "Intento obsoleto", original, 2));
        var denied = service.variantFamilyHistory(
                context(CommercialCatalogPermissions.VIEW), id);

        assertTrue(revised.successful());
        CatalogDefinitions.VariantFamily family = revised.value().orElseThrow();
        assertEquals(id, family.id());
        assertEquals("ROPA", family.code());
        assertEquals("Ropa por talle", family.displayName());
        assertEquals(revisedAttributes, family.attributes());
        assertEquals(3, family.version());
        assertEquals(List.of(3L, 2L), history.value().orElseThrow().stream()
                .map(CatalogDefinitions.VariantFamilyRevision::version).toList());
        assertTrue(history.value().orElseThrow().getFirst().current());
        assertFalse(history.value().orElseThrow().get(1).current());
        assertEquals(COMPANY, repository.lastVariantHistoryCompany);
        assertEquals(CatalogResultCode.VERSION_CONFLICT, stale.code());
        assertEquals(CatalogResultCode.ACCESS_DENIED, denied.code());
        assertEquals("REVISE_CATALOG_VARIANT_FAMILY", audit.getFirst().operation());
        assertEquals(Optional.of(2L), audit.getFirst().previousVersion());
        assertEquals(Optional.of(3L), audit.getFirst().resultingVersion());
        assertFalse(audit.toString().contains("Ropa por talle"));
        assertFalse(audit.toString().contains("Temporada"));
    }

    @Test
    void revisesATaxProfileWithoutMutatingItsIdentityAndAuditsOnlyVersions() {
        MemoryDefinitions repository = new MemoryDefinitions();
        TaxProfileId id = new TaxProfileId(new UUID(0, 78));
        repository.taxes.add(new CatalogDefinitions.TaxProfile(
                id, "IVA10", "IVA diez", "TAXED", "Perfil original",
                CLOCK.instant(), Optional.empty(), CatalogDefinitions.State.ACTIVE, 2));
        List<TechnicalAuditEvent> audit = new ArrayList<>();
        CatalogDefinitionService service = new CatalogDefinitionService(
                repository, new CountingIds(), audit::add, CLOCK);

        var revised = service.reviseTaxProfile(
                context(CommercialCatalogPermissions.DEFINITIONS_MANAGE),
                new CatalogDefinitionCommands.ReviseTaxProfile(
                        id, "TAXED_REDUCED", "Perfil revisado",
                        CLOCK.instant().plusSeconds(3600), Optional.empty(), 2));

        assertTrue(revised.successful());
        CatalogDefinitions.TaxProfile profile = revised.value().orElseThrow();
        assertEquals("IVA10", profile.code());
        assertEquals("IVA diez", profile.displayName());
        assertEquals("TAXED_REDUCED", profile.internalKindCode());
        assertEquals(3, profile.version());
        assertEquals("REVISE_CATALOG_TAX_PROFILE", audit.getFirst().operation());
        assertEquals(Optional.of(2L), audit.getFirst().previousVersion());
        assertEquals(Optional.of(3L), audit.getFirst().resultingVersion());
        assertFalse(audit.toString().contains("Perfil revisado"));

        var stale = service.reviseTaxProfile(
                context(CommercialCatalogPermissions.DEFINITIONS_MANAGE),
                new CatalogDefinitionCommands.ReviseTaxProfile(
                        id, "TAXED_STANDARD", "Intento obsoleto",
                        CLOCK.instant(), Optional.empty(), 2));
        var denied = service.reviseTaxProfile(
                context(CommercialCatalogPermissions.VIEW),
                new CatalogDefinitionCommands.ReviseTaxProfile(
                        id, "TAXED_STANDARD", "Intento denegado",
                        CLOCK.instant(), Optional.empty(), 3));

        assertEquals(CatalogResultCode.VERSION_CONFLICT, stale.code());
        assertEquals(CatalogResultCode.ACCESS_DENIED, denied.code());
        assertEquals("TAXED_REDUCED", repository.taxes.getFirst().internalKindCode());
    }

    @Test
    void readsTaxProfileHistoryOnlyForTheAuthorizedCompanyManager() {
        MemoryDefinitions repository = new MemoryDefinitions();
        TaxProfileId id = new TaxProfileId(new UUID(0, 79));
        repository.taxHistory.add(new CatalogDefinitions.TaxProfileRevision(
                id, 1, "TAXED_REDUCED", "Perfil revisado",
                CLOCK.instant(), Optional.empty(), true));
        repository.taxHistory.add(new CatalogDefinitions.TaxProfileRevision(
                id, 0, "TAXED_STANDARD", "Perfil original",
                CLOCK.instant().minusSeconds(3600), Optional.empty(), false));
        CatalogDefinitionService service = new CatalogDefinitionService(
                repository, new CountingIds(), ignored -> { }, CLOCK);

        var allowed = service.taxProfileHistory(
                context(CommercialCatalogPermissions.DEFINITIONS_MANAGE), id);
        var denied = service.taxProfileHistory(
                context(CommercialCatalogPermissions.VIEW), id);

        assertTrue(allowed.successful());
        assertEquals(List.of(1L, 0L), allowed.value().orElseThrow().stream()
                .map(CatalogDefinitions.TaxProfileRevision::version).toList());
        assertEquals(COMPANY, repository.lastHistoryCompany);
        assertEquals(CatalogResultCode.ACCESS_DENIED, denied.code());
    }

    private static CatalogOperationContext context(ContributionId permission) {
        return new CatalogOperationContext(
                new AuthenticatedCompanyContext(
                        new AuthenticatedActor(new AppUserId(new UUID(0, 99))), COMPANY),
                CommercialCatalogIdentity.PLUGIN_ID, permission,
                "request:catalog-definition");
    }

    private static final class MemoryDefinitions implements CatalogDefinitionRepository {
        private final List<CatalogDefinitions.Unit> units = new ArrayList<>();
        private final List<CatalogDefinitions.Category> categories = new ArrayList<>();
        private final List<CatalogDefinitions.Brand> brands = new ArrayList<>();
        private final List<CatalogDefinitions.Tag> tags = new ArrayList<>();
        private final List<CatalogDefinitions.SimpleRevision> simpleHistory =
                new ArrayList<>();
        private final List<CatalogDefinitions.TaxProfile> taxes = new ArrayList<>();
        private final List<CatalogDefinitions.TaxProfileRevision> taxHistory = new ArrayList<>();
        private final List<CatalogDefinitions.VariantFamily> families = new ArrayList<>();
        private final List<CatalogDefinitions.VariantFamilyRevision> variantHistory =
                new ArrayList<>();
        private final List<CatalogDefinitions.ReplacementLink> replacements = new ArrayList<>();
        private CompanyId lastHistoryCompany;
        private CompanyId lastSimpleHistoryCompany;
        private CompanyId lastVariantHistoryCompany;
        private int insertions;
        private int reads;

        @Override
        public CatalogDefinitions.Snapshot findAll(CompanyId companyId) {
            reads++;
            return new CatalogDefinitions.Snapshot(
                    units, categories, brands, tags, taxes, families, replacements);
        }

        @Override public CatalogDefinitions.Unit insert(
                CompanyId companyId, CatalogDefinitions.Unit definition) {
            insertions++; units.add(definition); return definition;
        }
        @Override public CatalogDefinitions.Category insert(
                CompanyId companyId, CatalogDefinitions.Category definition) {
            insertions++; categories.add(definition); return definition;
        }
        @Override public CatalogDefinitions.Brand insert(
                CompanyId companyId, CatalogDefinitions.Brand definition) {
            insertions++; brands.add(definition); return definition;
        }
        @Override public CatalogDefinitions.Tag insert(
                CompanyId companyId, CatalogDefinitions.Tag definition) {
            insertions++; tags.add(definition); return definition;
        }
        @Override public CatalogDefinitions.Lifecycle changeSimpleState(
                CompanyId companyId,
                CatalogDefinitions.SimpleKind kind,
                String identity,
                CatalogDefinitions.State targetState,
                long expectedVersion) {
            if (kind != CatalogDefinitions.SimpleKind.UNIT) {
                throw new UnsupportedOperationException(kind.name());
            }
            for (int index = 0; index < units.size(); index++) {
                CatalogDefinitions.Unit current = units.get(index);
                if (!current.code().value().equals(identity)) {
                    continue;
                }
                if (current.version() != expectedVersion) {
                    throw new CatalogPersistenceException(
                            CatalogPersistenceCode.VERSION_CONFLICT);
                }
                boolean changed = current.state() != targetState;
                long version = changed ? expectedVersion + 1 : expectedVersion;
                units.set(index, new CatalogDefinitions.Unit(
                        current.code(), current.displayName(), current.decimalScale(),
                        targetState, version));
                return new CatalogDefinitions.Lifecycle(
                        kind, identity, targetState, version, changed);
            }
            throw new CatalogPersistenceException(
                    CatalogPersistenceCode.DEFINITION_NOT_FOUND);
        }
        @Override public CatalogDefinitions.SimpleRevision reviseSimpleDefinition(
                CompanyId companyId,
                CatalogDefinitions.SimpleKind kind,
                String identity,
                String displayName,
                Optional<Integer> decimalScale,
                Optional<CategoryId> parentId,
                long expectedVersion) {
            if (kind != CatalogDefinitions.SimpleKind.UNIT) {
                throw new UnsupportedOperationException(kind.name());
            }
            for (int index = 0; index < units.size(); index++) {
                CatalogDefinitions.Unit current = units.get(index);
                if (!current.code().value().equals(identity)) {
                    continue;
                }
                if (current.version() != expectedVersion) {
                    throw new CatalogPersistenceException(
                            CatalogPersistenceCode.VERSION_CONFLICT);
                }
                CatalogDefinitions.Unit revised = new CatalogDefinitions.Unit(
                        current.code(), displayName, decimalScale.orElseThrow(),
                        current.state(), expectedVersion + 1);
                units.set(index, revised);
                simpleHistory.replaceAll(revision ->
                        revision.kind() == kind && revision.identity().equals(identity)
                                ? new CatalogDefinitions.SimpleRevision(
                                        revision.kind(), revision.identity(), revision.version(),
                                        revision.displayName(), revision.decimalScale(),
                                        revision.parentId(), revision.state(), false)
                                : revision);
                CatalogDefinitions.SimpleRevision revision =
                        new CatalogDefinitions.SimpleRevision(
                                kind, identity, expectedVersion + 1, displayName,
                                decimalScale, parentId, current.state(), true);
                simpleHistory.add(0, revision);
                return revision;
            }
            throw new CatalogPersistenceException(
                    CatalogPersistenceCode.DEFINITION_NOT_FOUND);
        }
        @Override public List<CatalogDefinitions.SimpleRevision> simpleDefinitionHistory(
                CompanyId companyId,
                CatalogDefinitions.SimpleKind kind,
                String identity) {
            lastSimpleHistoryCompany = companyId;
            List<CatalogDefinitions.SimpleRevision> result = simpleHistory.stream()
                    .filter(revision -> revision.kind() == kind
                            && revision.identity().equals(identity))
                    .toList();
            if (result.isEmpty()) {
                throw new CatalogPersistenceException(
                        CatalogPersistenceCode.DEFINITION_NOT_FOUND);
            }
            return result;
        }
        @Override public CatalogDefinitions.Replacement replaceSimpleDefinition(
                CompanyId companyId,
                CatalogDefinitions.SimpleKind kind,
                String identity,
                CatalogDefinitions.ReplacementCandidate replacement,
                long expectedVersion) {
            if (kind != CatalogDefinitions.SimpleKind.BRAND) {
                throw new UnsupportedOperationException(kind.name());
            }
            for (int index = 0; index < brands.size(); index++) {
                CatalogDefinitions.Brand current = brands.get(index);
                if (!current.id().value().toString().equals(identity)) {
                    continue;
                }
                if (current.version() != expectedVersion) {
                    throw new CatalogPersistenceException(
                            CatalogPersistenceCode.VERSION_CONFLICT);
                }
                if (current.state() != CatalogDefinitions.State.ACTIVE) {
                    throw new IllegalStateException("Only an active definition can be replaced");
                }
                brands.set(index, new CatalogDefinitions.Brand(
                        current.id(), current.code(), current.displayName(),
                        CatalogDefinitions.State.INACTIVE, expectedVersion + 1));
                CatalogDefinitions.Brand successor = new CatalogDefinitions.Brand(
                        new BrandId(UUID.fromString(replacement.identity())),
                        replacement.code(), replacement.displayName(),
                        CatalogDefinitions.State.ACTIVE, 0);
                brands.add(successor);
                replacements.add(new CatalogDefinitions.ReplacementLink(
                        kind, identity, replacement.identity()));
                return new CatalogDefinitions.Replacement(
                        kind, identity, expectedVersion + 1, replacement.identity(),
                        replacement.code(), 0);
            }
            throw new CatalogPersistenceException(
                    CatalogPersistenceCode.DEFINITION_NOT_FOUND);
        }
        @Override public CatalogDefinitions.TaxProfile insert(
                CompanyId companyId, CatalogDefinitions.TaxProfile definition) {
            insertions++; taxes.add(definition); return definition;
        }
        @Override public CatalogDefinitions.TaxProfile changeTaxProfileState(
                CompanyId companyId,
                TaxProfileId id,
                CatalogDefinitions.State targetState,
                long expectedVersion) {
            for (int index = 0; index < taxes.size(); index++) {
                CatalogDefinitions.TaxProfile current = taxes.get(index);
                if (!current.id().equals(id)) {
                    continue;
                }
                if (current.version() != expectedVersion) {
                    throw new CatalogPersistenceException(
                            CatalogPersistenceCode.VERSION_CONFLICT);
                }
                long version = current.state() == targetState
                        ? expectedVersion
                        : expectedVersion + 1;
                CatalogDefinitions.TaxProfile changed = new CatalogDefinitions.TaxProfile(
                        current.id(), current.code(), current.displayName(),
                        current.internalKindCode(), current.description(), current.validFrom(),
                        current.validUntil(), targetState, version);
                taxes.set(index, changed);
                return changed;
            }
            throw new CatalogPersistenceException(
                    CatalogPersistenceCode.DEFINITION_NOT_FOUND);
        }
        @Override public CatalogDefinitions.TaxProfile reviseTaxProfile(
                CompanyId companyId,
                TaxProfileId id,
                String internalKindCode,
                String description,
                Instant validFrom,
                Optional<Instant> validUntil,
                long expectedVersion) {
            for (int index = 0; index < taxes.size(); index++) {
                CatalogDefinitions.TaxProfile current = taxes.get(index);
                if (!current.id().equals(id)) {
                    continue;
                }
                if (current.version() != expectedVersion) {
                    throw new CatalogPersistenceException(
                            CatalogPersistenceCode.VERSION_CONFLICT);
                }
                CatalogDefinitions.TaxProfile revised = new CatalogDefinitions.TaxProfile(
                        current.id(), current.code(), current.displayName(), internalKindCode,
                        description, validFrom, validUntil, current.state(),
                        expectedVersion + 1);
                taxes.set(index, revised);
                return revised;
            }
            throw new CatalogPersistenceException(
                    CatalogPersistenceCode.DEFINITION_NOT_FOUND);
        }
        @Override public List<CatalogDefinitions.TaxProfileRevision> taxProfileHistory(
                CompanyId companyId, TaxProfileId id) {
            lastHistoryCompany = companyId;
            List<CatalogDefinitions.TaxProfileRevision> result = taxHistory.stream()
                    .filter(revision -> revision.profileId().equals(id))
                    .toList();
            if (result.isEmpty()) {
                throw new CatalogPersistenceException(
                        CatalogPersistenceCode.DEFINITION_NOT_FOUND);
            }
            return result;
        }
        @Override public CatalogDefinitions.VariantFamily insert(
                CompanyId companyId, CatalogDefinitions.VariantFamily definition) {
            insertions++; families.add(definition); return definition;
        }
        @Override public CatalogDefinitions.VariantFamily changeVariantFamilyState(
                CompanyId companyId,
                VariantFamilyId id,
                CatalogDefinitions.State targetState,
                long expectedVersion) {
            for (int index = 0; index < families.size(); index++) {
                CatalogDefinitions.VariantFamily current = families.get(index);
                if (!current.id().equals(id)) {
                    continue;
                }
                if (current.version() != expectedVersion) {
                    throw new CatalogPersistenceException(
                            CatalogPersistenceCode.VERSION_CONFLICT);
                }
                long version = current.state() == targetState
                        ? expectedVersion
                        : expectedVersion + 1;
                CatalogDefinitions.VariantFamily changed =
                        new CatalogDefinitions.VariantFamily(
                                current.id(), current.code(), current.displayName(),
                                current.attributes(), targetState, version);
                families.set(index, changed);
                if (version != expectedVersion) {
                    variantHistory.replaceAll(revision -> revision.familyId().equals(id)
                            ? new CatalogDefinitions.VariantFamilyRevision(
                                    revision.familyId(), revision.version(),
                                    revision.displayName(), revision.attributes(),
                                    revision.state(), false)
                            : revision);
                    variantHistory.add(0, new CatalogDefinitions.VariantFamilyRevision(
                            id, version, changed.displayName(), changed.attributes(),
                            targetState, true));
                }
                return changed;
            }
            throw new CatalogPersistenceException(
                    CatalogPersistenceCode.DEFINITION_NOT_FOUND);
        }
        @Override public CatalogDefinitions.VariantFamily reviseVariantFamily(
                CompanyId companyId,
                VariantFamilyId id,
                String displayName,
                List<CatalogDefinitions.VariantAttribute> attributes,
                long expectedVersion) {
            for (int index = 0; index < families.size(); index++) {
                CatalogDefinitions.VariantFamily current = families.get(index);
                if (!current.id().equals(id)) {
                    continue;
                }
                if (current.version() != expectedVersion) {
                    throw new CatalogPersistenceException(
                            CatalogPersistenceCode.VERSION_CONFLICT);
                }
                CatalogDefinitions.VariantFamily revised =
                        new CatalogDefinitions.VariantFamily(
                                id, current.code(), displayName, attributes,
                                current.state(), expectedVersion + 1);
                families.set(index, revised);
                variantHistory.replaceAll(revision -> revision.familyId().equals(id)
                        ? new CatalogDefinitions.VariantFamilyRevision(
                                revision.familyId(), revision.version(),
                                revision.displayName(), revision.attributes(),
                                revision.state(), false)
                        : revision);
                variantHistory.add(0, new CatalogDefinitions.VariantFamilyRevision(
                        id, revised.version(), revised.displayName(), revised.attributes(),
                        revised.state(), true));
                return revised;
            }
            throw new CatalogPersistenceException(
                    CatalogPersistenceCode.DEFINITION_NOT_FOUND);
        }
        @Override public List<CatalogDefinitions.VariantFamilyRevision> variantFamilyHistory(
                CompanyId companyId, VariantFamilyId id) {
            lastVariantHistoryCompany = companyId;
            List<CatalogDefinitions.VariantFamilyRevision> result = variantHistory.stream()
                    .filter(revision -> revision.familyId().equals(id))
                    .toList();
            if (result.isEmpty()) {
                throw new CatalogPersistenceException(
                        CatalogPersistenceCode.DEFINITION_NOT_FOUND);
            }
            return result;
        }
    }

    private static final class CountingIds implements CatalogIdGenerator {
        private int calls;
        @Override public CatalogItemId nextItemId() { calls++; return new CatalogItemId(new UUID(0, 10)); }
        @Override public CatalogDetailId nextDetailId() { calls++; return new CatalogDetailId(new UUID(0, 11)); }
        @Override public PriceListId nextPriceListId() { calls++; return new PriceListId(new UUID(0, 20)); }
        @Override public PriceEntryId nextPriceEntryId() { calls++; return new PriceEntryId(new UUID(0, 21)); }
        @Override public CategoryId nextCategoryId() { calls++; return new CategoryId(new UUID(0, 31)); }
        @Override public BrandId nextBrandId() { calls++; return new BrandId(new UUID(0, 32)); }
        @Override public TagId nextTagId() { calls++; return new TagId(new UUID(0, 33)); }
        @Override public TaxProfileId nextTaxProfileId() { calls++; return new TaxProfileId(new UUID(0, 34)); }
        @Override public VariantFamilyId nextVariantFamilyId() { calls++; return new VariantFamilyId(new UUID(0, 35)); }
    }
}
