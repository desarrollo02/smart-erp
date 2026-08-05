package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.util.Locale;
import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogCodeSequenceRepository;

@ApplicationScoped
@Transactional
public class JpaCatalogCodeSequenceRepository implements CatalogCodeSequenceRepository {
    private static final String NEXT_SQL = """
            INSERT INTO plg_commercial_catalog.catalog_code_sequence
                (company_id, sequence_scope, next_value, updated_at)
            VALUES (:company, :scope, 2, CURRENT_TIMESTAMP)
            ON CONFLICT (company_id, sequence_scope)
            DO UPDATE SET next_value = plg_commercial_catalog.catalog_code_sequence.next_value + 1,
                          updated_at = CURRENT_TIMESTAMP
            RETURNING next_value - 1
            """;

    @PersistenceContext(unitName = CommercialCatalogPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaCatalogCodeSequenceRepository() {
    }

    JpaCatalogCodeSequenceRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public long next(CompanyId companyId, String scope) {
        Objects.requireNonNull(companyId, "companyId");
        String normalizedScope = normalizeScope(scope);
        try {
            Number result = (Number) entityManager.createNativeQuery(NEXT_SQL)
                    .setParameter("company", companyId.value())
                    .setParameter("scope", normalizedScope)
                    .getSingleResult();
            return result.longValue();
        } catch (PersistenceException failure) {
            throw PostgreSqlCatalogConflictMapper.map(failure);
        }
    }

    private static String normalizeScope(String scope) {
        String normalized = Objects.requireNonNull(scope, "scope").trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9][A-Z0-9_.-]{0,63}")) {
            throw new IllegalArgumentException("scope must be a stable code with at most 64 characters");
        }
        return normalized;
    }
}
