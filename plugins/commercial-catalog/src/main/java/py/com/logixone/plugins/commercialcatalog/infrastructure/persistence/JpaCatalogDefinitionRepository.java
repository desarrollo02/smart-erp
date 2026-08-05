package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.application.definition.CatalogDefinitions;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogDefinitionRepository;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogPersistenceCode;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogPersistenceException;
import py.com.logixone.plugins.commercialcatalog.application.port.VariantFamilyAssignmentRepository;
import py.com.logixone.plugins.commercialcatalog.domain.BrandId;
import py.com.logixone.plugins.commercialcatalog.domain.CategoryId;
import py.com.logixone.plugins.commercialcatalog.domain.TagId;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileId;
import py.com.logixone.plugins.commercialcatalog.domain.UnitCode;
import py.com.logixone.plugins.commercialcatalog.domain.VariantAttributeCode;
import py.com.logixone.plugins.commercialcatalog.domain.VariantFamilyId;
import py.com.logixone.plugins.commercialcatalog.domain.VariantValueType;

/** Native persistence adapter for simple controlled definitions without cross-aggregate JPA links. */
@ApplicationScoped
@Transactional
public class JpaCatalogDefinitionRepository
        implements CatalogDefinitionRepository, VariantFamilyAssignmentRepository {

    private static final String SCHEMA = CommercialCatalogPersistenceNames.SCHEMA;

    @PersistenceContext(unitName = CommercialCatalogPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaCatalogDefinitionRepository() {
    }

    JpaCatalogDefinitionRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public CatalogDefinitions.Snapshot findAll(CompanyId companyId) {
        Objects.requireNonNull(companyId, "companyId");
        UUID company = companyId.value();
        return new CatalogDefinitions.Snapshot(
                units(company), categories(company), brands(company), tags(company),
                taxProfiles(company), variantFamilies(company), replacementLinks(company));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<CatalogDefinitions.VariantFamily> findCurrentForAssignment(
            CompanyId companyId, VariantFamilyId familyId) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(familyId, "familyId");
        List<Object[]> families = entityManager.createNativeQuery(
                        "SELECT variant_family_id, code, display_name, state, version "
                                + "FROM " + SCHEMA + ".variant_family "
                                + "WHERE company_id = :company AND variant_family_id = :family "
                                + "FOR SHARE")
                .setParameter("company", companyId.value())
                .setParameter("family", familyId.value())
                .getResultList();
        if (families.isEmpty()) {
            return Optional.empty();
        }
        List<Object[]> attributes = entityManager.createNativeQuery(
                        "SELECT attribute_code, display_name, value_type, required, position "
                                + "FROM " + SCHEMA + ".variant_attribute_definition "
                                + "WHERE company_id = :company AND variant_family_id = :family "
                                + "ORDER BY position, attribute_code")
                .setParameter("company", companyId.value())
                .setParameter("family", familyId.value())
                .getResultList();
        Object[] family = families.getFirst();
        return Optional.of(new CatalogDefinitions.VariantFamily(
                new VariantFamilyId(uuid(family[0])), string(family[1]), string(family[2]),
                attributes.stream().map(attribute -> new CatalogDefinitions.VariantAttribute(
                        new VariantAttributeCode(string(attribute[0])), string(attribute[1]),
                        VariantValueType.valueOf(string(attribute[2])), (Boolean) attribute[3],
                        number(attribute[4]).intValue())).toList(),
                state(family[3]), number(family[4]).longValue()));
    }

    @Override
    public CatalogDefinitions.Unit insert(
            CompanyId companyId, CatalogDefinitions.Unit definition) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(definition, "definition");
        try {
            execute("INSERT INTO " + SCHEMA + ".unit_definition " +
                            "(company_id, unit_code, display_name, decimal_scale, state, version) " +
                            "VALUES (:company, :code, :name, :scale, :state, :version)",
                    Map.of(
                            "company", companyId.value(),
                            "code", definition.code().value(),
                            "name", definition.displayName(),
                            "scale", definition.decimalScale(),
                            "state", definition.state().name(),
                            "version", definition.version()));
            insertSimpleRevision(companyId, new CatalogDefinitions.SimpleRevision(
                    CatalogDefinitions.SimpleKind.UNIT, definition.code().value(),
                    definition.version(), definition.displayName(),
                    Optional.of(definition.decimalScale()), Optional.empty(),
                    definition.state(), true));
            return definition;
        } catch (PersistenceException failure) {
            throw PostgreSqlCatalogConflictMapper.map(failure);
        }
    }

    @Override
    public CatalogDefinitions.Category insert(
            CompanyId companyId, CatalogDefinitions.Category definition) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(definition, "definition");
        try {
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("company", companyId.value());
            parameters.put("id", definition.id().value());
            parameters.put("parent", definition.parentId().map(CategoryId::value).orElse(null));
            parameters.put("code", definition.code());
            parameters.put("name", definition.displayName());
            parameters.put("state", definition.state().name());
            parameters.put("version", definition.version());
            execute("INSERT INTO " + SCHEMA + ".category_definition " +
                    "(company_id, category_id, parent_category_id, code, display_name, state, version) " +
                    "VALUES (:company, :id, :parent, :code, :name, :state, :version)", parameters);
            insertSimpleRevision(companyId, new CatalogDefinitions.SimpleRevision(
                    CatalogDefinitions.SimpleKind.CATEGORY,
                    definition.id().value().toString(), definition.version(),
                    definition.displayName(), Optional.empty(), definition.parentId(),
                    definition.state(), true));
            return definition;
        } catch (PersistenceException failure) {
            throw PostgreSqlCatalogConflictMapper.map(failure);
        }
    }

    @Override
    public CatalogDefinitions.Brand insert(
            CompanyId companyId, CatalogDefinitions.Brand definition) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(definition, "definition");
        try {
            execute("INSERT INTO " + SCHEMA + ".brand_definition " +
                            "(company_id, brand_id, code, display_name, state, version) " +
                            "VALUES (:company, :id, :code, :name, :state, :version)",
                    common(companyId.value(), definition.id().value(), definition.code(),
                            definition.displayName(), definition.state(), definition.version()));
            insertSimpleRevision(companyId, new CatalogDefinitions.SimpleRevision(
                    CatalogDefinitions.SimpleKind.BRAND,
                    definition.id().value().toString(), definition.version(),
                    definition.displayName(), Optional.empty(), Optional.empty(),
                    definition.state(), true));
            return definition;
        } catch (PersistenceException failure) {
            throw PostgreSqlCatalogConflictMapper.map(failure);
        }
    }

    @Override
    public CatalogDefinitions.Tag insert(
            CompanyId companyId, CatalogDefinitions.Tag definition) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(definition, "definition");
        try {
            execute("INSERT INTO " + SCHEMA + ".tag_definition " +
                            "(company_id, tag_id, code, display_name, state, version) " +
                            "VALUES (:company, :id, :code, :name, :state, :version)",
                    common(companyId.value(), definition.id().value(), definition.code(),
                            definition.displayName(), definition.state(), definition.version()));
            insertSimpleRevision(companyId, new CatalogDefinitions.SimpleRevision(
                    CatalogDefinitions.SimpleKind.TAG,
                    definition.id().value().toString(), definition.version(),
                    definition.displayName(), Optional.empty(), Optional.empty(),
                    definition.state(), true));
            return definition;
        } catch (PersistenceException failure) {
            throw PostgreSqlCatalogConflictMapper.map(failure);
        }
    }

    @Override
    public CatalogDefinitions.Lifecycle changeSimpleState(
            CompanyId companyId,
            CatalogDefinitions.SimpleKind kind,
            String identity,
            CatalogDefinitions.State targetState,
            long expectedVersion) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(targetState, "targetState");
        DefinitionStorage storage = storage(kind, identity);
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> currentRows = entityManager.createNativeQuery(
                            "SELECT state, version, " + replacementColumn(kind)
                                    + " FROM " + SCHEMA + "." + storage.table()
                                    + " WHERE company_id = :company AND "
                                    + storage.identityColumn() + " = :identity")
                    .setParameter("company", companyId.value())
                    .setParameter("identity", storage.identity())
                    .getResultList();
            if (currentRows.isEmpty()) {
                throw new CatalogPersistenceException(
                        CatalogPersistenceCode.DEFINITION_NOT_FOUND);
            }
            Object[] current = currentRows.getFirst();
            CatalogDefinitions.State currentState = state(current[0]);
            long currentVersion = number(current[1]).longValue();
            if (currentVersion != expectedVersion) {
                throw new CatalogPersistenceException(CatalogPersistenceCode.VERSION_CONFLICT);
            }
            if (targetState == CatalogDefinitions.State.ACTIVE && current[2] != null) {
                throw new IllegalStateException(
                        "A replaced definition cannot be reactivated");
            }
            if (currentState == targetState) {
                return new CatalogDefinitions.Lifecycle(
                        kind, storage.canonicalIdentity(), targetState,
                        currentVersion, false);
            }
            int updated = execute("UPDATE " + SCHEMA + "." + storage.table()
                            + " SET state = :state, version = version + 1"
                            + " WHERE company_id = :company AND "
                            + storage.identityColumn() + " = :identity"
                            + " AND version = :version",
                    Map.of(
                            "state", targetState.name(),
                            "company", companyId.value(),
                            "identity", storage.identity(),
                            "version", expectedVersion));
            if (updated != 1) {
                throw new CatalogPersistenceException(CatalogPersistenceCode.VERSION_CONFLICT);
            }
            CatalogDefinitions.SimpleRevision revision = currentSimpleRevision(
                    companyId, kind, storage.canonicalIdentity());
            insertSimpleRevision(companyId, revision);
            return new CatalogDefinitions.Lifecycle(
                    kind, storage.canonicalIdentity(), targetState,
                    expectedVersion + 1, true);
        } catch (CatalogPersistenceException failure) {
            throw failure;
        } catch (PersistenceException failure) {
            throw PostgreSqlCatalogConflictMapper.map(failure);
        }
    }

    @Override
    public CatalogDefinitions.SimpleRevision reviseSimpleDefinition(
            CompanyId companyId,
            CatalogDefinitions.SimpleKind kind,
            String identity,
            String displayName,
            Optional<Integer> decimalScale,
            Optional<CategoryId> parentId,
            long expectedVersion) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(decimalScale, "decimalScale");
        Objects.requireNonNull(parentId, "parentId");
        DefinitionStorage storage = storage(kind, identity);
        try {
            CatalogDefinitions.SimpleRevision current = currentSimpleRevision(
                    companyId, kind, storage.canonicalIdentity());
            if (current.version() != expectedVersion) {
                throw new CatalogPersistenceException(CatalogPersistenceCode.VERSION_CONFLICT);
            }
            if (hasReplacement(companyId, kind, storage)) {
                throw new IllegalStateException("A replaced definition cannot be revised");
            }
            CatalogDefinitions.SimpleRevision revision = new CatalogDefinitions.SimpleRevision(
                    kind, storage.canonicalIdentity(), expectedVersion + 1, displayName,
                    decimalScale, parentId, current.state(), true);
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("company", companyId.value());
            parameters.put("identity", storage.identity());
            parameters.put("name", revision.displayName());
            parameters.put("version", expectedVersion);
            String assignments = switch (kind) {
                case UNIT -> {
                    parameters.put("scale", revision.decimalScale().orElseThrow());
                    yield "display_name = :name, decimal_scale = :scale";
                }
                case CATEGORY -> {
                    parameters.put("parent", revision.parentId()
                            .map(CategoryId::value).orElse(null));
                    yield "display_name = :name, parent_category_id = :parent";
                }
                case BRAND, TAG -> "display_name = :name";
            };
            int updated = execute("UPDATE " + SCHEMA + "." + storage.table()
                            + " SET " + assignments
                            + ", version = version + 1, updated_at = CURRENT_TIMESTAMP"
                            + " WHERE company_id = :company AND "
                            + storage.identityColumn() + " = :identity"
                            + " AND version = :version",
                    parameters);
            if (updated != 1) {
                throw new CatalogPersistenceException(CatalogPersistenceCode.VERSION_CONFLICT);
            }
            insertSimpleRevision(companyId, revision);
            return revision;
        } catch (CatalogPersistenceException failure) {
            throw failure;
        } catch (PersistenceException failure) {
            throw PostgreSqlCatalogConflictMapper.map(failure);
        }
    }

    @Override
    public List<CatalogDefinitions.SimpleRevision> simpleDefinitionHistory(
            CompanyId companyId,
            CatalogDefinitions.SimpleKind kind,
            String identity) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(identity, "identity");
        DefinitionStorage storage = storage(kind, identity);
        String revisionTable = revisionTable(kind);
        String structuralColumns = switch (kind) {
            case UNIT -> "revision.decimal_scale, NULL";
            case CATEGORY -> "NULL, revision.parent_category_id";
            case BRAND, TAG -> "NULL, NULL";
        };
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT revision.definition_version, revision.display_name, "
                                + structuralColumns + ", revision.state, "
                                + "revision.definition_version = definition.version "
                                + "FROM " + SCHEMA + "." + storage.table() + " definition "
                                + "JOIN " + SCHEMA + "." + revisionTable + " revision "
                                + "ON revision.company_id = definition.company_id AND revision."
                                + storage.identityColumn() + " = definition."
                                + storage.identityColumn() + " "
                                + "WHERE definition.company_id = :company AND definition."
                                + storage.identityColumn() + " = :identity "
                                + "ORDER BY revision.definition_version DESC")
                .setParameter("company", companyId.value())
                .setParameter("identity", storage.identity())
                .getResultList();
        if (rows.isEmpty()) {
            throw new CatalogPersistenceException(CatalogPersistenceCode.DEFINITION_NOT_FOUND);
        }
        return rows.stream().map(row -> new CatalogDefinitions.SimpleRevision(
                kind,
                storage.canonicalIdentity(),
                number(row[0]).longValue(),
                string(row[1]),
                Optional.ofNullable(row[2]).map(value -> number(value).intValue()),
                Optional.ofNullable(row[3]).map(value -> new CategoryId(uuid(value))),
                state(row[4]),
                (Boolean) row[5])).toList();
    }

    @Override
    public CatalogDefinitions.Replacement replaceSimpleDefinition(
            CompanyId companyId,
            CatalogDefinitions.SimpleKind kind,
            String identity,
            CatalogDefinitions.ReplacementCandidate replacement,
            long expectedVersion) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(replacement, "replacement");
        if (replacement.kind() != kind) {
            throw new IllegalArgumentException("Replacement kind must match the source kind");
        }
        DefinitionStorage source = storage(kind, identity);
        DefinitionStorage target = storage(kind, replacement.identity());
        if (source.canonicalIdentity().equals(target.canonicalIdentity())) {
            throw new IllegalArgumentException("Replacement identity must be different");
        }
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> currentRows = entityManager.createNativeQuery(
                            "SELECT state, version, " + replacementColumn(kind)
                                    + " FROM " + SCHEMA + "." + source.table()
                                    + " WHERE company_id = :company AND "
                                    + source.identityColumn() + " = :identity")
                    .setParameter("company", companyId.value())
                    .setParameter("identity", source.identity())
                    .getResultList();
            if (currentRows.isEmpty()) {
                throw new CatalogPersistenceException(
                        CatalogPersistenceCode.DEFINITION_NOT_FOUND);
            }
            Object[] current = currentRows.getFirst();
            if (number(current[1]).longValue() != expectedVersion) {
                throw new CatalogPersistenceException(CatalogPersistenceCode.VERSION_CONFLICT);
            }
            if (state(current[0]) != CatalogDefinitions.State.ACTIVE || current[2] != null) {
                throw new IllegalStateException(
                        "Only an active definition without a successor can be replaced");
            }
            if (kind == CatalogDefinitions.SimpleKind.CATEGORY) {
                replacement.parentId().ifPresent(parent -> requireActiveCategoryParent(
                        companyId.value(), parent, source));
            }
            insertReplacement(companyId, replacement);
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("company", companyId.value());
            parameters.put("identity", source.identity());
            parameters.put("replacement", target.identity());
            parameters.put("version", expectedVersion);
            int updated = execute("UPDATE " + SCHEMA + "." + source.table()
                            + " SET state = 'INACTIVE', " + replacementColumn(kind)
                            + " = :replacement, version = version + 1, "
                            + "updated_at = CURRENT_TIMESTAMP"
                            + " WHERE company_id = :company AND "
                            + source.identityColumn() + " = :identity"
                            + " AND state = 'ACTIVE' AND " + replacementColumn(kind)
                            + " IS NULL AND version = :version",
                    parameters);
            if (updated != 1) {
                throw new CatalogPersistenceException(CatalogPersistenceCode.VERSION_CONFLICT);
            }
            insertSimpleRevision(companyId,
                    currentSimpleRevision(companyId, kind, source.canonicalIdentity()));
            return new CatalogDefinitions.Replacement(
                    kind, source.canonicalIdentity(), expectedVersion + 1,
                    target.canonicalIdentity(), replacement.code(), 0);
        } catch (CatalogPersistenceException failure) {
            throw failure;
        } catch (PersistenceException failure) {
            throw PostgreSqlCatalogConflictMapper.map(failure);
        }
    }

    @Override
    public CatalogDefinitions.TaxProfile insert(
            CompanyId companyId, CatalogDefinitions.TaxProfile definition) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(definition, "definition");
        try {
            execute("INSERT INTO " + SCHEMA + ".tax_profile " +
                            "(company_id, tax_profile_id, code, display_name, state, version) " +
                            "VALUES (:company, :id, :code, :name, :state, :version)",
                    common(companyId.value(), definition.id().value(), definition.code(),
                            definition.displayName(), definition.state(), definition.version()));
            Map<String, Object> revision = new LinkedHashMap<>();
            revision.put("company", companyId.value());
            revision.put("id", definition.id().value());
            revision.put("version", definition.version());
            revision.put("kind", definition.internalKindCode());
            revision.put("description", definition.description());
            revision.put("from", definition.validFrom());
            revision.put("until", definition.validUntil().orElse(null));
            revision.put("active", definition.state() == CatalogDefinitions.State.ACTIVE);
            execute("INSERT INTO " + SCHEMA + ".tax_profile_revision " +
                    "(company_id, tax_profile_id, profile_version, internal_kind_code, " +
                    "description, valid_from, valid_until, active) " +
                    "VALUES (:company, :id, :version, :kind, :description, :from, :until, :active)",
                    revision);
            return definition;
        } catch (PersistenceException failure) {
            throw PostgreSqlCatalogConflictMapper.map(failure);
        }
    }

    @Override
    public CatalogDefinitions.TaxProfile changeTaxProfileState(
            CompanyId companyId,
            TaxProfileId id,
            CatalogDefinitions.State targetState,
            long expectedVersion) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(targetState, "targetState");
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> currentRows = entityManager.createNativeQuery(
                            "SELECT state, version FROM " + SCHEMA + ".tax_profile"
                                    + " WHERE company_id = :company"
                                    + " AND tax_profile_id = :identity")
                    .setParameter("company", companyId.value())
                    .setParameter("identity", id.value())
                    .getResultList();
            if (currentRows.isEmpty()) {
                throw new CatalogPersistenceException(
                        CatalogPersistenceCode.DEFINITION_NOT_FOUND);
            }
            Object[] current = currentRows.getFirst();
            CatalogDefinitions.State currentState = state(current[0]);
            long currentVersion = number(current[1]).longValue();
            if (currentVersion != expectedVersion) {
                throw new CatalogPersistenceException(CatalogPersistenceCode.VERSION_CONFLICT);
            }
            if (currentState != targetState) {
                int updated = execute("UPDATE " + SCHEMA + ".tax_profile"
                                + " SET state = :state, version = version + 1"
                                + " WHERE company_id = :company"
                                + " AND tax_profile_id = :identity"
                                + " AND version = :version",
                        Map.of(
                                "state", targetState.name(),
                                "company", companyId.value(),
                                "identity", id.value(),
                                "version", expectedVersion));
                if (updated != 1) {
                    throw new CatalogPersistenceException(
                            CatalogPersistenceCode.VERSION_CONFLICT);
                }
                execute("UPDATE " + SCHEMA + ".tax_profile_revision"
                                + " SET active = FALSE"
                                + " WHERE company_id = :company"
                                + " AND tax_profile_id = :identity"
                                + " AND active = TRUE",
                        Map.of(
                                "company", companyId.value(),
                                "identity", id.value()));
                int revisionInserted = execute(
                        "INSERT INTO " + SCHEMA + ".tax_profile_revision"
                                + " (company_id, tax_profile_id, profile_version,"
                                + " internal_kind_code, description, valid_from, valid_until, active)"
                                + " SELECT company_id, tax_profile_id, :newVersion,"
                                + " internal_kind_code, description, valid_from, valid_until, :active"
                                + " FROM " + SCHEMA + ".tax_profile_revision"
                                + " WHERE company_id = :company"
                                + " AND tax_profile_id = :identity"
                                + " AND profile_version = :previousVersion",
                        Map.of(
                                "newVersion", expectedVersion + 1,
                                "active", targetState == CatalogDefinitions.State.ACTIVE,
                                "company", companyId.value(),
                                "identity", id.value(),
                                "previousVersion", expectedVersion));
                if (revisionInserted != 1) {
                    throw new CatalogPersistenceException(
                            CatalogPersistenceCode.DEFINITION_NOT_FOUND);
                }
            }
            return taxProfile(companyId.value(), id.value());
        } catch (CatalogPersistenceException failure) {
            throw failure;
        } catch (PersistenceException failure) {
            throw PostgreSqlCatalogConflictMapper.map(failure);
        }
    }

    @Override
    public CatalogDefinitions.TaxProfile reviseTaxProfile(
            CompanyId companyId,
            TaxProfileId id,
            String internalKindCode,
            String description,
            java.time.Instant validFrom,
            Optional<java.time.Instant> validUntil,
            long expectedVersion) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(internalKindCode, "internalKindCode");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(validFrom, "validFrom");
        Objects.requireNonNull(validUntil, "validUntil");
        CatalogDefinitions.TaxProfile validated = new CatalogDefinitions.TaxProfile(
                id, "VALIDATION", "Validation", internalKindCode, description,
                validFrom, validUntil, CatalogDefinitions.State.ACTIVE, expectedVersion);
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> currentRows = entityManager.createNativeQuery(
                            "SELECT state, version FROM " + SCHEMA + ".tax_profile"
                                    + " WHERE company_id = :company"
                                    + " AND tax_profile_id = :identity")
                    .setParameter("company", companyId.value())
                    .setParameter("identity", id.value())
                    .getResultList();
            if (currentRows.isEmpty()) {
                throw new CatalogPersistenceException(
                        CatalogPersistenceCode.DEFINITION_NOT_FOUND);
            }
            Object[] current = currentRows.getFirst();
            CatalogDefinitions.State currentState = state(current[0]);
            long currentVersion = number(current[1]).longValue();
            if (currentVersion != expectedVersion) {
                throw new CatalogPersistenceException(CatalogPersistenceCode.VERSION_CONFLICT);
            }
            int updated = execute("UPDATE " + SCHEMA + ".tax_profile"
                            + " SET version = version + 1, updated_at = CURRENT_TIMESTAMP"
                            + " WHERE company_id = :company"
                            + " AND tax_profile_id = :identity"
                            + " AND version = :version",
                    Map.of(
                            "company", companyId.value(),
                            "identity", id.value(),
                            "version", expectedVersion));
            if (updated != 1) {
                throw new CatalogPersistenceException(CatalogPersistenceCode.VERSION_CONFLICT);
            }
            execute("UPDATE " + SCHEMA + ".tax_profile_revision"
                            + " SET active = FALSE"
                            + " WHERE company_id = :company"
                            + " AND tax_profile_id = :identity"
                            + " AND active = TRUE",
                    Map.of(
                            "company", companyId.value(),
                            "identity", id.value()));
            Map<String, Object> revision = new LinkedHashMap<>();
            revision.put("company", companyId.value());
            revision.put("id", id.value());
            revision.put("version", expectedVersion + 1);
            revision.put("kind", validated.internalKindCode());
            revision.put("description", validated.description());
            revision.put("from", validated.validFrom());
            revision.put("until", validated.validUntil().orElse(null));
            revision.put("active", currentState == CatalogDefinitions.State.ACTIVE);
            execute("INSERT INTO " + SCHEMA + ".tax_profile_revision "
                            + "(company_id, tax_profile_id, profile_version, internal_kind_code, "
                            + "description, valid_from, valid_until, active) "
                            + "VALUES (:company, :id, :version, :kind, :description, :from, :until, :active)",
                    revision);
            return taxProfile(companyId.value(), id.value());
        } catch (CatalogPersistenceException failure) {
            throw failure;
        } catch (PersistenceException failure) {
            throw PostgreSqlCatalogConflictMapper.map(failure);
        }
    }

    @Override
    public List<CatalogDefinitions.TaxProfileRevision> taxProfileHistory(
            CompanyId companyId, TaxProfileId id) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(id, "id");
        @SuppressWarnings("unchecked")
        List<Object[]> revisions = entityManager.createNativeQuery(
                        "SELECT revision.profile_version, revision.internal_kind_code, "
                                + "revision.description, "
                                + "EXTRACT(EPOCH FROM revision.valid_from), "
                                + "EXTRACT(EPOCH FROM revision.valid_until), "
                                + "revision.profile_version = profile.version "
                                + "FROM " + SCHEMA + ".tax_profile profile "
                                + "JOIN " + SCHEMA + ".tax_profile_revision revision "
                                + "ON revision.company_id = profile.company_id "
                                + "AND revision.tax_profile_id = profile.tax_profile_id "
                                + "WHERE profile.company_id = :company "
                                + "AND profile.tax_profile_id = :identity "
                                + "ORDER BY revision.profile_version DESC")
                .setParameter("company", companyId.value())
                .setParameter("identity", id.value())
                .getResultList();
        if (revisions.isEmpty()) {
            throw new CatalogPersistenceException(CatalogPersistenceCode.DEFINITION_NOT_FOUND);
        }
        return revisions.stream().map(row -> new CatalogDefinitions.TaxProfileRevision(
                id,
                number(row[0]).longValue(),
                string(row[1]),
                string(row[2]),
                epoch(row[3]),
                Optional.ofNullable(row[4]).map(JpaCatalogDefinitionRepository::epoch),
                (Boolean) row[5])).toList();
    }

    @Override
    public CatalogDefinitions.VariantFamily insert(
            CompanyId companyId, CatalogDefinitions.VariantFamily definition) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(definition, "definition");
        try {
            execute("INSERT INTO " + SCHEMA + ".variant_family " +
                            "(company_id, variant_family_id, code, display_name, state, version) " +
                            "VALUES (:company, :id, :code, :name, :state, :version)",
                    common(companyId.value(), definition.id().value(), definition.code(),
                            definition.displayName(), definition.state(), definition.version()));
            for (CatalogDefinitions.VariantAttribute attribute : definition.attributes()) {
                execute("INSERT INTO " + SCHEMA + ".variant_attribute_definition " +
                                "(company_id, variant_family_id, attribute_code, display_name, " +
                                "value_type, required, position) " +
                                "VALUES (:company, :id, :code, :name, :type, :required, :position)",
                        Map.of(
                                "company", companyId.value(),
                                "id", definition.id().value(),
                                "code", attribute.code().value(),
                                "name", attribute.displayName(),
                                "type", attribute.valueType().name(),
                                "required", attribute.required(),
                                "position", attribute.position()));
            }
            insertVariantFamilyRevision(companyId, definition);
            return definition;
        } catch (PersistenceException failure) {
            throw PostgreSqlCatalogConflictMapper.map(failure);
        }
    }

    @Override
    public CatalogDefinitions.VariantFamily changeVariantFamilyState(
            CompanyId companyId,
            VariantFamilyId id,
            CatalogDefinitions.State targetState,
            long expectedVersion) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(targetState, "targetState");
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> currentRows = entityManager.createNativeQuery(
                            "SELECT state, version FROM " + SCHEMA + ".variant_family"
                                    + " WHERE company_id = :company"
                                    + " AND variant_family_id = :identity")
                    .setParameter("company", companyId.value())
                    .setParameter("identity", id.value())
                    .getResultList();
            if (currentRows.isEmpty()) {
                throw new CatalogPersistenceException(
                        CatalogPersistenceCode.DEFINITION_NOT_FOUND);
            }
            Object[] current = currentRows.getFirst();
            CatalogDefinitions.State currentState = state(current[0]);
            long currentVersion = number(current[1]).longValue();
            if (currentVersion != expectedVersion) {
                throw new CatalogPersistenceException(CatalogPersistenceCode.VERSION_CONFLICT);
            }
            if (currentState != targetState) {
                int updated = execute("UPDATE " + SCHEMA + ".variant_family"
                                + " SET state = :state, version = version + 1,"
                                + " updated_at = CURRENT_TIMESTAMP"
                                + " WHERE company_id = :company"
                                + " AND variant_family_id = :identity"
                                + " AND version = :version",
                        Map.of(
                                "state", targetState.name(),
                                "company", companyId.value(),
                                "identity", id.value(),
                                "version", expectedVersion));
                if (updated != 1) {
                    throw new CatalogPersistenceException(
                            CatalogPersistenceCode.VERSION_CONFLICT);
                }
                insertVariantFamilyRevision(
                        companyId, variantFamily(companyId.value(), id.value()));
            }
            return variantFamily(companyId.value(), id.value());
        } catch (CatalogPersistenceException failure) {
            throw failure;
        } catch (PersistenceException failure) {
            throw PostgreSqlCatalogConflictMapper.map(failure);
        }
    }

    @Override
    public CatalogDefinitions.VariantFamily reviseVariantFamily(
            CompanyId companyId,
            VariantFamilyId id,
            String displayName,
            List<CatalogDefinitions.VariantAttribute> attributes,
            long expectedVersion) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(attributes, "attributes");
        try {
            CatalogDefinitions.VariantFamily current = variantFamily(
                    companyId.value(), id.value());
            if (current.version() != expectedVersion) {
                throw new CatalogPersistenceException(CatalogPersistenceCode.VERSION_CONFLICT);
            }
            CatalogDefinitions.VariantFamily revised = new CatalogDefinitions.VariantFamily(
                    id, current.code(), displayName, attributes, current.state(),
                    expectedVersion + 1);
            int updated = execute("UPDATE " + SCHEMA + ".variant_family"
                            + " SET display_name = :name, version = version + 1,"
                            + " updated_at = CURRENT_TIMESTAMP"
                            + " WHERE company_id = :company"
                            + " AND variant_family_id = :identity"
                            + " AND version = :version",
                    Map.of(
                            "name", revised.displayName(),
                            "company", companyId.value(),
                            "identity", id.value(),
                            "version", expectedVersion));
            if (updated != 1) {
                throw new CatalogPersistenceException(CatalogPersistenceCode.VERSION_CONFLICT);
            }
            execute("DELETE FROM " + SCHEMA + ".variant_attribute_definition"
                            + " WHERE company_id = :company"
                            + " AND variant_family_id = :identity",
                    Map.of("company", companyId.value(), "identity", id.value()));
            for (CatalogDefinitions.VariantAttribute attribute : revised.attributes()) {
                execute("INSERT INTO " + SCHEMA + ".variant_attribute_definition "
                                + "(company_id, variant_family_id, attribute_code, display_name, "
                                + "value_type, required, position) "
                                + "VALUES (:company, :id, :code, :name, :type, :required, :position)",
                        Map.of(
                                "company", companyId.value(),
                                "id", id.value(),
                                "code", attribute.code().value(),
                                "name", attribute.displayName(),
                                "type", attribute.valueType().name(),
                                "required", attribute.required(),
                                "position", attribute.position()));
            }
            insertVariantFamilyRevision(companyId, revised);
            return revised;
        } catch (CatalogPersistenceException failure) {
            throw failure;
        } catch (PersistenceException failure) {
            throw PostgreSqlCatalogConflictMapper.map(failure);
        }
    }

    @Override
    public List<CatalogDefinitions.VariantFamilyRevision> variantFamilyHistory(
            CompanyId companyId, VariantFamilyId id) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(id, "id");
        @SuppressWarnings("unchecked")
        List<Object[]> revisions = entityManager.createNativeQuery(
                        "SELECT revision.family_version, revision.display_name, "
                                + "revision.state, revision.family_version = family.version "
                                + "FROM " + SCHEMA + ".variant_family family "
                                + "JOIN " + SCHEMA + ".variant_family_revision revision "
                                + "ON revision.company_id = family.company_id "
                                + "AND revision.variant_family_id = family.variant_family_id "
                                + "WHERE family.company_id = :company "
                                + "AND family.variant_family_id = :identity "
                                + "ORDER BY revision.family_version DESC")
                .setParameter("company", companyId.value())
                .setParameter("identity", id.value())
                .getResultList();
        if (revisions.isEmpty()) {
            throw new CatalogPersistenceException(CatalogPersistenceCode.DEFINITION_NOT_FOUND);
        }
        Map<Long, List<CatalogDefinitions.VariantAttribute>> attributes = new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
        List<Object[]> attributeRows = entityManager.createNativeQuery(
                        "SELECT family_version, attribute_code, display_name, value_type, "
                                + "required, position FROM " + SCHEMA
                                + ".variant_attribute_revision "
                                + "WHERE company_id = :company "
                                + "AND variant_family_id = :identity "
                                + "ORDER BY family_version DESC, position, attribute_code")
                .setParameter("company", companyId.value())
                .setParameter("identity", id.value())
                .getResultList();
        attributeRows.forEach(row -> attributes
                .computeIfAbsent(number(row[0]).longValue(), ignored -> new ArrayList<>())
                .add(new CatalogDefinitions.VariantAttribute(
                        new VariantAttributeCode(string(row[1])), string(row[2]),
                        VariantValueType.valueOf(string(row[3])), (Boolean) row[4],
                        number(row[5]).intValue())));
        return revisions.stream().map(row -> {
            long version = number(row[0]).longValue();
            return new CatalogDefinitions.VariantFamilyRevision(
                    id, version, string(row[1]), attributes.getOrDefault(version, List.of()),
                    state(row[2]), (Boolean) row[3]);
        }).toList();
    }

    private void insertVariantFamilyRevision(
            CompanyId companyId, CatalogDefinitions.VariantFamily family) {
        execute("INSERT INTO " + SCHEMA + ".variant_family_revision "
                        + "(company_id, variant_family_id, family_version, display_name, state) "
                        + "VALUES (:company, :id, :version, :name, :state)",
                Map.of(
                        "company", companyId.value(),
                        "id", family.id().value(),
                        "version", family.version(),
                        "name", family.displayName(),
                        "state", family.state().name()));
        for (CatalogDefinitions.VariantAttribute attribute : family.attributes()) {
            execute("INSERT INTO " + SCHEMA + ".variant_attribute_revision "
                            + "(company_id, variant_family_id, family_version, attribute_code, "
                            + "display_name, value_type, required, position) "
                            + "VALUES (:company, :id, :version, :code, :name, :type, "
                            + ":required, :position)",
                    Map.of(
                            "company", companyId.value(),
                            "id", family.id().value(),
                            "version", family.version(),
                            "code", attribute.code().value(),
                            "name", attribute.displayName(),
                            "type", attribute.valueType().name(),
                            "required", attribute.required(),
                            "position", attribute.position()));
        }
    }

    private List<CatalogDefinitions.Unit> units(UUID company) {
        return rows("SELECT unit_code, display_name, decimal_scale, state, version " +
                "FROM " + SCHEMA + ".unit_definition WHERE company_id = :company " +
                "ORDER BY unit_code", company).stream().map(row -> new CatalogDefinitions.Unit(
                        new UnitCode(string(row[0])), string(row[1]), number(row[2]).intValue(),
                        state(row[3]), number(row[4]).longValue())).toList();
    }

    private List<CatalogDefinitions.Category> categories(UUID company) {
        return rows("SELECT category_id, parent_category_id, code, display_name, state, version " +
                "FROM " + SCHEMA + ".category_definition WHERE company_id = :company " +
                "ORDER BY code, category_id", company).stream().map(row ->
                        new CatalogDefinitions.Category(
                                new CategoryId(uuid(row[0])),
                                Optional.ofNullable(row[1]).map(value -> new CategoryId(uuid(value))),
                                string(row[2]), string(row[3]), state(row[4]),
                                number(row[5]).longValue())).toList();
    }

    private List<CatalogDefinitions.Brand> brands(UUID company) {
        return rows("SELECT brand_id, code, display_name, state, version " +
                "FROM " + SCHEMA + ".brand_definition WHERE company_id = :company " +
                "ORDER BY code, brand_id", company).stream().map(row ->
                        new CatalogDefinitions.Brand(
                                new BrandId(uuid(row[0])), string(row[1]), string(row[2]),
                                state(row[3]), number(row[4]).longValue())).toList();
    }

    private List<CatalogDefinitions.Tag> tags(UUID company) {
        return rows("SELECT tag_id, code, display_name, state, version " +
                "FROM " + SCHEMA + ".tag_definition WHERE company_id = :company " +
                "ORDER BY code, tag_id", company).stream().map(row ->
                        new CatalogDefinitions.Tag(
                                new TagId(uuid(row[0])), string(row[1]), string(row[2]),
                                state(row[3]), number(row[4]).longValue())).toList();
    }

    private List<CatalogDefinitions.TaxProfile> taxProfiles(UUID company) {
        return rows("SELECT profile.tax_profile_id, profile.code, profile.display_name, " +
                "revision.internal_kind_code, revision.description, " +
                "EXTRACT(EPOCH FROM revision.valid_from), " +
                "EXTRACT(EPOCH FROM revision.valid_until), profile.state, profile.version " +
                "FROM " + SCHEMA + ".tax_profile profile " +
                "JOIN " + SCHEMA + ".tax_profile_revision revision " +
                "ON revision.company_id = profile.company_id " +
                "AND revision.tax_profile_id = profile.tax_profile_id " +
                "AND revision.profile_version = profile.version " +
                "WHERE profile.company_id = :company ORDER BY profile.code, profile.tax_profile_id",
                company).stream().map(row -> new CatalogDefinitions.TaxProfile(
                        new TaxProfileId(uuid(row[0])), string(row[1]), string(row[2]),
                        string(row[3]), string(row[4]), epoch(row[5]),
                        Optional.ofNullable(row[6]).map(JpaCatalogDefinitionRepository::epoch),
                        state(row[7]), number(row[8]).longValue())).toList();
    }

    private CatalogDefinitions.TaxProfile taxProfile(UUID company, UUID id) {
        @SuppressWarnings("unchecked")
        List<Object[]> profiles = entityManager.createNativeQuery(
                        "SELECT profile.tax_profile_id, profile.code, profile.display_name, "
                                + "revision.internal_kind_code, revision.description, "
                                + "EXTRACT(EPOCH FROM revision.valid_from), "
                                + "EXTRACT(EPOCH FROM revision.valid_until), "
                                + "profile.state, profile.version "
                                + "FROM " + SCHEMA + ".tax_profile profile "
                                + "JOIN " + SCHEMA + ".tax_profile_revision revision "
                                + "ON revision.company_id = profile.company_id "
                                + "AND revision.tax_profile_id = profile.tax_profile_id "
                                + "AND revision.profile_version = profile.version "
                                + "WHERE profile.company_id = :company "
                                + "AND profile.tax_profile_id = :identity")
                .setParameter("company", company)
                .setParameter("identity", id)
                .getResultList();
        if (profiles.isEmpty()) {
            throw new CatalogPersistenceException(CatalogPersistenceCode.DEFINITION_NOT_FOUND);
        }
        Object[] row = profiles.getFirst();
        return new CatalogDefinitions.TaxProfile(
                new TaxProfileId(uuid(row[0])), string(row[1]), string(row[2]),
                string(row[3]), string(row[4]), epoch(row[5]),
                Optional.ofNullable(row[6]).map(JpaCatalogDefinitionRepository::epoch),
                state(row[7]), number(row[8]).longValue());
    }

    private List<CatalogDefinitions.VariantFamily> variantFamilies(UUID company) {
        Map<UUID, List<CatalogDefinitions.VariantAttribute>> attributes = new LinkedHashMap<>();
        rows("SELECT variant_family_id, attribute_code, display_name, value_type, required, position " +
                "FROM " + SCHEMA + ".variant_attribute_definition WHERE company_id = :company " +
                "ORDER BY variant_family_id, position, attribute_code", company).forEach(row ->
                        attributes.computeIfAbsent(uuid(row[0]), ignored -> new ArrayList<>())
                                .add(new CatalogDefinitions.VariantAttribute(
                                        new VariantAttributeCode(string(row[1])), string(row[2]),
                                        VariantValueType.valueOf(string(row[3])),
                                        (Boolean) row[4], number(row[5]).intValue())));
        return rows("SELECT variant_family_id, code, display_name, state, version " +
                "FROM " + SCHEMA + ".variant_family WHERE company_id = :company " +
                "ORDER BY code, variant_family_id", company).stream().map(row -> {
                    UUID id = uuid(row[0]);
                    return new CatalogDefinitions.VariantFamily(
                            new VariantFamilyId(id), string(row[1]), string(row[2]),
                            attributes.getOrDefault(id, List.of()), state(row[3]),
                            number(row[4]).longValue());
                }).toList();
    }

    private List<CatalogDefinitions.ReplacementLink> replacementLinks(UUID company) {
        List<CatalogDefinitions.ReplacementLink> links = new ArrayList<>();
        replacementRows(
                company, CatalogDefinitions.SimpleKind.UNIT,
                "unit_definition", "unit_code", "replacement_unit_code", links);
        replacementRows(
                company, CatalogDefinitions.SimpleKind.CATEGORY,
                "category_definition", "category_id", "replacement_category_id", links);
        replacementRows(
                company, CatalogDefinitions.SimpleKind.BRAND,
                "brand_definition", "brand_id", "replacement_brand_id", links);
        replacementRows(
                company, CatalogDefinitions.SimpleKind.TAG,
                "tag_definition", "tag_id", "replacement_tag_id", links);
        return List.copyOf(links);
    }

    private void replacementRows(
            UUID company,
            CatalogDefinitions.SimpleKind kind,
            String table,
            String identityColumn,
            String replacementColumn,
            List<CatalogDefinitions.ReplacementLink> target) {
        rows("SELECT " + identityColumn + ", " + replacementColumn
                        + " FROM " + SCHEMA + "." + table
                        + " WHERE company_id = :company AND " + replacementColumn
                        + " IS NOT NULL ORDER BY " + identityColumn,
                company).stream().map(row -> new CatalogDefinitions.ReplacementLink(
                        kind, string(row[0]), string(row[1]))).forEach(target::add);
    }

    private boolean hasReplacement(
            CompanyId companyId,
            CatalogDefinitions.SimpleKind kind,
            DefinitionStorage storage) {
        Number count = (Number) entityManager.createNativeQuery(
                        "SELECT count(*) FROM " + SCHEMA + "." + storage.table()
                                + " WHERE company_id = :company AND "
                                + storage.identityColumn() + " = :identity AND "
                                + replacementColumn(kind) + " IS NOT NULL")
                .setParameter("company", companyId.value())
                .setParameter("identity", storage.identity())
                .getSingleResult();
        return count.longValue() == 1;
    }

    private CatalogDefinitions.VariantFamily variantFamily(UUID company, UUID id) {
        return variantFamilies(company).stream()
                .filter(family -> family.id().value().equals(id))
                .findFirst()
                .orElseThrow(() -> new CatalogPersistenceException(
                        CatalogPersistenceCode.DEFINITION_NOT_FOUND));
    }

    private CatalogDefinitions.SimpleRevision currentSimpleRevision(
            CompanyId companyId,
            CatalogDefinitions.SimpleKind kind,
            String identity) {
        DefinitionStorage storage = storage(kind, identity);
        String columns = switch (kind) {
            case UNIT -> "display_name, decimal_scale, NULL, state, version";
            case CATEGORY -> "display_name, NULL, parent_category_id, state, version";
            case BRAND, TAG -> "display_name, NULL, NULL, state, version";
        };
        @SuppressWarnings("unchecked")
        List<Object[]> values = entityManager.createNativeQuery(
                        "SELECT " + columns + " FROM " + SCHEMA + "." + storage.table()
                                + " WHERE company_id = :company AND "
                                + storage.identityColumn() + " = :identity")
                .setParameter("company", companyId.value())
                .setParameter("identity", storage.identity())
                .getResultList();
        if (values.isEmpty()) {
            throw new CatalogPersistenceException(CatalogPersistenceCode.DEFINITION_NOT_FOUND);
        }
        Object[] row = values.getFirst();
        return new CatalogDefinitions.SimpleRevision(
                kind,
                storage.canonicalIdentity(),
                number(row[4]).longValue(),
                string(row[0]),
                Optional.ofNullable(row[1]).map(value -> number(value).intValue()),
                Optional.ofNullable(row[2]).map(value -> new CategoryId(uuid(value))),
                state(row[3]),
                true);
    }

    private void insertSimpleRevision(
            CompanyId companyId, CatalogDefinitions.SimpleRevision revision) {
        DefinitionStorage storage = storage(revision.kind(), revision.identity());
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("company", companyId.value());
        parameters.put("identity", storage.identity());
        parameters.put("version", revision.version());
        parameters.put("name", revision.displayName());
        parameters.put("state", revision.state().name());
        String columns;
        String values;
        switch (revision.kind()) {
            case UNIT -> {
                parameters.put("scale", revision.decimalScale().orElseThrow());
                columns = ", decimal_scale";
                values = ", :scale";
            }
            case CATEGORY -> {
                parameters.put("parent", revision.parentId()
                        .map(CategoryId::value).orElse(null));
                columns = ", parent_category_id";
                values = ", :parent";
            }
            case BRAND, TAG -> {
                columns = "";
                values = "";
            }
            default -> throw new IllegalStateException("Unsupported simple definition kind");
        }
        execute("INSERT INTO " + SCHEMA + "." + revisionTable(revision.kind())
                        + " (company_id, " + storage.identityColumn()
                        + ", definition_version, display_name, state" + columns + ")"
                        + " VALUES (:company, :identity, :version, :name, :state"
                        + values + ")",
                parameters);
    }

    private void insertReplacement(
            CompanyId companyId, CatalogDefinitions.ReplacementCandidate replacement) {
        switch (replacement.kind()) {
            case UNIT -> insert(companyId, new CatalogDefinitions.Unit(
                    new UnitCode(replacement.code()), replacement.displayName(),
                    replacement.decimalScale().orElseThrow(),
                    CatalogDefinitions.State.ACTIVE, 0));
            case CATEGORY -> insert(companyId, new CatalogDefinitions.Category(
                    new CategoryId(UUID.fromString(replacement.identity())),
                    replacement.parentId(), replacement.code(), replacement.displayName(),
                    CatalogDefinitions.State.ACTIVE, 0));
            case BRAND -> insert(companyId, new CatalogDefinitions.Brand(
                    new BrandId(UUID.fromString(replacement.identity())), replacement.code(),
                    replacement.displayName(), CatalogDefinitions.State.ACTIVE, 0));
            case TAG -> insert(companyId, new CatalogDefinitions.Tag(
                    new TagId(UUID.fromString(replacement.identity())), replacement.code(),
                    replacement.displayName(), CatalogDefinitions.State.ACTIVE, 0));
        }
    }

    private void requireActiveCategoryParent(
            UUID company, CategoryId parentId, DefinitionStorage source) {
        if (source.identity().equals(parentId.value())) {
            throw new IllegalArgumentException(
                    "The replaced category cannot be the replacement parent");
        }
        @SuppressWarnings("unchecked")
        List<Object> states = entityManager.createNativeQuery(
                        "SELECT state FROM " + SCHEMA + ".category_definition"
                                + " WHERE company_id = :company AND category_id = :parent"
                                + " FOR SHARE")
                .setParameter("company", company)
                .setParameter("parent", parentId.value())
                .getResultList();
        if (states.isEmpty() || state(states.getFirst()) != CatalogDefinitions.State.ACTIVE) {
            throw new CatalogPersistenceException(CatalogPersistenceCode.REFERENCE_CONFLICT);
        }
    }

    private static String revisionTable(CatalogDefinitions.SimpleKind kind) {
        return switch (kind) {
            case UNIT -> "unit_definition_revision";
            case CATEGORY -> "category_definition_revision";
            case BRAND -> "brand_definition_revision";
            case TAG -> "tag_definition_revision";
        };
    }

    private static String replacementColumn(CatalogDefinitions.SimpleKind kind) {
        return switch (kind) {
            case UNIT -> "replacement_unit_code";
            case CATEGORY -> "replacement_category_id";
            case BRAND -> "replacement_brand_id";
            case TAG -> "replacement_tag_id";
        };
    }

    private int execute(String sql, Map<String, Object> parameters) {
        Query query = entityManager.createNativeQuery(sql);
        parameters.forEach(query::setParameter);
        return query.executeUpdate();
    }

    private static DefinitionStorage storage(
            CatalogDefinitions.SimpleKind kind, String identity) {
        return switch (kind) {
            case UNIT -> {
                String code = new UnitCode(identity).value();
                yield new DefinitionStorage(
                        "unit_definition", "unit_code", code, code);
            }
            case CATEGORY -> uuidStorage(
                    "category_definition", "category_id", identity);
            case BRAND -> uuidStorage("brand_definition", "brand_id", identity);
            case TAG -> uuidStorage("tag_definition", "tag_id", identity);
        };
    }

    private static DefinitionStorage uuidStorage(
            String table, String identityColumn, String identity) {
        UUID id = UUID.fromString(identity);
        return new DefinitionStorage(table, identityColumn, id, id.toString());
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql, UUID company) {
        return entityManager.createNativeQuery(sql)
                .setParameter("company", company)
                .getResultList();
    }

    private static Map<String, Object> common(
            UUID company,
            UUID id,
            String code,
            String name,
            CatalogDefinitions.State state,
            long version) {
        return Map.of(
                "company", company,
                "id", id,
                "code", code,
                "name", name,
                "state", state.name(),
                "version", version);
    }

    private static UUID uuid(Object value) {
        return value instanceof UUID uuid ? uuid : UUID.fromString(string(value));
    }

    private static String string(Object value) {
        return Objects.requireNonNull(value, "database value").toString();
    }

    private static Number number(Object value) {
        return (Number) Objects.requireNonNull(value, "database number");
    }

    private static CatalogDefinitions.State state(Object value) {
        return CatalogDefinitions.State.valueOf(string(value));
    }

    private static Instant epoch(Object value) {
        BigDecimal epoch = new BigDecimal(string(value));
        long seconds = epoch.longValue();
        int nanos = epoch.subtract(BigDecimal.valueOf(seconds))
                .movePointRight(9).intValue();
        return Instant.ofEpochSecond(seconds, nanos);
    }

    private record DefinitionStorage(
            String table,
            String identityColumn,
            Object identity,
            String canonicalIdentity) {
    }
}
