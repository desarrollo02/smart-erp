package py.com.logixone.plugins.commercialcatalog.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.api.*;
import py.com.logixone.plugins.commercialcatalog.application.port.*;
import py.com.logixone.plugins.commercialcatalog.domain.*;

@ApplicationScoped
@Transactional
public class JpaCatalogItemRepository implements CatalogItemRepository {
    @PersistenceContext(unitName = CommercialCatalogPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaCatalogItemRepository() { }
    JpaCatalogItemRepository(EntityManager entityManager) { this.entityManager=Objects.requireNonNull(entityManager, "entityManager"); }

    @Override
    public Optional<CatalogItem> findById(CompanyId companyId, CatalogItemId itemId) {
        Objects.requireNonNull(companyId, "companyId"); Objects.requireNonNull(itemId, "itemId");
        CatalogItemEntity entity=entityManager.find(CatalogItemEntity.class,
                new CatalogItemEntity.Key(companyId.value(), itemId.value()));
        return Optional.ofNullable(entity).map(this::restore);
    }

    @Override
    public CatalogSearchPage search(CompanyId companyId, CatalogSearchCriteria criteria) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(criteria, "criteria");
        SearchQuery query = searchQuery(companyId, criteria);
        var pageQuery = entityManager.createQuery(
                        "SELECT item " + query.clause() +
                                " ORDER BY LOWER(item.displayName), item.catalogItemId",
                        CatalogItemEntity.class)
                .setParameter("company", companyId.value())
                .setFirstResult(criteria.offset())
                .setMaxResults(criteria.limit());
        var countQuery = entityManager.createQuery(
                        "SELECT COUNT(item) " + query.clause(), Long.class)
                .setParameter("company", companyId.value());
        query.parameters().forEach((name, value) -> {
            pageQuery.setParameter(name, value);
            countQuery.setParameter(name, value);
        });
        List<CatalogItemEntity> roots = pageQuery.getResultList();
        Map<UUID, Set<CatalogItemScope>> scopes = scopes(companyId.value(), roots);
        List<CatalogItemReference> references = roots.stream()
                .map(root -> new CatalogItemReference(
                        new CatalogItemId(root.catalogItemId()), root.code(), root.displayName(),
                        root.itemType(), root.state(), scopes.getOrDefault(
                                root.catalogItemId(), Set.of()), root.baseUnitCode(), root.version()))
                .toList();
        return new CatalogSearchPage(
                references, countQuery.getSingleResult(), criteria.offset(), criteria.limit());
    }

    @Override
    public CatalogItem insert(CatalogItem item) {
        Objects.requireNonNull(item, "item");
        CatalogItemSnapshot snapshot=item.snapshot();
        CatalogItemEntity entity=CatalogItemEntity.from(snapshot);
        try {
            entityManager.persist(entity);
            entityManager.flush();
            persistDetails(snapshot);
            entityManager.flush();
            return restore(entity);
        } catch (PersistenceException failure) {
            throw PostgreSqlCatalogConflictMapper.map(failure);
        }
    }

    @Override
    public CatalogItem update(CatalogItem item, long expectedPersistedVersion) {
        Objects.requireNonNull(item, "item");
        if (expectedPersistedVersion < 0 || item.version() != expectedPersistedVersion + 1) {
            throw new CatalogPersistenceException(CatalogPersistenceCode.VERSION_CONFLICT);
        }
        CatalogItemSnapshot snapshot=item.snapshot();
        CatalogItemEntity entity=entityManager.find(CatalogItemEntity.class,
                new CatalogItemEntity.Key(snapshot.companyId().value(), snapshot.id().value()));
        if (entity == null) { throw new CatalogPersistenceException(CatalogPersistenceCode.ITEM_NOT_FOUND); }
        if (entity.version() != expectedPersistedVersion) {
            throw new CatalogPersistenceException(CatalogPersistenceCode.VERSION_CONFLICT);
        }
        try {
            entity.apply(snapshot);
            synchronizeDetails(snapshot);
            entityManager.flush();
            return restore(entity);
        } catch (OptimisticLockException failure) {
            throw new CatalogPersistenceException(CatalogPersistenceCode.VERSION_CONFLICT, failure);
        } catch (PersistenceException failure) {
            throw PostgreSqlCatalogConflictMapper.map(failure);
        }
    }

    private CatalogItem restore(CatalogItemEntity root) {
        UUID company=root.companyId(); UUID item=root.catalogItemId();
        List<CatalogItemCategoryEntity> categoryRows=owned(CatalogItemCategoryEntity.class, company, item, "record.detailId");
        Optional<CatalogClassification> classification=classification(root, categoryRows, company, item);
        Optional<CatalogVariant> variant=variant(company, item);
        Map<String,List<CatalogItemUnitPurposeEntity>> purposes=owned(
                CatalogItemUnitPurposeEntity.class, company, item, "record.unitCode, record.purposeCode")
                .stream().collect(Collectors.groupingBy(CatalogItemUnitPurposeEntity::unitCode));
        List<ItemUnitConversion> conversions=owned(CatalogItemUnitConversionEntity.class, company, item, "record.detailCode")
                .stream().map(conversion -> {
                    Set<UnitPurpose> all=purposes.getOrDefault(conversion.key(), List.of()).stream()
                            .map(CatalogItemUnitPurposeEntity::purpose).collect(Collectors.toUnmodifiableSet());
                    Set<UnitPurpose> defaults=purposes.getOrDefault(conversion.key(), List.of()).stream()
                            .filter(CatalogItemUnitPurposeEntity::isDefault)
                            .map(CatalogItemUnitPurposeEntity::purpose).collect(Collectors.toUnmodifiableSet());
                    return conversion.toDomain(all, defaults);
                }).toList();
        return CatalogItem.restore(new CatalogItemSnapshot(
                new CompanyId(company), new CatalogItemId(item), new CatalogItemCode(root.code()),
                new CatalogItemName(root.displayName()), root.description(), root.itemType(),
                owned(CatalogItemScopeEntity.class, company, item, "record.detailCode").stream()
                        .map(CatalogItemScopeEntity::toDomain).collect(Collectors.toUnmodifiableSet()),
                new UnitCode(root.baseUnitCode()),
                new TaxProfileReference(new TaxProfileId(root.taxProfileId()), root.taxProfileVersion()),
                classification, variant, root.state(),
                Optional.ofNullable(root.replacementItemId()).map(CatalogItemId::new),
                owned(CatalogItemIdentifierEntity.class, company, item, "record.detailId").stream()
                        .map(CatalogItemIdentifierEntity::toDomain).toList(),
                conversions, root.version()));
    }

    private Optional<CatalogClassification> classification(
            CatalogItemEntity root, List<CatalogItemCategoryEntity> categories, UUID company, UUID item) {
        if (categories.isEmpty()) {
            if (root.brandId()!=null || !owned(CatalogItemTagEntity.class, company, item, "record.detailId").isEmpty()) {
                throw new CatalogPersistenceException(CatalogPersistenceCode.REFERENCE_CONFLICT);
            }
            return Optional.empty();
        }
        List<CatalogItemCategoryEntity> primary=categories.stream().filter(CatalogItemCategoryEntity::primary).toList();
        if (primary.size()!=1) { throw new CatalogPersistenceException(CatalogPersistenceCode.REFERENCE_CONFLICT); }
        Set<CategoryId> secondary=categories.stream().filter(value -> !value.primary())
                .map(CatalogItemCategoryEntity::toDomain).collect(Collectors.toUnmodifiableSet());
        Set<TagId> tags=owned(CatalogItemTagEntity.class, company, item, "record.detailId").stream()
                .map(CatalogItemTagEntity::toDomain).collect(Collectors.toUnmodifiableSet());
        return Optional.of(new CatalogClassification(primary.getFirst().toDomain(), secondary,
                Optional.ofNullable(root.brandId()).map(BrandId::new), tags));
    }

    private Optional<CatalogVariant> variant(UUID company, UUID item) {
        List<CatalogItemVariantEntity> roots=owned(CatalogItemVariantEntity.class, company, item, "record.variantFamilyId");
        if (roots.isEmpty()) { return Optional.empty(); }
        if (roots.size()!=1) { throw new CatalogPersistenceException(CatalogPersistenceCode.REFERENCE_CONFLICT); }
        Map<VariantAttributeCode,VariantAttributeValue> attributes=owned(
                CatalogItemVariantAttributeEntity.class, company, item, "record.detailCode").stream()
                .collect(Collectors.toUnmodifiableMap(
                        CatalogItemVariantAttributeEntity::code, CatalogItemVariantAttributeEntity::value));
        return Optional.of(new CatalogVariant(
                roots.getFirst().family(), roots.getFirst().familyVersion(), attributes));
    }

    private void persistDetails(CatalogItemSnapshot snapshot) {
        UUID company=snapshot.companyId().value(), item=snapshot.id().value();
        snapshot.scopes().forEach(value -> entityManager.persist(CatalogItemScopeEntity.from(company,item,value)));
        snapshot.identifiers().forEach(value -> entityManager.persist(CatalogItemIdentifierEntity.from(company,item,value)));
        snapshot.conversions().forEach(value -> persistConversion(company,item,value));
        snapshot.classification().ifPresent(value -> persistClassification(company,item,value));
        snapshot.variant().ifPresent(value -> persistVariant(company,item,value));
    }

    private void persistConversion(UUID company, UUID item, ItemUnitConversion value) {
        entityManager.persist(CatalogItemUnitConversionEntity.from(company,item,value));
        value.purposes().forEach(purpose -> entityManager.persist(CatalogItemUnitPurposeEntity.from(
                company,item,value.unit().value(),purpose,value.defaultFor().contains(purpose))));
    }

    private void persistClassification(UUID company, UUID item, CatalogClassification value) {
        entityManager.persist(CatalogItemCategoryEntity.from(company,item,value.mainCategory(),true));
        value.secondaryCategories().forEach(id -> entityManager.persist(CatalogItemCategoryEntity.from(company,item,id,false)));
        value.tags().forEach(id -> entityManager.persist(CatalogItemTagEntity.from(company,item,id)));
    }

    private void persistVariant(UUID company, UUID item, CatalogVariant value) {
        entityManager.persist(CatalogItemVariantEntity.from(
                company,item,value.familyId(),value.familyVersion()));
        value.attributes().forEach((code,attribute) -> entityManager.persist(
                CatalogItemVariantAttributeEntity.from(
                        company,item,value.familyId(),value.familyVersion(),code,attribute)));
    }

    private void synchronizeDetails(CatalogItemSnapshot snapshot) {
        UUID company=snapshot.companyId().value(), item=snapshot.id().value();
        syncAssignments(CatalogItemScopeEntity.class, company, item,
                snapshot.scopes().stream().collect(Collectors.toMap(Enum::name, value -> CatalogItemScopeEntity.from(company,item,value))),
                CatalogItemScopeEntity::key);
        snapshot.identifiers().forEach(value -> upsert(CatalogItemIdentifierEntity.class,
                new CatalogItemUuidKey(company,item,value.id().value()),
                () -> CatalogItemIdentifierEntity.from(company,item,value), entity -> entity.apply(value)));
        snapshot.conversions().forEach(value -> upsert(CatalogItemUnitConversionEntity.class,
                new CatalogItemStringKey(company,item,value.unit().value()),
                () -> CatalogItemUnitConversionEntity.from(company,item,value), entity -> entity.apply(value)));
        syncPurposes(snapshot, company, item);
        Map<UUID,CatalogItemCategoryEntity> categories=new LinkedHashMap<>();
        snapshot.classification().ifPresent(value -> {
            categories.put(value.mainCategory().value(), CatalogItemCategoryEntity.from(company,item,value.mainCategory(),true));
            value.secondaryCategories().forEach(id -> categories.put(id.value(), CatalogItemCategoryEntity.from(company,item,id,false)));
        });
        syncAssignments(CatalogItemCategoryEntity.class, company, item, categories, CatalogItemCategoryEntity::key);
        Map<UUID,CatalogItemTagEntity> tags=new LinkedHashMap<>();
        snapshot.classification().ifPresent(value -> value.tags().forEach(id -> tags.put(id.value(), CatalogItemTagEntity.from(company,item,id))));
        syncAssignments(CatalogItemTagEntity.class, company, item, tags, CatalogItemTagEntity::key);
        synchronizeVariant(snapshot, company, item);
    }

    private void syncPurposes(CatalogItemSnapshot snapshot, UUID company, UUID item) {
        Map<CatalogItemPurposeKey,CatalogItemUnitPurposeEntity> existing=owned(
                CatalogItemUnitPurposeEntity.class, company, item, "record.unitCode, record.purposeCode")
                .stream().collect(Collectors.toMap(CatalogItemUnitPurposeEntity::key, Function.identity()));
        Map<CatalogItemPurposeKey,CatalogItemUnitPurposeEntity> desired=new LinkedHashMap<>();
        snapshot.conversions().forEach(value -> value.purposes().forEach(purpose -> {
            CatalogItemUnitPurposeEntity purposeEntity=CatalogItemUnitPurposeEntity.from(
                    company,item,value.unit().value(),purpose,value.defaultFor().contains(purpose));
            desired.put(purposeEntity.key(),purposeEntity);
        }));
        existing.forEach((key,value) -> { if (!desired.containsKey(key)) { entityManager.remove(value); } });
        desired.forEach((key,value) -> {
            CatalogItemUnitPurposeEntity current=existing.get(key);
            if (current==null) { entityManager.persist(value); } else { current.apply(value.isDefault()); }
        });
    }

    private void synchronizeVariant(CatalogItemSnapshot snapshot, UUID company, UUID item) {
        owned(CatalogItemVariantAttributeEntity.class, company, item, "record.detailCode").forEach(entityManager::remove);
        owned(CatalogItemVariantEntity.class, company, item, "record.variantFamilyId").forEach(entityManager::remove);
        entityManager.flush();
        snapshot.variant().ifPresent(value -> persistVariant(company,item,value));
    }

    private <T,K> void syncAssignments(
            Class<T> type, UUID company, UUID item, Map<K,T> desired, Function<T,K> key) {
        Map<K,T> existing=owned(type, company, item, "record.catalogItemId").stream()
                .collect(Collectors.toMap(key, Function.identity()));
        existing.forEach((id,value) -> { if (!desired.containsKey(id)) { entityManager.remove(value); } });
        desired.forEach((id,value) -> { if (!existing.containsKey(id)) { entityManager.persist(value); } });
    }

    private <T> void upsert(Class<T> type, Object key, java.util.function.Supplier<T> creator, java.util.function.Consumer<T> updater) {
        T existing=entityManager.find(type,key);
        if (existing==null) { entityManager.persist(creator.get()); } else { updater.accept(existing); }
    }

    private <T> List<T> owned(Class<T> type, UUID company, UUID item, String orderBy) {
        return entityManager.createQuery("SELECT record FROM "+type.getSimpleName()+" record WHERE record.companyId = :company AND record.catalogItemId = :item ORDER BY "+orderBy, type)
                .setParameter("company",company).setParameter("item",item).getResultList();
    }

    private Map<UUID, Set<CatalogItemScope>> scopes(
            UUID companyId, List<CatalogItemEntity> roots) {
        if (roots.isEmpty()) {
            return Map.of();
        }
        Set<UUID> itemIds = roots.stream()
                .map(CatalogItemEntity::catalogItemId)
                .collect(Collectors.toSet());
        return entityManager.createQuery(
                        "SELECT scope FROM CatalogItemScopeEntity scope " +
                                "WHERE scope.companyId = :company " +
                                "AND scope.catalogItemId IN :items",
                        CatalogItemScopeEntity.class)
                .setParameter("company", companyId)
                .setParameter("items", itemIds)
                .getResultList().stream()
                .collect(Collectors.groupingBy(
                        CatalogItemScopeEntity::catalogItemId,
                        Collectors.mapping(
                                CatalogItemScopeEntity::toDomain,
                                Collectors.toUnmodifiableSet())));
    }

    private static SearchQuery searchQuery(
            CompanyId companyId, CatalogSearchCriteria criteria) {
        StringBuilder clause = new StringBuilder(
                "FROM CatalogItemEntity item WHERE item.companyId = :company");
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (!criteria.query().isEmpty()) {
            clause.append(" AND (LOWER(item.code) LIKE :query")
                    .append(" OR LOWER(item.displayName) LIKE :query")
                    .append(" OR EXISTS (SELECT identifier.detailId ")
                    .append("FROM CatalogItemIdentifierEntity identifier ")
                    .append("WHERE identifier.companyId = item.companyId ")
                    .append("AND identifier.catalogItemId = item.catalogItemId ")
                    .append("AND identifier.active = true ")
                    .append("AND LOWER(identifier.normalizedValue) LIKE :query))");
            parameters.put("query", "%" + criteria.query().toLowerCase(Locale.ROOT) + "%");
        }
        if (!criteria.types().isEmpty()) {
            clause.append(" AND item.itemType IN :types");
            parameters.put("types", criteria.types());
        }
        if (!criteria.states().isEmpty()) {
            clause.append(" AND item.state IN :states");
            parameters.put("states", criteria.states());
        }
        return new SearchQuery(clause.toString(), parameters);
    }

    private record SearchQuery(String clause, Map<String, Object> parameters) {
    }
}
