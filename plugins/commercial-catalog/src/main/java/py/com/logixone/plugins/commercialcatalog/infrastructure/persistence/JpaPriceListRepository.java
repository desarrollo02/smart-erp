package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import java.util.*;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.api.PriceListId;
import py.com.logixone.plugins.commercialcatalog.application.port.*;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSearchPage;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSummary;
import py.com.logixone.plugins.commercialcatalog.domain.*;

@ApplicationScoped
@Transactional
public class JpaPriceListRepository implements PriceListRepository {
    @PersistenceContext(unitName=CommercialCatalogPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;
    public JpaPriceListRepository() { }
    JpaPriceListRepository(EntityManager entityManager) { this.entityManager=Objects.requireNonNull(entityManager,"entityManager"); }

    @Override public Optional<PriceList> findById(CompanyId companyId, PriceListId priceListId) {
        Objects.requireNonNull(companyId,"companyId"); Objects.requireNonNull(priceListId,"priceListId");
        PriceListEntity entity=entityManager.find(PriceListEntity.class,new PriceListEntity.Key(companyId.value(),priceListId.value()));
        return Optional.ofNullable(entity).map(this::restore);
    }

    @Override
    public PriceListSearchPage search(CompanyId companyId, PriceListSearchCriteria criteria) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(criteria, "criteria");
        StringBuilder clause = new StringBuilder(
                "FROM PriceListEntity list WHERE list.companyId = :company");
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (!criteria.query().isEmpty()) {
            clause.append(" AND (LOWER(list.code) LIKE :query ")
                    .append("OR LOWER(list.displayName) LIKE :query)");
            parameters.put("query", "%" + criteria.query().toLowerCase(Locale.ROOT) + "%");
        }
        if (!criteria.states().isEmpty()) {
            clause.append(" AND list.state IN :states");
            parameters.put("states", criteria.states());
        }
        var pageQuery = entityManager.createQuery(
                        "SELECT list " + clause +
                                " ORDER BY LOWER(list.displayName), list.priceListId",
                        PriceListEntity.class)
                .setParameter("company", companyId.value())
                .setFirstResult(criteria.offset())
                .setMaxResults(criteria.limit());
        var countQuery = entityManager.createQuery(
                        "SELECT COUNT(list) " + clause, Long.class)
                .setParameter("company", companyId.value());
        parameters.forEach((name, value) -> {
            pageQuery.setParameter(name, value);
            countQuery.setParameter(name, value);
        });
        List<PriceListEntity> roots = pageQuery.getResultList();
        Map<UUID, EntryCounts> counts = entryCounts(companyId.value(), roots);
        List<PriceListSummary> summaries = roots.stream().map(root -> {
            EntryCounts values = counts.getOrDefault(root.priceListId(), new EntryCounts(0, 0));
            return new PriceListSummary(
                    new PriceListId(root.priceListId()), new PriceListCode(root.code()),
                    new PriceListName(root.displayName()), root.currencyCode(), root.taxMode(),
                    root.state(), values.total(), values.active(), root.version());
        }).toList();
        return new PriceListSearchPage(
                summaries, countQuery.getSingleResult(), criteria.offset(), criteria.limit());
    }

    @Override public PriceList insert(PriceList list) {
        Objects.requireNonNull(list,"list"); PriceListSnapshot snapshot=list.snapshot();
        PriceListEntity entity=PriceListEntity.from(snapshot);
        try { entityManager.persist(entity); entityManager.flush(); snapshot.entries().forEach(value -> entityManager.persist(PriceEntryEntity.from(snapshot.companyId().value(),snapshot.id().value(),value))); entityManager.flush(); return restore(entity); }
        catch(PersistenceException failure){ throw PostgreSqlCatalogConflictMapper.map(failure); }
    }

    @Override public PriceList update(PriceList list,long expectedPersistedVersion) {
        Objects.requireNonNull(list,"list");
        if(expectedPersistedVersion<0 || list.version()!=expectedPersistedVersion+1) throw new CatalogPersistenceException(CatalogPersistenceCode.VERSION_CONFLICT);
        PriceListSnapshot snapshot=list.snapshot();
        PriceListEntity entity=entityManager.find(PriceListEntity.class,new PriceListEntity.Key(snapshot.companyId().value(),snapshot.id().value()));
        if(entity==null) throw new CatalogPersistenceException(CatalogPersistenceCode.PRICE_LIST_NOT_FOUND);
        if(entity.version()!=expectedPersistedVersion) throw new CatalogPersistenceException(CatalogPersistenceCode.VERSION_CONFLICT);
        try { entity.apply(snapshot); snapshot.entries().forEach(value -> {
            PriceEntryKey key=new PriceEntryKey(snapshot.companyId().value(),snapshot.id().value(),value.id().value());
            PriceEntryEntity existing=entityManager.find(PriceEntryEntity.class,key);
            if(existing==null) entityManager.persist(PriceEntryEntity.from(snapshot.companyId().value(),snapshot.id().value(),value)); else existing.apply(value);
        }); entityManager.flush(); return restore(entity); }
        catch(OptimisticLockException failure){ throw new CatalogPersistenceException(CatalogPersistenceCode.VERSION_CONFLICT,failure); }
        catch(PersistenceException failure){ throw PostgreSqlCatalogConflictMapper.map(failure); }
    }

    private PriceList restore(PriceListEntity root) {
        List<PriceEntry> entries=entityManager.createQuery("SELECT record FROM PriceEntryEntity record WHERE record.companyId = :company AND record.priceListId = :list ORDER BY record.validFrom, record.priceEntryId",PriceEntryEntity.class)
                .setParameter("company",root.companyId()).setParameter("list",root.priceListId()).getResultList().stream().map(PriceEntryEntity::toDomain).toList();
        return PriceList.restore(new PriceListSnapshot(new CompanyId(root.companyId()),new PriceListId(root.priceListId()),new PriceListCode(root.code()),new PriceListName(root.displayName()),root.currencyCode(),root.taxMode(),root.amountScale(),root.roundingMode(),root.state(),entries,root.version()));
    }

    private Map<UUID, EntryCounts> entryCounts(
            UUID companyId, List<PriceListEntity> roots) {
        if (roots.isEmpty()) {
            return Map.of();
        }
        Set<UUID> ids = roots.stream().map(PriceListEntity::priceListId)
                .collect(java.util.stream.Collectors.toSet());
        Map<UUID, EntryCounts> result = new HashMap<>();
        entityManager.createQuery(
                        "SELECT priceEntry.priceListId, COUNT(priceEntry), " +
                                "SUM(CASE WHEN priceEntry.active = true THEN 1 ELSE 0 END) " +
                                "FROM PriceEntryEntity priceEntry " +
                                "WHERE priceEntry.companyId = :company " +
                                "AND priceEntry.priceListId IN :lists " +
                                "GROUP BY priceEntry.priceListId",
                        Object[].class)
                .setParameter("company", companyId)
                .setParameter("lists", ids)
                .getResultList()
                .forEach(row -> result.put((UUID) row[0], new EntryCounts(
                        ((Number) row[1]).longValue(), ((Number) row[2]).longValue())));
        return result;
    }

    private record EntryCounts(long total, long active) {
    }
}
