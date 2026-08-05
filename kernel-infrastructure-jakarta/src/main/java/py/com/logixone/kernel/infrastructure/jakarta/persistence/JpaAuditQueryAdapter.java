package py.com.logixone.kernel.infrastructure.jakarta.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import py.com.logixone.kernel.application.audit.admin.AuditEventView;
import py.com.logixone.kernel.application.audit.admin.AuditPage;
import py.com.logixone.kernel.application.audit.admin.AuditQuery;
import py.com.logixone.kernel.application.audit.port.AuditQueryPort;

/** Bounded JPA query whose predicates are selected only from neutral closed filters. */
@ApplicationScoped
@Transactional
public class JpaAuditQueryAdapter implements AuditQueryPort {

    @PersistenceContext(unitName = CorePersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaAuditQueryAdapter() {
    }

    JpaAuditQueryAdapter(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public AuditPage query(AuditQuery query) {
        Objects.requireNonNull(query, "query");
        StringBuilder jpql = new StringBuilder("SELECT event FROM AuditEventEntity event WHERE 1 = 1");
        List<Parameter> parameters = new ArrayList<>();

        query.category().ifPresent(category -> {
            jpql.append(" AND event.category = :category");
            parameters.add(new Parameter("category", category));
        });
        query.outcome().ifPresent(outcome -> {
            jpql.append(" AND event.outcome = :outcome");
            parameters.add(new Parameter("outcome", outcome));
        });
        query.timeWindow().lowerBound(Instant.now()).ifPresent(lowerBound -> {
            jpql.append(" AND event.occurredAt >= :lowerBound");
            parameters.add(new Parameter("lowerBound", lowerBound));
        });
        query.companyId().ifPresent(companyId -> {
            jpql.append(" AND event.companyId = :companyId");
            parameters.add(new Parameter("companyId", companyId.value()));
        });
        query.correlationId().ifPresent(correlationId -> {
            jpql.append(" AND event.correlationId = :correlationId");
            parameters.add(new Parameter("correlationId", correlationId));
        });
        jpql.append(" ORDER BY event.occurredAt DESC, event.auditEventId DESC");

        TypedQuery<AuditEventEntity> persistenceQuery =
                entityManager.createQuery(jpql.toString(), AuditEventEntity.class);
        parameters.forEach(parameter ->
                persistenceQuery.setParameter(parameter.name(), parameter.value()));
        int offset = Math.multiplyExact(query.page(), query.pageSize());
        List<AuditEventEntity> found = persistenceQuery
                .setFirstResult(offset)
                .setMaxResults(query.pageSize() + 1)
                .getResultList();
        boolean hasNext = found.size() > query.pageSize();
        List<AuditEventView> events = found.stream()
                .limit(query.pageSize())
                .map(AuditEventEntity::toView)
                .toList();
        return new AuditPage(events, query.page(), query.pageSize(), hasNext);
    }

    private record Parameter(String name, Object value) {
    }
}
