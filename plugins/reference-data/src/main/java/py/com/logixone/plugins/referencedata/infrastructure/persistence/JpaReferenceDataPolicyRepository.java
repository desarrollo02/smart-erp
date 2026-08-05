package py.com.logixone.plugins.referencedata.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AppUserId;
import py.com.logixone.plugins.referencedata.api.ReferenceDataCatalog;
import py.com.logixone.plugins.referencedata.application.policy.ChangeReferenceDataPolicy;
import py.com.logixone.plugins.referencedata.application.policy.ConcurrentReferenceDataPolicyChangeException;
import py.com.logixone.plugins.referencedata.application.policy.ReferenceDataPolicy;
import py.com.logixone.plugins.referencedata.application.policy.ReferenceDataPolicyRepository;
import py.com.logixone.plugins.referencedata.application.policy.ReferenceDataPolicyRevision;

/** Native adapter over plugin-private current policy and append-only history tables. */
@ApplicationScoped
@Transactional
public class JpaReferenceDataPolicyRepository implements ReferenceDataPolicyRepository {

    private static final String SCHEMA = ReferenceDataPersistenceNames.SCHEMA;

    @PersistenceContext(unitName = ReferenceDataPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaReferenceDataPolicyRepository() {
    }

    JpaReferenceDataPolicyRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public boolean existsInCurrentRelease(ReferenceDataCatalog catalog, String code) {
        Objects.requireNonNull(catalog, "catalog");
        String canonical = ReferenceDataPolicy.canonicalCode(catalog, code);
        CatalogSql sql = CatalogSql.forCatalog(catalog);
        Object result = entityManager.createNativeQuery(
                        "SELECT EXISTS (SELECT 1 FROM " + SCHEMA + ".catalog_release r "
                                + "JOIN " + SCHEMA + "." + sql.entryTable() + " e "
                                + "ON e.catalog_kind = r.catalog_kind "
                                + "AND e.release_id = r.release_id "
                                + "WHERE r.catalog_kind = :catalog AND r.current_release "
                                + "AND e." + sql.codeColumn() + " = :code)")
                .setParameter("catalog", catalog.name())
                .setParameter("code", canonical)
                .getSingleResult();
        return Boolean.TRUE.equals(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<ReferenceDataPolicy> find(
            CompanyId companyId, ReferenceDataCatalog catalog, String code) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(catalog, "catalog");
        String canonical = ReferenceDataPolicy.canonicalCode(catalog, code);
        CatalogSql sql = CatalogSql.forCatalog(catalog);
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT enabled, version FROM " + SCHEMA + "." + sql.policyTable()
                                + " WHERE company_id = :company AND " + sql.codeColumn()
                                + " = :code")
                .setParameter("company", companyId.value())
                .setParameter("code", canonical)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.getFirst();
        return Optional.of(new ReferenceDataPolicy(
                companyId, catalog, canonical, (Boolean) row[0], number(row[1]).longValue()));
    }

    @Override
    public ReferenceDataPolicy change(
            CompanyId companyId,
            ChangeReferenceDataPolicy command,
            AppUserId actorUserId,
            String correlationId,
            Instant changedAt) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(actorUserId, "actorUserId");
        Objects.requireNonNull(changedAt, "changedAt");
        CatalogSql sql = CatalogSql.forCatalog(command.catalog());
        int changed = updateExisting(companyId, command, changedAt, sql);
        if (changed == 0 && command.expectedVersion() == 0) {
            changed = insertFirst(companyId, command, changedAt, sql);
        }
        if (changed != 1) {
            throw new ConcurrentReferenceDataPolicyChangeException();
        }

        long nextVersion = command.expectedVersion() + 1;
        entityManager.createNativeQuery(
                        "INSERT INTO " + SCHEMA + ".company_reference_policy_history "
                                + "(company_id, catalog_kind, reference_code, version, enabled, "
                                + "actor_user_id, correlation_id, changed_at) VALUES "
                                + "(:company, :catalog, :code, :version, :enabled, :actor, "
                                + ":correlation, :changedAt)")
                .setParameter("company", companyId.value())
                .setParameter("catalog", command.catalog().name())
                .setParameter("code", command.code())
                .setParameter("version", nextVersion)
                .setParameter("enabled", command.enabled())
                .setParameter("actor", actorUserId.value())
                .setParameter("correlation", correlationId)
                .setParameter("changedAt", changedAt)
                .executeUpdate();
        return new ReferenceDataPolicy(
                companyId, command.catalog(), command.code(), command.enabled(), nextVersion);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ReferenceDataPolicyRevision> history(
            CompanyId companyId, ReferenceDataCatalog catalog, String code) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(catalog, "catalog");
        String canonical = ReferenceDataPolicy.canonicalCode(catalog, code);
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT enabled, version, actor_user_id, correlation_id, changed_at "
                                + "FROM " + SCHEMA + ".company_reference_policy_history "
                                + "WHERE company_id = :company AND catalog_kind = :catalog "
                                + "AND reference_code = :code ORDER BY version DESC")
                .setParameter("company", companyId.value())
                .setParameter("catalog", catalog.name())
                .setParameter("code", canonical)
                .getResultList();
        return rows.stream().map(row -> new ReferenceDataPolicyRevision(
                companyId,
                catalog,
                canonical,
                (Boolean) row[0],
                number(row[1]).longValue(),
                new AppUserId((java.util.UUID) row[2]),
                Objects.toString(row[3]),
                instant(row[4]))).toList();
    }

    private int updateExisting(
            CompanyId companyId,
            ChangeReferenceDataPolicy command,
            Instant changedAt,
            CatalogSql sql) {
        return entityManager.createNativeQuery(
                        "UPDATE " + SCHEMA + "." + sql.policyTable() + " SET enabled = :enabled, "
                                + "version = version + 1, updated_at = :changedAt "
                                + "WHERE company_id = :company AND " + sql.codeColumn()
                                + " = :code AND version = :expected")
                .setParameter("enabled", command.enabled())
                .setParameter("changedAt", changedAt)
                .setParameter("company", companyId.value())
                .setParameter("code", command.code())
                .setParameter("expected", command.expectedVersion())
                .executeUpdate();
    }

    private int insertFirst(
            CompanyId companyId,
            ChangeReferenceDataPolicy command,
            Instant changedAt,
            CatalogSql sql) {
        return entityManager.createNativeQuery(
                        "INSERT INTO " + SCHEMA + "." + sql.policyTable()
                                + " (company_id, " + sql.codeColumn()
                                + ", enabled, version, updated_at) "
                                + "VALUES (:company, :code, :enabled, 1, :changedAt) "
                                + "ON CONFLICT (company_id, " + sql.codeColumn() + ") DO NOTHING")
                .setParameter("company", companyId.value())
                .setParameter("code", command.code())
                .setParameter("enabled", command.enabled())
                .setParameter("changedAt", changedAt)
                .executeUpdate();
    }

    private static Number number(Object value) {
        return (Number) value;
    }

    private static Instant instant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        return Instant.parse(Objects.toString(value));
    }

    private record CatalogSql(String entryTable, String policyTable, String codeColumn) {

        private static CatalogSql forCatalog(ReferenceDataCatalog catalog) {
            return switch (catalog) {
                case COUNTRY -> new CatalogSql(
                        "country_entry", "company_country_policy", "alpha2_code");
                case CURRENCY -> new CatalogSql(
                        "currency_entry", "company_currency_policy", "alphabetic_code");
            };
        }
    }
}
