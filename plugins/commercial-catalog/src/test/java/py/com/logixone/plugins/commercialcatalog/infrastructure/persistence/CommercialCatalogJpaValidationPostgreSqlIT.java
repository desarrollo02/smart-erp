package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemScope;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemType;
import py.com.logixone.plugins.commercialcatalog.api.CatalogTaxMode;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.api.PriceEntryId;
import py.com.logixone.plugins.commercialcatalog.api.PriceListId;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogPersistenceCode;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogPersistenceException;
import py.com.logixone.plugins.commercialcatalog.application.definition.CatalogDefinitions;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListState;
import py.com.logixone.plugins.commercialcatalog.domain.BrandId;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogClassification;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogDetailId;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItem;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemCode;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemIdentifier;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemName;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogVariant;
import py.com.logixone.plugins.commercialcatalog.domain.CategoryId;
import py.com.logixone.plugins.commercialcatalog.domain.ItemUnitConversion;
import py.com.logixone.plugins.commercialcatalog.domain.PriceEntry;
import py.com.logixone.plugins.commercialcatalog.domain.PriceList;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListCode;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListName;
import py.com.logixone.plugins.commercialcatalog.domain.TagId;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileId;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileReference;
import py.com.logixone.plugins.commercialcatalog.domain.UnitCode;
import py.com.logixone.plugins.commercialcatalog.domain.UnitPurpose;
import py.com.logixone.plugins.commercialcatalog.domain.VariantAttributeCode;
import py.com.logixone.plugins.commercialcatalog.domain.VariantAttributeValue;
import py.com.logixone.plugins.commercialcatalog.domain.VariantFamilyId;
import py.com.logixone.plugins.commercialcatalog.domain.VariantValueType;

@Testcontainers
class CommercialCatalogJpaValidationPostgreSqlIT {
    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse(
                    "postgres:18.4-bookworm@sha256:16fa100a3a6e92c0556632870455e7f8c6f3df5cefddd67d6b95292732bd7ff0")
            .asCompatibleSubstituteFor("postgres");
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("logixone_catalog_jpa_validation")
            .withUsername("logixone_test")
            .withPassword("test-only-password");
    private static EntityManagerFactory entityManagerFactory;

    @BeforeAll
    static void migrateAndValidate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(CommercialCatalogPersistenceNames.SCHEMA)
                .defaultSchema(CommercialCatalogPersistenceNames.SCHEMA)
                .locations("classpath:db/migration/commercial_catalog")
                .createSchemas(true)
                .cleanDisabled(true)
                .load()
                .migrate();
        entityManagerFactory = Persistence.createEntityManagerFactory(
                CommercialCatalogPersistenceNames.TEST_UNIT_NAME,
                Map.of(
                        "jakarta.persistence.jdbc.url", POSTGRES.getJdbcUrl(),
                        "jakarta.persistence.jdbc.user", POSTGRES.getUsername(),
                        "jakarta.persistence.jdbc.password", POSTGRES.getPassword(),
                        "jakarta.persistence.jdbc.driver", "org.postgresql.Driver"));
    }

    @AfterAll
    static void closeFactory() {
        if (entityManagerFactory != null) { entityManagerFactory.close(); }
    }

    @Test
    void validatesAllMappedTablesWithoutCreatingOrUpdatingDdl() {
        assertDoesNotThrow(() -> {
            var entityManager = entityManagerFactory.createEntityManager();
            entityManager.close();
        });
    }

    @Test
    void persistsAndRebuildsTheCompleteCatalogItemInsideItsCompanyBoundary() {
        Fixture fixture = fixture();
        inTransaction(entityManager -> {
            seedDefinitions(entityManager, fixture);
            return null;
        });
        CatalogItem original = completeItem(fixture);

        CatalogItem inserted = inTransaction(entityManager ->
                new JpaCatalogItemRepository(entityManager).insert(original));
        CatalogItem loaded = inTransaction(entityManager ->
                new JpaCatalogItemRepository(entityManager)
                        .findById(fixture.companyId(), fixture.itemId()).orElseThrow());
        boolean visibleFromOtherCompany = inTransaction(entityManager ->
                new JpaCatalogItemRepository(entityManager)
                        .findById(new CompanyId(UUID.randomUUID()), fixture.itemId()).isPresent());

        assertEquals(original.snapshot(), inserted.snapshot());
        assertEquals(original.snapshot(), loaded.snapshot());
        assertFalse(visibleFromOtherCompany);

        long persistedVersion = loaded.version();
        loaded.reviseIdentity(
                new CatalogItemCode("SKU-UPDATED"), new CatalogItemName("Updated item"),
                "Updated description", Set.of(CatalogItemScope.SALE), persistedVersion);
        CatalogItem updated = inTransaction(entityManager ->
                new JpaCatalogItemRepository(entityManager).update(loaded, persistedVersion));

        assertEquals(persistedVersion + 1, updated.version());
        assertEquals(Set.of(CatalogItemScope.SALE), updated.scopes());
        assertEquals("SKU-UPDATED", updated.code().value());
    }

    @Test
    void rejectsAStaleCatalogItemUpdateWithAStableApplicationCode() {
        Fixture fixture = fixture();
        inTransaction(entityManager -> {
            seedDefinitions(entityManager, fixture);
            new JpaCatalogItemRepository(entityManager).insert(completeItem(fixture));
            return null;
        });
        EntityManager firstManager = entityManagerFactory.createEntityManager();
        EntityManager staleManager = entityManagerFactory.createEntityManager();
        try {
            firstManager.getTransaction().begin();
            staleManager.getTransaction().begin();
            JpaCatalogItemRepository firstRepository = new JpaCatalogItemRepository(firstManager);
            JpaCatalogItemRepository staleRepository = new JpaCatalogItemRepository(staleManager);
            CatalogItem first = firstRepository.findById(fixture.companyId(), fixture.itemId()).orElseThrow();
            CatalogItem stale = staleRepository.findById(fixture.companyId(), fixture.itemId()).orElseThrow();
            long persistedVersion = first.version();
            first.reviseIdentity(
                    new CatalogItemCode("FIRST"), new CatalogItemName("First writer"), "",
                    Set.of(CatalogItemScope.SALE), persistedVersion);
            stale.reviseIdentity(
                    new CatalogItemCode("STALE"), new CatalogItemName("Stale writer"), "",
                    Set.of(CatalogItemScope.SALE), persistedVersion);
            firstRepository.update(first, persistedVersion);
            firstManager.getTransaction().commit();

            CatalogPersistenceException failure = assertThrows(
                    CatalogPersistenceException.class,
                    () -> staleRepository.update(stale, persistedVersion));
            assertEquals(CatalogPersistenceCode.VERSION_CONFLICT, failure.code());
            staleManager.getTransaction().rollback();
        } finally {
            rollbackIfActive(firstManager);
            rollbackIfActive(staleManager);
            firstManager.close();
            staleManager.close();
        }
    }

    @Test
    void persistsPriceEntriesAndPreservesTheirHistoryWhenTheyAreInactivated() {
        Fixture fixture = fixture();
        inTransaction(entityManager -> {
            seedDefinitions(entityManager, fixture);
            new JpaCatalogItemRepository(entityManager).insert(completeItem(fixture));
            return null;
        });
        PriceEntryId entryId = new PriceEntryId(UUID.randomUUID());
        PriceList list = PriceList.create(
                fixture.companyId(), new PriceListId(UUID.randomUUID()), new PriceListCode("RETAIL"),
                new PriceListName("Retail"), "PYG", CatalogTaxMode.TAX_INCLUDED,
                2, RoundingMode.HALF_UP);
        list.addEntry(PriceEntry.active(
                entryId, fixture.itemId(), new UnitCode("EA"), BigDecimal.ONE,
                new BigDecimal("15000"), Instant.parse("2026-01-01T00:00:00Z"), Optional.empty()), 0);

        PriceList inserted = inTransaction(entityManager ->
                new JpaPriceListRepository(entityManager).insert(list));
        PriceList loaded = inTransaction(entityManager ->
                new JpaPriceListRepository(entityManager)
                        .findById(fixture.companyId(), list.id()).orElseThrow());

        assertEquals(list.snapshot(), inserted.snapshot());
        assertEquals(list.snapshot(), loaded.snapshot());

        long persistedVersion = loaded.version();
        loaded.inactivateEntry(entryId, persistedVersion);
        PriceList updated = inTransaction(entityManager ->
                new JpaPriceListRepository(entityManager).update(loaded, persistedVersion));

        assertEquals(persistedVersion + 1, updated.version());
        assertFalse(updated.entries().get(entryId).active());
        assertEquals(1, updated.entries().size());
    }

    @Test
    void allocatesUniqueIncreasingNumbersConcurrentlyAndSeparatelyForEachCompany() throws Exception {
        CompanyId firstCompany = new CompanyId(UUID.randomUUID());
        CompanyId secondCompany = new CompanyId(UUID.randomUUID());
        try (var executor = Executors.newFixedThreadPool(8)) {
            var tasks = IntStream.range(0, 12)
                    .mapToObj(ignored -> (Callable<Long>) () -> inTransaction(entityManager ->
                            new JpaCatalogCodeSequenceRepository(entityManager).next(firstCompany, " item ")))
                    .toList();
            var values = executor.invokeAll(tasks).stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception failure) {
                    throw new IllegalStateException(failure);
                }
            }).sorted().toList();

            assertEquals(IntStream.rangeClosed(1, 12).mapToObj(Long::valueOf).toList(), values);
        }
        long otherCompanyFirst = inTransaction(entityManager ->
                new JpaCatalogCodeSequenceRepository(entityManager).next(secondCompany, "ITEM"));
        long nextForFirstCompany = inTransaction(entityManager ->
                new JpaCatalogCodeSequenceRepository(entityManager).next(firstCompany, "ITEM"));

        assertEquals(1, otherCompanyFirst);
        assertEquals(13, nextForFirstCompany);
    }

    @Test
    void persistsAndReadsEveryControlledDefinitionInsideItsCompany() {
        CompanyId companyId = new CompanyId(UUID.randomUUID());
        CategoryId categoryId = new CategoryId(UUID.randomUUID());
        BrandId brandId = new BrandId(UUID.randomUUID());
        TagId tagId = new TagId(UUID.randomUUID());
        TaxProfileId taxProfileId = new TaxProfileId(UUID.randomUUID());
        VariantFamilyId familyId = new VariantFamilyId(UUID.randomUUID());
        Instant validFrom = Instant.parse("2026-01-01T00:00:00Z");

        inTransaction(entityManager -> {
            JpaCatalogDefinitionRepository repository =
                    new JpaCatalogDefinitionRepository(entityManager);
            repository.insert(companyId, new CatalogDefinitions.Unit(
                    new UnitCode("EA"), "Each", 0,
                    CatalogDefinitions.State.ACTIVE, 0));
            repository.insert(companyId, new CatalogDefinitions.Category(
                    categoryId, Optional.empty(), "ROOT", "Root",
                    CatalogDefinitions.State.ACTIVE, 0));
            repository.insert(companyId, new CatalogDefinitions.Brand(
                    brandId, "BRAND", "Brand",
                    CatalogDefinitions.State.ACTIVE, 0));
            repository.insert(companyId, new CatalogDefinitions.Tag(
                    tagId, "TAG", "Tag", CatalogDefinitions.State.ACTIVE, 0));
            repository.insert(companyId, new CatalogDefinitions.TaxProfile(
                    taxProfileId, "STANDARD", "Standard", "STANDARD", "Standard tax",
                    validFrom, Optional.empty(), CatalogDefinitions.State.ACTIVE, 0));
            repository.insert(companyId, new CatalogDefinitions.VariantFamily(
                    familyId, "SIZE", "Size", List.of(new CatalogDefinitions.VariantAttribute(
                            new VariantAttributeCode("VALUE"), "Value",
                            VariantValueType.TEXT, true, 0)),
                    CatalogDefinitions.State.ACTIVE, 0));
            return null;
        });

        CatalogDefinitions.Snapshot snapshot = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager).findAll(companyId));
        CatalogDefinitions.Snapshot anotherCompany = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager)
                        .findAll(new CompanyId(UUID.randomUUID())));

        assertEquals(1, snapshot.units().size());
        assertEquals(1, snapshot.categories().size());
        assertEquals(1, snapshot.brands().size());
        assertEquals(1, snapshot.tags().size());
        assertEquals(validFrom, snapshot.taxProfiles().getFirst().validFrom());
        assertEquals(1, snapshot.variantFamilies().getFirst().attributes().size());
        assertTrue(anotherCompany.units().isEmpty());
        assertTrue(anotherCompany.taxProfiles().isEmpty());
    }

    @Test
    void resolvesTheCurrentVariantFamilyForAssignmentInsideItsCompanyBoundary() {
        CompanyId owner = new CompanyId(UUID.randomUUID());
        CompanyId anotherCompany = new CompanyId(UUID.randomUUID());
        CompanyId absentCompany = new CompanyId(UUID.randomUUID());
        VariantFamilyId familyId = new VariantFamilyId(UUID.randomUUID());
        List<CatalogDefinitions.VariantAttribute> ownerAttributes = List.of(
                new CatalogDefinitions.VariantAttribute(
                        new VariantAttributeCode("COLOR"), "Color",
                        VariantValueType.TEXT, true, 0),
                new CatalogDefinitions.VariantAttribute(
                        new VariantAttributeCode("NUMBER"), "Number",
                        VariantValueType.NUMBER, false, 1));

        inTransaction(entityManager -> {
            JpaCatalogDefinitionRepository repository =
                    new JpaCatalogDefinitionRepository(entityManager);
            repository.insert(owner, new CatalogDefinitions.VariantFamily(
                    familyId, "APPAREL", "Apparel", ownerAttributes,
                    CatalogDefinitions.State.ACTIVE, 0));
            repository.insert(anotherCompany, new CatalogDefinitions.VariantFamily(
                    familyId, "OTHER", "Other company", List.of(
                            new CatalogDefinitions.VariantAttribute(
                                    new VariantAttributeCode("OTHER"), "Other",
                                    VariantValueType.BOOLEAN, true, 0)),
                    CatalogDefinitions.State.INACTIVE, 0));
            return null;
        });

        Optional<CatalogDefinitions.VariantFamily> owned = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager)
                        .findCurrentForAssignment(owner, familyId));
        Optional<CatalogDefinitions.VariantFamily> foreign = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager)
                        .findCurrentForAssignment(anotherCompany, familyId));
        Optional<CatalogDefinitions.VariantFamily> absent = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager)
                        .findCurrentForAssignment(absentCompany, familyId));

        assertEquals("APPAREL", owned.orElseThrow().code());
        assertEquals(ownerAttributes, owned.orElseThrow().attributes());
        assertEquals(CatalogDefinitions.State.INACTIVE, foreign.orElseThrow().state());
        assertEquals("OTHER", foreign.orElseThrow().attributes().getFirst().code().value());
        assertTrue(absent.isEmpty());
    }

    @Test
    void changesVariantFamilyStateWithoutLosingAttributesAndEnforcesCompanyVersion() {
        CompanyId companyId = new CompanyId(UUID.randomUUID());
        CompanyId anotherCompanyId = new CompanyId(UUID.randomUUID());
        VariantFamilyId familyId = new VariantFamilyId(UUID.randomUUID());
        List<CatalogDefinitions.VariantAttribute> attributes = List.of(
                new CatalogDefinitions.VariantAttribute(
                        new VariantAttributeCode("COLOR"), "Color",
                        VariantValueType.TEXT, true, 0),
                new CatalogDefinitions.VariantAttribute(
                        new VariantAttributeCode("SIZE"), "Size",
                        VariantValueType.NUMBER, false, 1));

        inTransaction(entityManager -> {
            JpaCatalogDefinitionRepository repository =
                    new JpaCatalogDefinitionRepository(entityManager);
            repository.insert(companyId, new CatalogDefinitions.VariantFamily(
                    familyId, "APPAREL", "Apparel", attributes,
                    CatalogDefinitions.State.ACTIVE, 0));
            repository.insert(anotherCompanyId, new CatalogDefinitions.VariantFamily(
                    familyId, "APPAREL", "Other apparel", attributes,
                    CatalogDefinitions.State.ACTIVE, 0));
            return null;
        });

        CatalogDefinitions.VariantFamily inactive = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager).changeVariantFamilyState(
                        companyId, familyId, CatalogDefinitions.State.INACTIVE, 0));
        CatalogDefinitions.VariantFamily unchanged = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager).changeVariantFamilyState(
                        companyId, familyId, CatalogDefinitions.State.INACTIVE, 1));

        assertEquals(CatalogDefinitions.State.INACTIVE, inactive.state());
        assertEquals(1, inactive.version());
        assertEquals(attributes, inactive.attributes());
        assertEquals(1, unchanged.version());
        assertEquals(attributes, unchanged.attributes());

        CatalogPersistenceException stale = assertThrows(
                CatalogPersistenceException.class,
                () -> inTransaction(entityManager ->
                        new JpaCatalogDefinitionRepository(entityManager)
                                .changeVariantFamilyState(
                                        companyId, familyId,
                                        CatalogDefinitions.State.ACTIVE, 0)));
        assertEquals(CatalogPersistenceCode.VERSION_CONFLICT, stale.code());

        CatalogDefinitions.Snapshot owner = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager).findAll(companyId));
        CatalogDefinitions.Snapshot other = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager).findAll(anotherCompanyId));
        assertEquals(CatalogDefinitions.State.INACTIVE,
                owner.variantFamilies().getFirst().state());
        assertEquals(1, owner.variantFamilies().getFirst().version());
        assertEquals(CatalogDefinitions.State.ACTIVE,
                other.variantFamilies().getFirst().state());
        assertEquals(0, other.variantFamilies().getFirst().version());
    }

    @Test
    void appendsCompleteVariantFamilyRevisionsAndKeepsOnlyTheLatestCurrent() {
        CompanyId companyId = new CompanyId(UUID.randomUUID());
        VariantFamilyId familyId = new VariantFamilyId(UUID.randomUUID());
        List<CatalogDefinitions.VariantAttribute> initial = List.of(
                new CatalogDefinitions.VariantAttribute(
                        new VariantAttributeCode("COLOR"), "Color",
                        VariantValueType.TEXT, true, 0));
        List<CatalogDefinitions.VariantAttribute> revisedAttributes = List.of(
                new CatalogDefinitions.VariantAttribute(
                        new VariantAttributeCode("SIZE"), "Size",
                        VariantValueType.NUMBER, true, 0),
                new CatalogDefinitions.VariantAttribute(
                        new VariantAttributeCode("SEASON"), "Season",
                        VariantValueType.TEXT, false, 1));

        inTransaction(entityManager -> {
            new JpaCatalogDefinitionRepository(entityManager).insert(
                    companyId, new CatalogDefinitions.VariantFamily(
                            familyId, "APPAREL", "Apparel", initial,
                            CatalogDefinitions.State.ACTIVE, 0));
            return null;
        });
        inTransaction(entityManager -> {
            new JpaCatalogDefinitionRepository(entityManager).changeVariantFamilyState(
                    companyId, familyId, CatalogDefinitions.State.INACTIVE, 0);
            return null;
        });
        CatalogDefinitions.VariantFamily revised = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager).reviseVariantFamily(
                        companyId, familyId, "Apparel by size", revisedAttributes, 1));
        List<CatalogDefinitions.VariantFamilyRevision> history = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager)
                        .variantFamilyHistory(companyId, familyId));

        assertEquals("APPAREL", revised.code());
        assertEquals("Apparel by size", revised.displayName());
        assertEquals(CatalogDefinitions.State.INACTIVE, revised.state());
        assertEquals(2, revised.version());
        assertEquals(revisedAttributes, revised.attributes());
        assertEquals(List.of(2L, 1L, 0L), history.stream()
                .map(CatalogDefinitions.VariantFamilyRevision::version).toList());
        assertEquals(List.of(true, false, false), history.stream()
                .map(CatalogDefinitions.VariantFamilyRevision::current).toList());
        assertEquals(revisedAttributes, history.getFirst().attributes());
        assertEquals(initial, history.get(1).attributes());
        assertEquals(CatalogDefinitions.State.INACTIVE, history.get(1).state());
        assertEquals(CatalogDefinitions.State.ACTIVE, history.get(2).state());

        CatalogPersistenceException stale = assertThrows(
                CatalogPersistenceException.class,
                () -> inTransaction(entityManager ->
                        new JpaCatalogDefinitionRepository(entityManager).reviseVariantFamily(
                                companyId, familyId, "Stale", initial, 1)));
        assertEquals(CatalogPersistenceCode.VERSION_CONFLICT, stale.code());
    }

    @Test
    void changesSimpleDefinitionStatesWithCompanyAndVersionBoundaries() {
        CompanyId companyId = new CompanyId(UUID.randomUUID());
        CompanyId anotherCompanyId = new CompanyId(UUID.randomUUID());
        CategoryId categoryId = new CategoryId(UUID.randomUUID());
        BrandId brandId = new BrandId(UUID.randomUUID());
        TagId tagId = new TagId(UUID.randomUUID());

        inTransaction(entityManager -> {
            JpaCatalogDefinitionRepository repository =
                    new JpaCatalogDefinitionRepository(entityManager);
            repository.insert(companyId, new CatalogDefinitions.Unit(
                    new UnitCode("EA"), "Each", 0,
                    CatalogDefinitions.State.ACTIVE, 0));
            repository.insert(companyId, new CatalogDefinitions.Category(
                    categoryId, Optional.empty(), "ROOT", "Root",
                    CatalogDefinitions.State.ACTIVE, 0));
            repository.insert(companyId, new CatalogDefinitions.Brand(
                    brandId, "BRAND", "Brand",
                    CatalogDefinitions.State.ACTIVE, 0));
            repository.insert(companyId, new CatalogDefinitions.Tag(
                    tagId, "TAG", "Tag",
                    CatalogDefinitions.State.ACTIVE, 0));
            repository.insert(anotherCompanyId, new CatalogDefinitions.Unit(
                    new UnitCode("EA"), "Other each", 0,
                    CatalogDefinitions.State.ACTIVE, 0));
            return null;
        });

        Map<CatalogDefinitions.SimpleKind, String> identities = Map.of(
                CatalogDefinitions.SimpleKind.UNIT, "EA",
                CatalogDefinitions.SimpleKind.CATEGORY, categoryId.value().toString(),
                CatalogDefinitions.SimpleKind.BRAND, brandId.value().toString(),
                CatalogDefinitions.SimpleKind.TAG, tagId.value().toString());
        identities.forEach((kind, identity) -> {
            CatalogDefinitions.Lifecycle changed = inTransaction(entityManager ->
                    new JpaCatalogDefinitionRepository(entityManager).changeSimpleState(
                            companyId, kind, identity,
                            CatalogDefinitions.State.INACTIVE, 0));
            assertTrue(changed.changed());
            assertEquals(1, changed.version());
            assertEquals(CatalogDefinitions.State.INACTIVE, changed.state());
        });

        CatalogPersistenceException stale = assertThrows(
                CatalogPersistenceException.class,
                () -> inTransaction(entityManager ->
                        new JpaCatalogDefinitionRepository(entityManager).changeSimpleState(
                                companyId, CatalogDefinitions.SimpleKind.UNIT, "EA",
                                CatalogDefinitions.State.ACTIVE, 0)));
        assertEquals(CatalogPersistenceCode.VERSION_CONFLICT, stale.code());

        CatalogDefinitions.Snapshot changed = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager).findAll(companyId));
        CatalogDefinitions.Snapshot untouched = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager).findAll(anotherCompanyId));
        assertTrue(changed.units().stream().allMatch(value ->
                value.state() == CatalogDefinitions.State.INACTIVE && value.version() == 1));
        assertTrue(changed.categories().stream().allMatch(value ->
                value.state() == CatalogDefinitions.State.INACTIVE && value.version() == 1));
        assertTrue(changed.brands().stream().allMatch(value ->
                value.state() == CatalogDefinitions.State.INACTIVE && value.version() == 1));
        assertTrue(changed.tags().stream().allMatch(value ->
                value.state() == CatalogDefinitions.State.INACTIVE && value.version() == 1));
        assertEquals(CatalogDefinitions.State.ACTIVE, untouched.units().getFirst().state());
        assertEquals(0, untouched.units().getFirst().version());
    }

    @Test
    void revisesSimpleDefinitionsAndKeepsAppendOnlyHistoryInsideTheCompany() {
        CompanyId companyId = new CompanyId(UUID.randomUUID());
        CompanyId anotherCompanyId = new CompanyId(UUID.randomUUID());
        CategoryId parentId = new CategoryId(UUID.randomUUID());
        CategoryId categoryId = new CategoryId(UUID.randomUUID());
        BrandId brandId = new BrandId(UUID.randomUUID());
        TagId tagId = new TagId(UUID.randomUUID());

        inTransaction(entityManager -> {
            JpaCatalogDefinitionRepository repository =
                    new JpaCatalogDefinitionRepository(entityManager);
            repository.insert(companyId, new CatalogDefinitions.Unit(
                    new UnitCode("EA"), "Each", 0,
                    CatalogDefinitions.State.ACTIVE, 0));
            repository.insert(companyId, new CatalogDefinitions.Category(
                    parentId, Optional.empty(), "ROOT", "Root",
                    CatalogDefinitions.State.ACTIVE, 0));
            repository.insert(companyId, new CatalogDefinitions.Category(
                    categoryId, Optional.empty(), "DRINKS", "Drinks",
                    CatalogDefinitions.State.ACTIVE, 0));
            repository.insert(companyId, new CatalogDefinitions.Brand(
                    brandId, "BRAND", "Brand",
                    CatalogDefinitions.State.ACTIVE, 0));
            repository.insert(companyId, new CatalogDefinitions.Tag(
                    tagId, "TAG", "Tag",
                    CatalogDefinitions.State.ACTIVE, 0));
            repository.insert(anotherCompanyId, new CatalogDefinitions.Unit(
                    new UnitCode("EA"), "Other each", 0,
                    CatalogDefinitions.State.ACTIVE, 0));
            return null;
        });

        inTransaction(entityManager -> {
            JpaCatalogDefinitionRepository repository =
                    new JpaCatalogDefinitionRepository(entityManager);
            repository.reviseSimpleDefinition(
                    companyId, CatalogDefinitions.SimpleKind.UNIT, "EA",
                    "Each with decimals", Optional.of(3), Optional.empty(), 0);
            repository.reviseSimpleDefinition(
                    companyId, CatalogDefinitions.SimpleKind.CATEGORY,
                    categoryId.value().toString(), "Cold drinks",
                    Optional.empty(), Optional.of(parentId), 0);
            repository.reviseSimpleDefinition(
                    companyId, CatalogDefinitions.SimpleKind.BRAND,
                    brandId.value().toString(), "Preferred brand",
                    Optional.empty(), Optional.empty(), 0);
            repository.reviseSimpleDefinition(
                    companyId, CatalogDefinitions.SimpleKind.TAG,
                    tagId.value().toString(), "Seasonal tag",
                    Optional.empty(), Optional.empty(), 0);
            repository.changeSimpleState(
                    companyId, CatalogDefinitions.SimpleKind.UNIT, "EA",
                    CatalogDefinitions.State.INACTIVE, 1);
            return null;
        });

        CatalogDefinitions.Snapshot current = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager).findAll(companyId));
        List<CatalogDefinitions.SimpleRevision> unitHistory = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager).simpleDefinitionHistory(
                        companyId, CatalogDefinitions.SimpleKind.UNIT, "EA"));
        List<CatalogDefinitions.SimpleRevision> categoryHistory = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager).simpleDefinitionHistory(
                        companyId, CatalogDefinitions.SimpleKind.CATEGORY,
                        categoryId.value().toString()));
        List<CatalogDefinitions.SimpleRevision> brandHistory = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager).simpleDefinitionHistory(
                        companyId, CatalogDefinitions.SimpleKind.BRAND,
                        brandId.value().toString()));
        List<CatalogDefinitions.SimpleRevision> tagHistory = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager).simpleDefinitionHistory(
                        companyId, CatalogDefinitions.SimpleKind.TAG,
                        tagId.value().toString()));
        List<CatalogDefinitions.SimpleRevision> isolatedHistory = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager).simpleDefinitionHistory(
                        anotherCompanyId, CatalogDefinitions.SimpleKind.UNIT, "EA"));

        assertEquals("Each with decimals", current.units().getFirst().displayName());
        assertEquals(3, current.units().getFirst().decimalScale());
        assertEquals(CatalogDefinitions.State.INACTIVE, current.units().getFirst().state());
        assertEquals(2, current.units().getFirst().version());
        assertEquals(Optional.of(parentId), current.categories().stream()
                .filter(category -> category.id().equals(categoryId))
                .findFirst().orElseThrow().parentId());
        assertEquals("Preferred brand", current.brands().getFirst().displayName());
        assertEquals("Seasonal tag", current.tags().getFirst().displayName());

        assertEquals(List.of(2L, 1L, 0L),
                unitHistory.stream().map(CatalogDefinitions.SimpleRevision::version).toList());
        assertTrue(unitHistory.getFirst().current());
        assertEquals(CatalogDefinitions.State.INACTIVE, unitHistory.getFirst().state());
        assertEquals(Optional.of(3), unitHistory.getFirst().decimalScale());
        assertFalse(unitHistory.get(1).current());
        assertEquals(CatalogDefinitions.State.ACTIVE, unitHistory.get(1).state());
        assertEquals("Each", unitHistory.get(2).displayName());
        assertEquals(Optional.of(parentId), categoryHistory.getFirst().parentId());
        assertEquals("Drinks", categoryHistory.get(1).displayName());
        assertEquals("Preferred brand", brandHistory.getFirst().displayName());
        assertEquals("Seasonal tag", tagHistory.getFirst().displayName());
        assertEquals(List.of("Other each"), isolatedHistory.stream()
                .map(CatalogDefinitions.SimpleRevision::displayName).toList());

        CatalogPersistenceException stale = assertThrows(
                CatalogPersistenceException.class,
                () -> inTransaction(entityManager ->
                        new JpaCatalogDefinitionRepository(entityManager)
                                .reviseSimpleDefinition(
                                        companyId, CatalogDefinitions.SimpleKind.UNIT, "EA",
                                        "Stale", Optional.of(2), Optional.empty(), 1)));
        CatalogPersistenceException wrongCompany = assertThrows(
                CatalogPersistenceException.class,
                () -> inTransaction(entityManager ->
                        new JpaCatalogDefinitionRepository(entityManager)
                                .simpleDefinitionHistory(
                                        anotherCompanyId, CatalogDefinitions.SimpleKind.BRAND,
                                        brandId.value().toString())));
        assertEquals(CatalogPersistenceCode.VERSION_CONFLICT, stale.code());
        assertEquals(CatalogPersistenceCode.DEFINITION_NOT_FOUND, wrongCompany.code());
    }

    @Test
    void replacesEverySimpleDefinitionWithoutReassigningThePreviousIdentity() {
        CompanyId companyId = new CompanyId(UUID.randomUUID());
        CategoryId parentId = new CategoryId(UUID.randomUUID());
        CategoryId categoryId = new CategoryId(UUID.randomUUID());
        BrandId brandId = new BrandId(UUID.randomUUID());
        TagId tagId = new TagId(UUID.randomUUID());
        CategoryId replacementCategoryId = new CategoryId(UUID.randomUUID());
        BrandId replacementBrandId = new BrandId(UUID.randomUUID());
        TagId replacementTagId = new TagId(UUID.randomUUID());

        inTransaction(entityManager -> {
            JpaCatalogDefinitionRepository repository =
                    new JpaCatalogDefinitionRepository(entityManager);
            repository.insert(companyId, new CatalogDefinitions.Unit(
                    new UnitCode("EA"), "Each", 0,
                    CatalogDefinitions.State.ACTIVE, 0));
            repository.insert(companyId, new CatalogDefinitions.Category(
                    parentId, Optional.empty(), "ROOT", "Root",
                    CatalogDefinitions.State.ACTIVE, 0));
            repository.insert(companyId, new CatalogDefinitions.Category(
                    categoryId, Optional.empty(), "OLD-CAT", "Old category",
                    CatalogDefinitions.State.ACTIVE, 0));
            repository.insert(companyId, new CatalogDefinitions.Brand(
                    brandId, "OLD-BRAND", "Old brand",
                    CatalogDefinitions.State.ACTIVE, 0));
            repository.insert(companyId, new CatalogDefinitions.Tag(
                    tagId, "OLD-TAG", "Old tag",
                    CatalogDefinitions.State.ACTIVE, 0));
            return null;
        });

        List<CatalogDefinitions.Replacement> results = inTransaction(entityManager -> {
            JpaCatalogDefinitionRepository repository =
                    new JpaCatalogDefinitionRepository(entityManager);
            return List.of(
                    repository.replaceSimpleDefinition(
                            companyId, CatalogDefinitions.SimpleKind.UNIT, "EA",
                            new CatalogDefinitions.ReplacementCandidate(
                                    CatalogDefinitions.SimpleKind.UNIT,
                                    "EA2", "EA2", "Replacement each",
                                    Optional.of(2), Optional.empty()), 0),
                    repository.replaceSimpleDefinition(
                            companyId, CatalogDefinitions.SimpleKind.CATEGORY,
                            categoryId.value().toString(),
                            new CatalogDefinitions.ReplacementCandidate(
                                    CatalogDefinitions.SimpleKind.CATEGORY,
                                    replacementCategoryId.value().toString(),
                                    "NEW-CAT", "New category",
                                    Optional.empty(), Optional.of(parentId)), 0),
                    repository.replaceSimpleDefinition(
                            companyId, CatalogDefinitions.SimpleKind.BRAND,
                            brandId.value().toString(),
                            new CatalogDefinitions.ReplacementCandidate(
                                    CatalogDefinitions.SimpleKind.BRAND,
                                    replacementBrandId.value().toString(),
                                    "NEW-BRAND", "New brand",
                                    Optional.empty(), Optional.empty()), 0),
                    repository.replaceSimpleDefinition(
                            companyId, CatalogDefinitions.SimpleKind.TAG,
                            tagId.value().toString(),
                            new CatalogDefinitions.ReplacementCandidate(
                                    CatalogDefinitions.SimpleKind.TAG,
                                    replacementTagId.value().toString(),
                                    "NEW-TAG", "New tag",
                                    Optional.empty(), Optional.empty()), 0));
        });

        CatalogDefinitions.Snapshot snapshot = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager).findAll(companyId));
        List<CatalogDefinitions.SimpleRevision> oldUnitHistory = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager).simpleDefinitionHistory(
                        companyId, CatalogDefinitions.SimpleKind.UNIT, "EA"));
        List<CatalogDefinitions.SimpleRevision> newUnitHistory = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager).simpleDefinitionHistory(
                        companyId, CatalogDefinitions.SimpleKind.UNIT, "EA2"));

        assertTrue(results.stream().allMatch(result -> result.previousVersion() == 1));
        assertTrue(results.stream().allMatch(result -> result.replacementVersion() == 0));
        assertEquals(4, snapshot.replacements().size());
        assertEquals(CatalogDefinitions.State.INACTIVE, snapshot.units().stream()
                .filter(unit -> unit.code().equals(new UnitCode("EA")))
                .findFirst().orElseThrow().state());
        assertEquals(CatalogDefinitions.State.ACTIVE, snapshot.units().stream()
                .filter(unit -> unit.code().equals(new UnitCode("EA2")))
                .findFirst().orElseThrow().state());
        assertTrue(snapshot.categories().stream()
                .filter(category -> category.id().equals(categoryId))
                .allMatch(category -> category.state() == CatalogDefinitions.State.INACTIVE));
        assertEquals(Optional.of(parentId), snapshot.categories().stream()
                .filter(category -> category.id().equals(replacementCategoryId))
                .findFirst().orElseThrow().parentId());
        assertEquals(List.of(1L, 0L), oldUnitHistory.stream()
                .map(CatalogDefinitions.SimpleRevision::version).toList());
        assertEquals(CatalogDefinitions.State.INACTIVE, oldUnitHistory.getFirst().state());
        assertEquals(List.of(0L), newUnitHistory.stream()
                .map(CatalogDefinitions.SimpleRevision::version).toList());

        IllegalStateException repeated = assertThrows(
                IllegalStateException.class,
                () -> inTransaction(entityManager ->
                        new JpaCatalogDefinitionRepository(entityManager)
                                .replaceSimpleDefinition(
                                        companyId, CatalogDefinitions.SimpleKind.UNIT, "EA",
                                        new CatalogDefinitions.ReplacementCandidate(
                                                CatalogDefinitions.SimpleKind.UNIT,
                                                "EA3", "EA3", "Another each",
                                                Optional.of(0), Optional.empty()), 1)));
        assertTrue(repeated.getMessage().contains("active definition"));

        IllegalStateException reactivation = assertThrows(
                IllegalStateException.class,
                () -> inTransaction(entityManager ->
                        new JpaCatalogDefinitionRepository(entityManager).changeSimpleState(
                                companyId, CatalogDefinitions.SimpleKind.UNIT, "EA",
                                CatalogDefinitions.State.ACTIVE, 1)));
        assertTrue(reactivation.getMessage().contains("cannot be reactivated"));

        IllegalStateException revision = assertThrows(
                IllegalStateException.class,
                () -> inTransaction(entityManager ->
                        new JpaCatalogDefinitionRepository(entityManager)
                                .reviseSimpleDefinition(
                                        companyId, CatalogDefinitions.SimpleKind.UNIT, "EA",
                                        "Changed historical name", Optional.of(2),
                                        Optional.empty(), 1)));
        assertTrue(revision.getMessage().contains("cannot be revised"));
    }

    @Test
    void changesTaxProfileStateWithRevisionHistoryAndCompanyBoundaries() {
        CompanyId companyId = new CompanyId(UUID.randomUUID());
        CompanyId anotherCompanyId = new CompanyId(UUID.randomUUID());
        TaxProfileId id = new TaxProfileId(UUID.randomUUID());
        Instant validFrom = Instant.parse("2026-01-01T00:00:00Z");

        inTransaction(entityManager -> {
            JpaCatalogDefinitionRepository repository =
                    new JpaCatalogDefinitionRepository(entityManager);
            repository.insert(companyId, new CatalogDefinitions.TaxProfile(
                    id, "IVA10", "IVA diez", "TAXED", "Perfil de prueba",
                    validFrom, Optional.empty(), CatalogDefinitions.State.ACTIVE, 0));
            return null;
        });

        CatalogDefinitions.TaxProfile inactive = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager).changeTaxProfileState(
                        companyId, id, CatalogDefinitions.State.INACTIVE, 0));
        CatalogDefinitions.TaxProfile active = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager).changeTaxProfileState(
                        companyId, id, CatalogDefinitions.State.ACTIVE, 1));

        assertEquals(CatalogDefinitions.State.INACTIVE, inactive.state());
        assertEquals(1, inactive.version());
        assertEquals(CatalogDefinitions.State.ACTIVE, active.state());
        assertEquals(2, active.version());
        assertEquals(validFrom, active.validFrom());

        CatalogPersistenceException stale = assertThrows(
                CatalogPersistenceException.class,
                () -> inTransaction(entityManager ->
                        new JpaCatalogDefinitionRepository(entityManager).changeTaxProfileState(
                                companyId, id, CatalogDefinitions.State.INACTIVE, 1)));
        CatalogPersistenceException wrongCompany = assertThrows(
                CatalogPersistenceException.class,
                () -> inTransaction(entityManager ->
                        new JpaCatalogDefinitionRepository(entityManager).changeTaxProfileState(
                                anotherCompanyId, id, CatalogDefinitions.State.INACTIVE, 2)));

        assertEquals(CatalogPersistenceCode.VERSION_CONFLICT, stale.code());
        assertEquals(CatalogPersistenceCode.DEFINITION_NOT_FOUND, wrongCompany.code());
        long revisions = inTransaction(entityManager -> ((Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM plg_commercial_catalog.tax_profile_revision "
                                + "WHERE company_id = :company AND tax_profile_id = :profile")
                .setParameter("company", companyId.value())
                .setParameter("profile", id.value())
                .getSingleResult()).longValue());
        assertEquals(3L, revisions);
    }

    @Test
    void createsAnExplicitTaxProfileRevisionWithOptimisticCompanyBoundaries() {
        CompanyId companyId = new CompanyId(UUID.randomUUID());
        CompanyId anotherCompanyId = new CompanyId(UUID.randomUUID());
        TaxProfileId id = new TaxProfileId(UUID.randomUUID());
        Instant originalFrom = Instant.parse("2026-01-01T00:00:00Z");
        Instant revisedFrom = Instant.parse("2026-08-01T00:00:00Z");

        inTransaction(entityManager -> {
            new JpaCatalogDefinitionRepository(entityManager).insert(
                    companyId, new CatalogDefinitions.TaxProfile(
                            id, "IVA10", "IVA diez", "TAXED_STANDARD", "Original",
                            originalFrom, Optional.empty(), CatalogDefinitions.State.ACTIVE, 0));
            return null;
        });

        CatalogDefinitions.TaxProfile revised = inTransaction(entityManager ->
                new JpaCatalogDefinitionRepository(entityManager).reviseTaxProfile(
                        companyId, id, "TAXED_REDUCED", "Nueva revisión", revisedFrom,
                        Optional.of(Instant.parse("2027-01-01T00:00:00Z")), 0));

        assertEquals(1, revised.version());
        assertEquals("IVA10", revised.code());
        assertEquals("IVA diez", revised.displayName());
        assertEquals("TAXED_REDUCED", revised.internalKindCode());
        assertEquals(revisedFrom, revised.validFrom());

        CatalogPersistenceException stale = assertThrows(
                CatalogPersistenceException.class,
                () -> inTransaction(entityManager ->
                        new JpaCatalogDefinitionRepository(entityManager).reviseTaxProfile(
                                companyId, id, "TAXED_STANDARD", "Obsoleto", originalFrom,
                                Optional.empty(), 0)));
        CatalogPersistenceException wrongCompany = assertThrows(
                CatalogPersistenceException.class,
                () -> inTransaction(entityManager ->
                        new JpaCatalogDefinitionRepository(entityManager).reviseTaxProfile(
                                anotherCompanyId, id, "TAXED_STANDARD", "Otra empresa",
                                originalFrom, Optional.empty(), 1)));

        assertEquals(CatalogPersistenceCode.VERSION_CONFLICT, stale.code());
        assertEquals(CatalogPersistenceCode.DEFINITION_NOT_FOUND, wrongCompany.code());
        List<?> history = inTransaction(entityManager -> entityManager.createNativeQuery(
                        "SELECT profile_version, internal_kind_code, active "
                                + "FROM plg_commercial_catalog.tax_profile_revision "
                                + "WHERE company_id = :company AND tax_profile_id = :profile "
                                + "ORDER BY profile_version")
                .setParameter("company", companyId.value())
                .setParameter("profile", id.value())
                .getResultList());
        assertEquals(2, history.size());
        Object[] originalRevision = (Object[]) history.get(0);
        Object[] currentRevision = (Object[]) history.get(1);
        assertEquals("TAXED_STANDARD", originalRevision[1]);
        assertEquals(Boolean.FALSE, originalRevision[2]);
        assertEquals("TAXED_REDUCED", currentRevision[1]);
        assertEquals(Boolean.TRUE, currentRevision[2]);

        List<CatalogDefinitions.TaxProfileRevision> projectedHistory = inTransaction(
                entityManager -> new JpaCatalogDefinitionRepository(entityManager)
                        .taxProfileHistory(companyId, id));
        assertEquals(List.of(1L, 0L), projectedHistory.stream()
                .map(CatalogDefinitions.TaxProfileRevision::version).toList());
        assertTrue(projectedHistory.getFirst().current());
        assertFalse(projectedHistory.getLast().current());
        assertEquals("Nueva revisión", projectedHistory.getFirst().description());
        CatalogPersistenceException hiddenFromOtherCompany = assertThrows(
                CatalogPersistenceException.class,
                () -> inTransaction(entityManager ->
                        new JpaCatalogDefinitionRepository(entityManager)
                                .taxProfileHistory(anotherCompanyId, id)));
        assertEquals(CatalogPersistenceCode.DEFINITION_NOT_FOUND,
                hiddenFromOtherCompany.code());
    }

    @Test
    void searchesCatalogIdentifiersAndPriceListSummariesWithCompanyFilters() {
        Fixture fixture = fixture();
        PriceListId priceListId = new PriceListId(UUID.randomUUID());
        inTransaction(entityManager -> {
            seedDefinitions(entityManager, fixture);
            new JpaCatalogItemRepository(entityManager).insert(completeItem(fixture));
            PriceList list = PriceList.create(
                    fixture.companyId(), priceListId, new PriceListCode("RETAIL"),
                    new PriceListName("Retail price"), "PYG", CatalogTaxMode.TAX_INCLUDED,
                    0, RoundingMode.HALF_UP);
            list.addEntry(PriceEntry.active(
                    new PriceEntryId(UUID.randomUUID()), fixture.itemId(), new UnitCode("EA"),
                    BigDecimal.ONE, new BigDecimal("15000"),
                    Instant.parse("2026-01-01T00:00:00Z"), Optional.empty()), 0);
            new JpaPriceListRepository(entityManager).insert(list);
            return null;
        });

        var itemPage = inTransaction(entityManager ->
                new JpaCatalogItemRepository(entityManager).search(
                        fixture.companyId(), new CatalogSearchCriteria(
                                "784001", Set.of(CatalogItemType.PRODUCT),
                                Set.of(CatalogItemState.ACTIVE), 0, 20)));
        var pricePage = inTransaction(entityManager ->
                new JpaPriceListRepository(entityManager).search(
                        fixture.companyId(), new PriceListSearchCriteria(
                                "retail", Set.of(PriceListState.ACTIVE), 0, 20)));
        var otherCompany = inTransaction(entityManager ->
                new JpaCatalogItemRepository(entityManager).search(
                        new CompanyId(UUID.randomUUID()), new CatalogSearchCriteria(
                                "", Set.of(), Set.of(), 0, 20)));

        assertEquals(1, itemPage.total());
        assertEquals(fixture.itemId(), itemPage.items().getFirst().id());
        assertEquals(Set.of(CatalogItemScope.PURCHASE, CatalogItemScope.SALE),
                itemPage.items().getFirst().scopes());
        assertEquals(1, pricePage.total());
        assertEquals(1, pricePage.items().getFirst().entries());
        assertEquals(1, pricePage.items().getFirst().activeEntries());
        assertEquals(0, otherCompany.total());
    }

    private static CatalogItem completeItem(Fixture fixture) {
        CatalogItem item = CatalogItem.create(
                fixture.companyId(), fixture.itemId(), new CatalogItemCode("SKU-1"),
                new CatalogItemName("Blue shirt"), "Cotton", CatalogItemType.PRODUCT,
                Set.of(CatalogItemScope.PURCHASE, CatalogItemScope.SALE), new UnitCode("EA"),
                new TaxProfileReference(new TaxProfileId(fixture.taxProfileId()), 0));
        item.addIdentifier(CatalogItemIdentifier.active(
                new CatalogDetailId(UUID.randomUUID()), "EAN", "784 001"), 0);
        item.addUnitConversion(new ItemUnitConversion(
                new UnitCode("BOX"), new BigDecimal("12"), Set.of(UnitPurpose.SALE),
                Set.of(UnitPurpose.SALE), true), 1);
        item.classify(new CatalogClassification(
                new CategoryId(fixture.mainCategoryId()), Set.of(new CategoryId(fixture.secondaryCategoryId())),
                Optional.of(new BrandId(fixture.brandId())), Set.of(new TagId(fixture.tagId()))), 2);
        item.assignVariant(new CatalogVariant(
                new VariantFamilyId(fixture.variantFamilyId()),
                0,
                Map.of(new VariantAttributeCode("COLOR"),
                        new VariantAttributeValue(VariantValueType.TEXT, "Blue"))), 3);
        return item;
    }

    private static Fixture fixture() {
        return new Fixture(
                new CompanyId(UUID.randomUUID()), new CatalogItemId(UUID.randomUUID()),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID());
    }

    private static void seedDefinitions(EntityManager entityManager, Fixture fixture) {
        insert(entityManager,
                "INSERT INTO plg_commercial_catalog.unit_definition "
                        + "(company_id, unit_code, display_name, decimal_scale, state) VALUES (?1, ?2, ?3, ?4, 'ACTIVE')",
                fixture.companyId().value(), "EA", "Each", 0);
        insert(entityManager,
                "INSERT INTO plg_commercial_catalog.unit_definition "
                        + "(company_id, unit_code, display_name, decimal_scale, state) VALUES (?1, ?2, ?3, ?4, 'ACTIVE')",
                fixture.companyId().value(), "BOX", "Box", 0);
        insert(entityManager,
                "INSERT INTO plg_commercial_catalog.tax_profile "
                        + "(company_id, tax_profile_id, code, display_name, state) VALUES (?1, ?2, 'STANDARD', 'Standard', 'ACTIVE')",
                fixture.companyId().value(), fixture.taxProfileId());
        insert(entityManager,
                "INSERT INTO plg_commercial_catalog.tax_profile_revision "
                        + "(company_id, tax_profile_id, profile_version, internal_kind_code, description, valid_from, active) "
                        + "VALUES (?1, ?2, 0, 'STANDARD', 'Standard', TIMESTAMPTZ '2026-01-01 00:00:00Z', TRUE)",
                fixture.companyId().value(), fixture.taxProfileId());
        insert(entityManager,
                "INSERT INTO plg_commercial_catalog.category_definition "
                        + "(company_id, category_id, code, display_name, state) VALUES (?1, ?2, 'MAIN', 'Main', 'ACTIVE')",
                fixture.companyId().value(), fixture.mainCategoryId());
        insert(entityManager,
                "INSERT INTO plg_commercial_catalog.category_definition "
                        + "(company_id, category_id, code, display_name, state) VALUES (?1, ?2, 'SECONDARY', 'Secondary', 'ACTIVE')",
                fixture.companyId().value(), fixture.secondaryCategoryId());
        insert(entityManager,
                "INSERT INTO plg_commercial_catalog.brand_definition "
                        + "(company_id, brand_id, code, display_name, state) VALUES (?1, ?2, 'BRAND', 'Brand', 'ACTIVE')",
                fixture.companyId().value(), fixture.brandId());
        insert(entityManager,
                "INSERT INTO plg_commercial_catalog.tag_definition "
                        + "(company_id, tag_id, code, display_name, state) VALUES (?1, ?2, 'TAG', 'Tag', 'ACTIVE')",
                fixture.companyId().value(), fixture.tagId());
        insert(entityManager,
                "INSERT INTO plg_commercial_catalog.variant_family "
                        + "(company_id, variant_family_id, code, display_name, state) VALUES (?1, ?2, 'CLOTHING', 'Clothing', 'ACTIVE')",
                fixture.companyId().value(), fixture.variantFamilyId());
        insert(entityManager,
                "INSERT INTO plg_commercial_catalog.variant_attribute_definition "
                        + "(company_id, variant_family_id, attribute_code, display_name, value_type, required, position) "
                        + "VALUES (?1, ?2, 'COLOR', 'Color', 'TEXT', TRUE, 0)",
                fixture.companyId().value(), fixture.variantFamilyId());
        insert(entityManager,
                "INSERT INTO plg_commercial_catalog.variant_family_revision "
                        + "(company_id, variant_family_id, family_version, display_name, state) "
                        + "VALUES (?1, ?2, 0, 'Clothing', 'ACTIVE')",
                fixture.companyId().value(), fixture.variantFamilyId());
        insert(entityManager,
                "INSERT INTO plg_commercial_catalog.variant_attribute_revision "
                        + "(company_id, variant_family_id, family_version, attribute_code, "
                        + "display_name, value_type, required, position) "
                        + "VALUES (?1, ?2, 0, 'COLOR', 'Color', 'TEXT', TRUE, 0)",
                fixture.companyId().value(), fixture.variantFamilyId());
    }

    private static void insert(EntityManager entityManager, String sql, Object... parameters) {
        var query = entityManager.createNativeQuery(sql);
        for (int index = 0; index < parameters.length; index++) {
            query.setParameter(index + 1, parameters[index]);
        }
        query.executeUpdate();
    }

    private static <T> T inTransaction(Function<EntityManager, T> work) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            T result = work.apply(entityManager);
            entityManager.getTransaction().commit();
            return result;
        } catch (RuntimeException failure) {
            rollbackIfActive(entityManager);
            throw failure;
        } finally {
            entityManager.close();
        }
    }

    private static void rollbackIfActive(EntityManager entityManager) {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
    }

    private record Fixture(
            CompanyId companyId,
            CatalogItemId itemId,
            UUID taxProfileId,
            UUID mainCategoryId,
            UUID secondaryCategoryId,
            UUID brandId,
            UUID tagId,
            UUID variantFamilyId) {
    }
}
