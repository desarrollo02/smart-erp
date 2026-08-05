package py.com.logixone.plugins.businesspartners.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerKind;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerRole;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerPersistenceCode;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerPersistenceException;
import py.com.logixone.plugins.businesspartners.application.query.BusinessPartnerSearchCriteria;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartner;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAddress;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAttributeCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerContact;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerContactChannel;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDetailId;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionKind;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerIdentification;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerName;

@Testcontainers
class BusinessPartnerJpaRepositoryPostgreSqlIT {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse(
                    "postgres:18.4-bookworm@sha256:16fa100a3a6e92c0556632870455e7f8c6f3df5cefddd67d6b95292732bd7ff0")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withDatabaseName("logixone_business_partners_jpa_test")
            .withUsername("logixone_test")
            .withPassword("test-only-password");

    private static EntityManagerFactory entityManagerFactory;

    @BeforeAll
    static void migrateAndValidateJpaMappings() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(BusinessPartnersPersistenceNames.SCHEMA)
                .defaultSchema(BusinessPartnersPersistenceNames.SCHEMA)
                .table("flyway_schema_history")
                .locations("classpath:db/migration/business_partners")
                .createSchemas(true)
                .cleanDisabled(true)
                .validateOnMigrate(true)
                .load()
                .migrate();
        entityManagerFactory = Persistence.createEntityManagerFactory(
                "logixone-business-partners-test-pu",
                Map.of(
                        "jakarta.persistence.jdbc.driver", "org.postgresql.Driver",
                        "jakarta.persistence.jdbc.url", POSTGRES.getJdbcUrl(),
                        "jakarta.persistence.jdbc.user", POSTGRES.getUsername(),
                        "jakarta.persistence.jdbc.password", POSTGRES.getPassword()));
    }

    @AfterAll
    static void closeEntityManagerFactory() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @Test
    void validatesTheFlywaySchemaAndRoundTripsTheCompleteAggregate() {
        CompanyId companyId = companyId();
        BusinessPartner initial = insert(companyId, "BP-COMPLETE");
        BusinessPartner withRole = change(companyId, initial.id(), partner -> partner.assignRole(
                partner.version(), BusinessPartnerRole.CLIENT, Optional.of(new BusinessPartnerCode("CLI-10"))));
        BusinessPartner withIdentification = change(companyId, initial.id(), partner -> partner.addIdentification(
                partner.version(),
                BusinessPartnerIdentification.create(
                        detailId(),
                        code("tax_id"),
                        Optional.of("PY"),
                        "80001234-5",
                        Optional.of("5"),
                        Optional.empty())));
        BusinessPartner withAddress = change(companyId, initial.id(), partner -> partner.addAddress(
                partner.version(), address(true)));
        BusinessPartner withChannel = change(companyId, initial.id(), partner -> partner.addContactChannel(
                partner.version(), channel("contact", true)));
        BusinessPartner complete = change(companyId, initial.id(), partner -> partner.addContact(
                partner.version(),
                new BusinessPartnerContact(
                        detailId(),
                        new BusinessPartnerName("Ana Demo"),
                        Optional.of(new BusinessPartnerName("Compras")),
                        List.of(channel("work", true)),
                        true)));

        BusinessPartner loaded = inTransaction(entityManager -> new JpaBusinessPartnerRepository(entityManager)
                .findById(companyId, initial.id()).orElseThrow());

        assertEquals(1, withRole.roles().size());
        assertEquals(1, withIdentification.identifications().size());
        assertEquals(1, withAddress.addresses().size());
        assertEquals(1, withChannel.channels().size());
        assertEquals(complete.snapshot(), loaded.snapshot());
        assertEquals(5, loaded.version());
    }

    @Test
    void neverReturnsTheSameOpaqueIdFromAnotherCompanyScope() {
        CompanyId owner = companyId();
        BusinessPartner stored = insert(owner, "BP-SCOPED");

        boolean leaked = inTransaction(entityManager -> new JpaBusinessPartnerRepository(entityManager)
                .findById(companyId(), stored.id()).isPresent());

        assertFalse(leaked);
    }

    @Test
    void exposesDuplicateIdentificationCandidatesWithoutRejectingThem() {
        CompanyId companyId = companyId();
        BusinessPartner first = insert(companyId, "BP-DUP-1");
        BusinessPartner second = insert(companyId, "BP-DUP-2");
        BusinessPartnerIdentification identification = BusinessPartnerIdentification.create(
                detailId(), code("tax_id"), Optional.of("PY"), "80009999-1", Optional.empty(), Optional.empty());
        change(companyId, first.id(), partner -> partner.addIdentification(partner.version(), identification));
        change(companyId, second.id(), partner -> partner.addIdentification(
                partner.version(),
                BusinessPartnerIdentification.create(
                        detailId(), code("tax_id"), Optional.of("PY"), "80009999-1",
                        Optional.empty(), Optional.empty())));

        List<BusinessPartnerId> candidates = inTransaction(entityManager ->
                new JpaBusinessPartnerRepository(entityManager).findIdentificationCandidates(
                        companyId, identification.duplicateCandidateKey()));

        assertEquals(2, candidates.size());
        assertTrue(candidates.containsAll(List.of(first.id(), second.id())));
    }

    @Test
    void searchesAndPaginatesOnlyInsideTheRequestedCompany() {
        CompanyId companyId = companyId();
        BusinessPartner first = insert(companyId, "BP-SEARCH-ALPHA");
        BusinessPartner second = insert(companyId, "BP-SEARCH-BETA");
        insert(companyId(), "BP-SEARCH-ALPHA-OTHER-COMPANY");
        change(companyId, first.id(), partner -> partner.assignRole(
                partner.version(), BusinessPartnerRole.CLIENT, Optional.empty()));
        BusinessPartnerIdentification identification = BusinessPartnerIdentification.create(
                detailId(), code("tax_id"), Optional.of("PY"), "80005555-1",
                Optional.empty(), Optional.empty());
        change(companyId, first.id(), partner -> partner.addIdentification(
                partner.version(), identification));
        change(companyId, second.id(), partner -> partner.inactivate(partner.version()));

        var byIdentification = inTransaction(entityManager ->
                new JpaBusinessPartnerRepository(entityManager).search(
                        companyId,
                        new BusinessPartnerSearchCriteria(
                                Optional.of("80005555"), Optional.empty(), Optional.empty(), 0, 20)));
        var byRole = inTransaction(entityManager -> new JpaBusinessPartnerRepository(entityManager).search(
                companyId,
                new BusinessPartnerSearchCriteria(
                        Optional.empty(), Optional.of(BusinessPartnerRole.CLIENT),
                        Optional.of(BusinessPartnerState.ACTIVE), 0, 20)));
        var firstPage = inTransaction(entityManager -> new JpaBusinessPartnerRepository(entityManager).search(
                companyId,
                new BusinessPartnerSearchCriteria(
                        Optional.of("BP-SEARCH"), Optional.empty(), Optional.empty(), 0, 1)));
        var unfiltered = inTransaction(entityManager -> new JpaBusinessPartnerRepository(entityManager).search(
                companyId,
                new BusinessPartnerSearchCriteria(
                        Optional.empty(), Optional.empty(), Optional.empty(), 0, 20)));

        assertEquals(List.of(first.id()), byIdentification.items().stream().map(value -> value.id()).toList());
        assertEquals(List.of(first.id()), byRole.items().stream().map(value -> value.id()).toList());
        assertEquals(2, firstPage.total());
        assertEquals(1, firstPage.items().size());
        assertEquals(2, unfiltered.total());
        assertEquals(2, unfiltered.items().size());
    }

    @Test
    void mapsGeneralCodeConflictToAStableOutcomeAndRollsBack() {
        CompanyId companyId = companyId();
        insert(companyId, "BP-UNIQUE");

        BusinessPartnerPersistenceException conflict = assertThrows(
                BusinessPartnerPersistenceException.class,
                () -> insert(companyId, "BP-UNIQUE"));

        assertEquals(BusinessPartnerPersistenceCode.GENERAL_CODE_ALREADY_EXISTS, conflict.code());
    }

    @Test
    void persistsCompanyOwnedChannelKindsWithoutLeakingAcrossCompanies() {
        CompanyId owner = companyId();
        CompanyId anotherCompany = companyId();
        BusinessPartnerDefinition definition = BusinessPartnerDefinition.create(
                owner,
                BusinessPartnerDefinitionKind.CHANNEL_KIND,
                code("telegram"),
                new BusinessPartnerName("Telegram empresarial"));

        BusinessPartnerDefinition stored = inTransaction(entityManager ->
                new JpaBusinessPartnerDefinitionRepository(entityManager).insert(definition));
        var ownerDefinitions = inTransaction(entityManager ->
                new JpaBusinessPartnerDefinitionRepository(entityManager).findAll(
                        owner, BusinessPartnerDefinitionKind.CHANNEL_KIND));
        var leaked = inTransaction(entityManager ->
                new JpaBusinessPartnerDefinitionRepository(entityManager).findByCode(
                        anotherCompany,
                        BusinessPartnerDefinitionKind.CHANNEL_KIND,
                        code("telegram")));

        assertEquals(definition, stored);
        assertEquals(List.of(definition), ownerDefinitions);
        assertTrue(leaked.isEmpty());

        BusinessPartnerPersistenceException duplicate = assertThrows(
                BusinessPartnerPersistenceException.class,
                () -> inTransaction(entityManager ->
                        new JpaBusinessPartnerDefinitionRepository(entityManager).insert(definition)));
        assertEquals(BusinessPartnerPersistenceCode.GENERAL_CODE_ALREADY_EXISTS, duplicate.code());
    }

    @Test
    void resolvesAnOperationalDefinitionWithCompanyAndKindScope() {
        CompanyId companyId = companyId();
        BusinessPartnerDefinition definition = BusinessPartnerDefinition.create(
                companyId,
                BusinessPartnerDefinitionKind.IDENTIFICATION_TYPE,
                code("membership_card"),
                new BusinessPartnerName("Carné de socio"));
        inTransaction(entityManager ->
                new JpaBusinessPartnerDefinitionRepository(entityManager).insert(definition));

        var resolved = inTransaction(entityManager ->
                new JpaBusinessPartnerDefinitionRepository(entityManager).findByCodeForReference(
                        companyId,
                        BusinessPartnerDefinitionKind.IDENTIFICATION_TYPE,
                        code("membership_card")));
        var wrongKind = inTransaction(entityManager ->
                new JpaBusinessPartnerDefinitionRepository(entityManager).findByCodeForReference(
                        companyId,
                        BusinessPartnerDefinitionKind.ADDRESS_TYPE,
                        code("membership_card")));
        var wrongCompany = inTransaction(entityManager ->
                new JpaBusinessPartnerDefinitionRepository(entityManager).findByCodeForReference(
                        companyId(),
                        BusinessPartnerDefinitionKind.IDENTIFICATION_TYPE,
                        code("membership_card")));

        assertEquals(Optional.of(definition), resolved);
        assertTrue(wrongKind.isEmpty());
        assertTrue(wrongCompany.isEmpty());
    }

    @Test
    void changesAChannelKindStateWithOptimisticVersionAndPreservesTheRow() {
        CompanyId companyId = companyId();
        BusinessPartnerDefinition initial = BusinessPartnerDefinition.create(
                companyId,
                BusinessPartnerDefinitionKind.CHANNEL_KIND,
                code("lifecycle_channel"),
                new BusinessPartnerName("Canal con ciclo"));
        inTransaction(entityManager ->
                new JpaBusinessPartnerDefinitionRepository(entityManager).insert(initial));

        BusinessPartnerDefinition inactive = inTransaction(entityManager ->
                new JpaBusinessPartnerDefinitionRepository(entityManager).update(
                        initial.changeState(BusinessPartnerState.INACTIVE, 0), 0));

        assertEquals(BusinessPartnerState.INACTIVE, inactive.state());
        assertEquals(1, inactive.version());
        assertEquals(List.of(inactive), inTransaction(entityManager ->
                new JpaBusinessPartnerDefinitionRepository(entityManager).findAll(
                        companyId, BusinessPartnerDefinitionKind.CHANNEL_KIND)));

        BusinessPartnerPersistenceException stale = assertThrows(
                BusinessPartnerPersistenceException.class,
                () -> inTransaction(entityManager ->
                        new JpaBusinessPartnerDefinitionRepository(entityManager).update(
                                initial.changeState(BusinessPartnerState.INACTIVE, 0), 0)));
        assertEquals(BusinessPartnerPersistenceCode.VERSION_CONFLICT, stale.code());

        BusinessPartnerDefinition active = inTransaction(entityManager ->
                new JpaBusinessPartnerDefinitionRepository(entityManager).update(
                        inactive.changeState(BusinessPartnerState.ACTIVE, 1), 1));
        assertEquals(BusinessPartnerState.ACTIVE, active.state());
        assertEquals(2, active.version());
        var history = inTransaction(entityManager ->
                new JpaBusinessPartnerDefinitionRepository(entityManager).history(
                        companyId,
                        BusinessPartnerDefinitionKind.CHANNEL_KIND,
                        code("lifecycle_channel")));
        assertEquals(List.of(2L, 1L, 0L),
                history.stream().map(revision -> revision.version()).toList());
        assertEquals(List.of(
                        BusinessPartnerState.ACTIVE,
                        BusinessPartnerState.INACTIVE,
                        BusinessPartnerState.ACTIVE),
                history.stream().map(revision -> revision.state()).toList());
        assertTrue(history.stream().allMatch(revision -> revision.changedAt() != null));
    }

    @Test
    void revisesAChannelKindNameWithoutChangingItsStableCode() {
        CompanyId companyId = companyId();
        BusinessPartnerDefinition initial = BusinessPartnerDefinition.create(
                companyId,
                BusinessPartnerDefinitionKind.CHANNEL_KIND,
                code("editable_channel"),
                new BusinessPartnerName("Canal original"));
        inTransaction(entityManager ->
                new JpaBusinessPartnerDefinitionRepository(entityManager).insert(initial));

        BusinessPartnerDefinition revised = inTransaction(entityManager ->
                new JpaBusinessPartnerDefinitionRepository(entityManager).update(
                        initial.reviseDisplayName(
                                new BusinessPartnerName("Canal revisado"), 0),
                        0));
        var history = inTransaction(entityManager ->
                new JpaBusinessPartnerDefinitionRepository(entityManager).history(
                        companyId,
                        BusinessPartnerDefinitionKind.CHANNEL_KIND,
                        code("editable_channel")));

        assertEquals("editable_channel", revised.code().value());
        assertEquals("Canal revisado", revised.displayName().value());
        assertEquals(1, revised.version());
        assertEquals(List.of(1L, 0L),
                history.stream().map(revision -> revision.version()).toList());
        assertEquals(List.of("Canal revisado", "Canal original"),
                history.stream()
                        .map(revision -> revision.displayName().value())
                        .toList());
        assertTrue(inTransaction(entityManager ->
                new JpaBusinessPartnerDefinitionRepository(entityManager).history(
                        companyId(),
                        BusinessPartnerDefinitionKind.CHANNEL_KIND,
                        code("editable_channel"))).isEmpty());
    }

    @Test
    void mapsRoleCodeConflictToAStableOutcomeAndRollsBack() {
        CompanyId companyId = companyId();
        BusinessPartner first = insert(companyId, "BP-ROLE-1");
        BusinessPartner second = insert(companyId, "BP-ROLE-2");
        change(companyId, first.id(), partner -> partner.assignRole(
                partner.version(),
                BusinessPartnerRole.CLIENT,
                Optional.of(new BusinessPartnerCode("CLIENT-SHARED"))));

        BusinessPartnerPersistenceException conflict = assertThrows(
                BusinessPartnerPersistenceException.class,
                () -> change(companyId, second.id(), partner -> partner.assignRole(
                        partner.version(),
                        BusinessPartnerRole.CLIENT,
                        Optional.of(new BusinessPartnerCode("CLIENT-SHARED")))));

        assertEquals(BusinessPartnerPersistenceCode.ROLE_CODE_ALREADY_EXISTS, conflict.code());
        boolean roleWasRolledBack = inTransaction(entityManager ->
                new JpaBusinessPartnerRepository(entityManager)
                        .findById(companyId, second.id()).orElseThrow().roles().isEmpty());
        assertTrue(roleWasRolledBack);
    }

    @Test
    void rejectsAStaleWriterThroughTheAggregateVersion() {
        CompanyId companyId = companyId();
        BusinessPartner stored = insert(companyId, "BP-VERSION");
        BusinessPartner stale = inTransaction(entityManager -> new JpaBusinessPartnerRepository(entityManager)
                .findById(companyId, stored.id()).orElseThrow());

        change(companyId, stored.id(), partner -> partner.rename(
                partner.version(),
                new BusinessPartnerName("Winner"),
                Optional.empty(),
                Optional.empty()));
        stale.changeCode(stale.version(), new BusinessPartnerCode("BP-STALE"));

        BusinessPartnerPersistenceException conflict = assertThrows(
                BusinessPartnerPersistenceException.class,
                () -> inTransaction(entityManager -> new JpaBusinessPartnerRepository(entityManager)
                        .update(stale, 0)));
        assertEquals(BusinessPartnerPersistenceCode.VERSION_CONFLICT, conflict.code());
    }

    @Test
    void replacesThePrimaryAddressWithoutAUniqueConstraintRace() {
        CompanyId companyId = companyId();
        BusinessPartner stored = insert(companyId, "BP-PRIMARY");
        change(companyId, stored.id(), partner -> partner.addAddress(partner.version(), address(true)));

        BusinessPartner updated = change(companyId, stored.id(), partner ->
                partner.addAddress(partner.version(), address(true)));

        assertEquals(2, updated.addresses().size());
        assertEquals(1, updated.addresses().stream().filter(BusinessPartnerAddress::primary).count());
    }

    @Test
    void allocatesIndependentTransactionalSequencesWithoutMaxPlusOne() {
        CompanyId company = companyId();

        long first = inTransaction(entityManager ->
                new JpaBusinessPartnerCodeSequenceRepository(entityManager).nextValue(company, "general"));
        long second = inTransaction(entityManager ->
                new JpaBusinessPartnerCodeSequenceRepository(entityManager).nextValue(company, "general"));
        long anotherScope = inTransaction(entityManager ->
                new JpaBusinessPartnerCodeSequenceRepository(entityManager).nextValue(company, "supplier"));
        long anotherCompany = inTransaction(entityManager ->
                new JpaBusinessPartnerCodeSequenceRepository(entityManager).nextValue(companyId(), "general"));

        assertEquals(1, first);
        assertEquals(2, second);
        assertEquals(1, anotherScope);
        assertEquals(1, anotherCompany);
    }

    @Test
    void serializesConcurrentAllocationsWithoutDuplicates() throws Exception {
        CompanyId company = companyId();
        try (var executor = Executors.newFixedThreadPool(6)) {
            List<Callable<Long>> allocations = java.util.stream.IntStream.range(0, 12)
                    .mapToObj(ignored -> (Callable<Long>) () -> inTransaction(entityManager ->
                            new JpaBusinessPartnerCodeSequenceRepository(entityManager)
                                    .nextValue(company, "concurrent")))
                    .toList();
            List<Long> values = executor.invokeAll(allocations).stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception failure) {
                            throw new IllegalStateException(failure);
                        }
                    })
                    .sorted()
                    .toList();

            assertEquals(
                    java.util.stream.LongStream.rangeClosed(1, 12).boxed().toList(),
                    values);
        }
    }

    private static BusinessPartner insert(CompanyId companyId, String value) {
        BusinessPartner partner = BusinessPartner.create(
                companyId,
                new BusinessPartnerId(UUID.randomUUID()),
                new BusinessPartnerCode(value),
                BusinessPartnerKind.ORGANIZATION,
                new BusinessPartnerName("Partner " + value),
                Optional.empty(),
                Optional.empty());
        return inTransaction(entityManager -> new JpaBusinessPartnerRepository(entityManager).insert(partner));
    }

    private static BusinessPartner change(
            CompanyId companyId, BusinessPartnerId id, Consumer<BusinessPartner> mutation) {
        return inTransaction(entityManager -> {
            JpaBusinessPartnerRepository repository = new JpaBusinessPartnerRepository(entityManager);
            BusinessPartner partner = repository.findById(companyId, id).orElseThrow();
            long expected = partner.version();
            mutation.accept(partner);
            return repository.update(partner, expected);
        });
    }

    private static BusinessPartnerAddress address(boolean primary) {
        return new BusinessPartnerAddress(
                detailId(),
                code("postal"),
                code("billing"),
                "Avenida Demo 123",
                Optional.empty(),
                Optional.of("123"),
                Optional.of("1209"),
                Optional.of("PY"),
                Optional.of("Central"),
                Optional.of("Asunción"),
                true,
                primary);
    }

    private static BusinessPartnerContactChannel channel(String purpose, boolean primary) {
        return new BusinessPartnerContactChannel(
                detailId(), code("email"), code(purpose), "demo@example.invalid", true, primary);
    }

    private static BusinessPartnerAttributeCode code(String value) {
        return new BusinessPartnerAttributeCode(value);
    }

    private static BusinessPartnerDetailId detailId() {
        return new BusinessPartnerDetailId(UUID.randomUUID());
    }

    private static CompanyId companyId() {
        return new CompanyId(UUID.randomUUID());
    }

    private static <T> T inTransaction(Function<EntityManager, T> work) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        try {
            T result = work.apply(entityManager);
            entityManager.getTransaction().commit();
            return result;
        } catch (RuntimeException failure) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw failure;
        } finally {
            entityManager.close();
        }
    }
}
