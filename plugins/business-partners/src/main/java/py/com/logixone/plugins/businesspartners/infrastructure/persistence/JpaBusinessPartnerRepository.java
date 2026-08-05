package py.com.logixone.plugins.businesspartners.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerPersistenceCode;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerPersistenceException;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerRepository;
import py.com.logixone.plugins.businesspartners.application.query.BusinessPartnerSearchCriteria;
import py.com.logixone.plugins.businesspartners.application.query.BusinessPartnerSearchPage;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartner;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAddress;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerContact;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerContactChannel;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerIdentification;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerIdentificationKey;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerSnapshot;
import py.com.logixone.plugins.businesspartners.domain.CommercialRole;

@ApplicationScoped
@Transactional
public class JpaBusinessPartnerRepository implements BusinessPartnerRepository {

    @PersistenceContext(unitName = BusinessPartnersPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaBusinessPartnerRepository() {
    }

    JpaBusinessPartnerRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public Optional<BusinessPartner> findById(CompanyId companyId, BusinessPartnerId id) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(id, "id");
        BusinessPartnerEntity entity = entityManager.find(
                BusinessPartnerEntity.class,
                new BusinessPartnerEntityId(companyId.value(), id.value()));
        return Optional.ofNullable(entity).map(this::restore);
    }

    @Override
    public BusinessPartner insert(BusinessPartner partner) {
        Objects.requireNonNull(partner, "partner");
        BusinessPartnerSnapshot snapshot = partner.snapshot();
        if (snapshot.version() != 0) {
            throw new BusinessPartnerPersistenceException(
                    BusinessPartnerPersistenceCode.VERSION_CONFLICT);
        }
        BusinessPartnerEntity entity = BusinessPartnerEntity.from(snapshot);
        try {
            entityManager.persist(entity);
            entityManager.flush();
            persistAllDetails(snapshot);
            entityManager.flush();
            return restore(entity);
        } catch (PersistenceException failure) {
            throw PostgreSqlBusinessPartnerConflictMapper.map(failure);
        }
    }

    @Override
    public BusinessPartner update(BusinessPartner partner, long expectedPersistedVersion) {
        Objects.requireNonNull(partner, "partner");
        if (expectedPersistedVersion < 0 || partner.version() != expectedPersistedVersion + 1) {
            throw new BusinessPartnerPersistenceException(
                    BusinessPartnerPersistenceCode.VERSION_CONFLICT);
        }
        BusinessPartnerSnapshot snapshot = partner.snapshot();
        BusinessPartnerEntity entity = entityManager.find(
                BusinessPartnerEntity.class,
                new BusinessPartnerEntityId(
                        snapshot.companyId().value(), snapshot.id().value()));
        if (entity == null) {
            throw new BusinessPartnerPersistenceException(
                    BusinessPartnerPersistenceCode.PARTNER_NOT_FOUND);
        }
        if (entity.version() != expectedPersistedVersion) {
            throw new BusinessPartnerPersistenceException(
                    BusinessPartnerPersistenceCode.VERSION_CONFLICT);
        }

        try {
            entity.apply(snapshot);
            synchronizeExistingDetails(snapshot);
            entityManager.flush();
            persistMissingDetails(snapshot);
            entityManager.flush();
            return restore(entity);
        } catch (OptimisticLockException failure) {
            throw new BusinessPartnerPersistenceException(
                    BusinessPartnerPersistenceCode.VERSION_CONFLICT, failure);
        } catch (PersistenceException failure) {
            throw PostgreSqlBusinessPartnerConflictMapper.map(failure);
        }
    }

    @Override
    public List<BusinessPartnerId> findIdentificationCandidates(
            CompanyId companyId, BusinessPartnerIdentificationKey candidate) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(candidate, "candidate");
        return entityManager.createQuery("""
                        SELECT DISTINCT candidateIdentification.id.businessPartnerId
                        FROM BusinessPartnerIdentificationEntity candidateIdentification
                        WHERE candidateIdentification.id.companyId = :companyId
                          AND candidateIdentification.typeCode = :typeCode
                          AND ((candidateIdentification.countryCode = :countryCode)
                               OR (candidateIdentification.countryCode IS NULL AND :countryCode IS NULL))
                          AND candidateIdentification.normalizedValue = :normalizedValue
                        ORDER BY candidateIdentification.id.businessPartnerId
                        """, UUID.class)
                .setParameter("companyId", companyId.value())
                .setParameter("typeCode", candidate.type().value())
                .setParameter("countryCode", candidate.countryCode().orElse(null))
                .setParameter("normalizedValue", candidate.normalizedValue())
                .getResultList()
                .stream()
                .map(BusinessPartnerId::new)
                .toList();
    }

    @Override
    public BusinessPartnerSearchPage search(
            CompanyId companyId, BusinessPartnerSearchCriteria criteria) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(criteria, "criteria");
        String predicate = searchPredicate(criteria);
        var idQuery = entityManager.createQuery(
                "SELECT partner.id.businessPartnerId FROM BusinessPartnerEntity partner WHERE "
                        + predicate
                        + " ORDER BY partner.displayName, partner.id.businessPartnerId",
                UUID.class);
        bindSearch(idQuery, companyId, criteria);
        List<UUID> ids = idQuery
                .setFirstResult(criteria.offset())
                .setMaxResults(criteria.limit())
                .getResultList();
        var countQuery = entityManager.createQuery(
                "SELECT COUNT(partner) FROM BusinessPartnerEntity partner WHERE " + predicate,
                Long.class);
        bindSearch(countQuery, companyId, criteria);
        List<py.com.logixone.plugins.businesspartners.api.BusinessPartnerReference> items = ids.stream()
                .map(id -> findById(companyId, new BusinessPartnerId(id)).orElseThrow())
                .map(BusinessPartner::toReference)
                .toList();
        return new BusinessPartnerSearchPage(
                items,
                countQuery.getSingleResult(),
                criteria.offset(),
                criteria.limit());
    }

    private static void bindSearch(
            jakarta.persistence.Query query,
            CompanyId companyId,
            BusinessPartnerSearchCriteria criteria) {
        query.setParameter("companyId", companyId.value());
        criteria.text().ifPresent(text ->
                query.setParameter("pattern", "%" + escapeLike(text) + "%"));
        criteria.state().ifPresent(state -> query.setParameter("state", state));
        criteria.role().ifPresent(role -> query.setParameter("role", role));
    }

    private static String searchPredicate(BusinessPartnerSearchCriteria criteria) {
        StringBuilder predicate = new StringBuilder("partner.id.companyId = :companyId");
        if (criteria.text().isPresent()) {
            predicate.append(' ');
            predicate.append("""
                    AND (LOWER(partner.code) LIKE :pattern
                        OR LOWER(partner.displayName) LIKE :pattern
                        OR LOWER(partner.legalName) LIKE :pattern
                        OR LOWER(partner.tradeName) LIKE :pattern
                        OR EXISTS (
                            SELECT identification.id.detailId
                            FROM BusinessPartnerIdentificationEntity identification
                            WHERE identification.id.companyId = partner.id.companyId
                              AND identification.id.businessPartnerId = partner.id.businessPartnerId
                              AND LOWER(identification.normalizedValue) LIKE :pattern))
                    """);
        }
        if (criteria.state().isPresent()) {
            predicate.append(" AND partner.state = :state");
        }
        if (criteria.role().isPresent()) {
            predicate.append(' ');
            predicate.append("""
                    AND EXISTS (
                        SELECT role.id.roleType
                        FROM BusinessPartnerRoleEntity role
                        WHERE role.id.companyId = partner.id.companyId
                          AND role.id.businessPartnerId = partner.id.businessPartnerId
                          AND role.id.roleType = :role)
                    """);
        }
        return predicate.toString();
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private BusinessPartner restore(BusinessPartnerEntity entity) {
        BusinessPartnerSnapshot root = entity.rootSnapshot();
        UUID companyId = root.companyId().value();
        UUID partnerId = root.id().value();
        List<BusinessPartnerContactChannelEntity> contactChannels = contactChannels(companyId, partnerId);
        Map<UUID, List<BusinessPartnerContactChannel>> channelsByContact = new LinkedHashMap<>();
        contactChannels.forEach(channel -> channelsByContact
                .computeIfAbsent(channel.contactId(), ignored -> new ArrayList<>())
                .add(channel.toDomain()));
        List<BusinessPartnerContact> contacts = contacts(companyId, partnerId).stream()
                .map(contact -> contact.toDomain(
                        channelsByContact.getOrDefault(contact.contactId(), List.of())))
                .toList();
        return BusinessPartner.restore(new BusinessPartnerSnapshot(
                root.companyId(),
                root.id(),
                root.code(),
                root.kind(),
                root.displayName(),
                root.legalName(),
                root.tradeName(),
                root.state(),
                roles(companyId, partnerId).stream().map(BusinessPartnerRoleEntity::toDomain).toList(),
                identifications(companyId, partnerId).stream()
                        .map(BusinessPartnerIdentificationEntity::toDomain).toList(),
                addresses(companyId, partnerId).stream().map(BusinessPartnerAddressEntity::toDomain).toList(),
                channels(companyId, partnerId).stream().map(BusinessPartnerChannelEntity::toDomain).toList(),
                contacts,
                root.version()));
    }

    private void persistAllDetails(BusinessPartnerSnapshot snapshot) {
        UUID companyId = snapshot.companyId().value();
        UUID partnerId = snapshot.id().value();
        snapshot.roles().forEach(value -> entityManager.persist(
                BusinessPartnerRoleEntity.from(companyId, partnerId, value)));
        snapshot.identifications().forEach(value -> entityManager.persist(
                BusinessPartnerIdentificationEntity.from(companyId, partnerId, value)));
        snapshot.addresses().forEach(value -> entityManager.persist(
                BusinessPartnerAddressEntity.from(companyId, partnerId, value)));
        snapshot.channels().forEach(value -> entityManager.persist(
                BusinessPartnerChannelEntity.from(companyId, partnerId, value)));
        snapshot.contacts().forEach(contact -> {
            entityManager.persist(BusinessPartnerContactEntity.from(companyId, partnerId, contact));
            contact.channels().forEach(channel -> entityManager.persist(
                    BusinessPartnerContactChannelEntity.from(
                            companyId, partnerId, contact.id().value(), channel)));
        });
    }

    private void synchronizeExistingDetails(BusinessPartnerSnapshot snapshot) {
        UUID companyId = snapshot.companyId().value();
        UUID partnerId = snapshot.id().value();
        snapshot.roles().forEach(value -> applyIfPresent(
                BusinessPartnerRoleEntity.class,
                new BusinessPartnerRoleEntityId(companyId, partnerId, value.type()),
                entity -> entity.apply(value)));
        snapshot.identifications().forEach(value -> applyIfPresent(
                BusinessPartnerIdentificationEntity.class,
                new BusinessPartnerDetailEntityId(companyId, partnerId, value.id().value()),
                entity -> entity.apply(value)));
        snapshot.addresses().forEach(value -> applyIfPresent(
                BusinessPartnerAddressEntity.class,
                new BusinessPartnerDetailEntityId(companyId, partnerId, value.id().value()),
                entity -> entity.apply(value)));
        snapshot.channels().forEach(value -> applyIfPresent(
                BusinessPartnerChannelEntity.class,
                new BusinessPartnerDetailEntityId(companyId, partnerId, value.id().value()),
                entity -> entity.apply(value)));
        snapshot.contacts().forEach(contact -> {
            applyIfPresent(
                    BusinessPartnerContactEntity.class,
                    new BusinessPartnerDetailEntityId(companyId, partnerId, contact.id().value()),
                    entity -> entity.apply(contact));
            contact.channels().forEach(channel -> applyIfPresent(
                    BusinessPartnerContactChannelEntity.class,
                    new BusinessPartnerContactChannelEntityId(
                            companyId, partnerId, contact.id().value(), channel.id().value()),
                    entity -> entity.apply(channel)));
        });
    }

    private void persistMissingDetails(BusinessPartnerSnapshot snapshot) {
        UUID companyId = snapshot.companyId().value();
        UUID partnerId = snapshot.id().value();
        snapshot.roles().forEach(value -> persistIfMissing(
                BusinessPartnerRoleEntity.class,
                new BusinessPartnerRoleEntityId(companyId, partnerId, value.type()),
                () -> BusinessPartnerRoleEntity.from(companyId, partnerId, value)));
        snapshot.identifications().forEach(value -> persistIfMissing(
                BusinessPartnerIdentificationEntity.class,
                new BusinessPartnerDetailEntityId(companyId, partnerId, value.id().value()),
                () -> BusinessPartnerIdentificationEntity.from(companyId, partnerId, value)));
        snapshot.addresses().forEach(value -> persistIfMissing(
                BusinessPartnerAddressEntity.class,
                new BusinessPartnerDetailEntityId(companyId, partnerId, value.id().value()),
                () -> BusinessPartnerAddressEntity.from(companyId, partnerId, value)));
        snapshot.channels().forEach(value -> persistIfMissing(
                BusinessPartnerChannelEntity.class,
                new BusinessPartnerDetailEntityId(companyId, partnerId, value.id().value()),
                () -> BusinessPartnerChannelEntity.from(companyId, partnerId, value)));
        snapshot.contacts().forEach(contact -> {
            persistIfMissing(
                    BusinessPartnerContactEntity.class,
                    new BusinessPartnerDetailEntityId(companyId, partnerId, contact.id().value()),
                    () -> BusinessPartnerContactEntity.from(companyId, partnerId, contact));
            contact.channels().forEach(channel -> persistIfMissing(
                    BusinessPartnerContactChannelEntity.class,
                    new BusinessPartnerContactChannelEntityId(
                            companyId, partnerId, contact.id().value(), channel.id().value()),
                    () -> BusinessPartnerContactChannelEntity.from(
                            companyId, partnerId, contact.id().value(), channel)));
        });
    }

    private <T, I> void applyIfPresent(
            Class<T> entityType, I id, java.util.function.Consumer<T> change) {
        T entity = entityManager.find(entityType, id);
        if (entity != null) {
            change.accept(entity);
        }
    }

    private <T, I> void persistIfMissing(
            Class<T> entityType, I id, java.util.function.Supplier<T> newEntity) {
        if (entityManager.find(entityType, id) == null) {
            entityManager.persist(newEntity.get());
        }
    }

    private List<BusinessPartnerRoleEntity> roles(UUID companyId, UUID partnerId) {
        return owned(BusinessPartnerRoleEntity.class, companyId, partnerId, "ownedRecord.id.roleType");
    }

    private List<BusinessPartnerIdentificationEntity> identifications(UUID companyId, UUID partnerId) {
        return owned(BusinessPartnerIdentificationEntity.class, companyId, partnerId, "ownedRecord.id.detailId");
    }

    private List<BusinessPartnerAddressEntity> addresses(UUID companyId, UUID partnerId) {
        return owned(BusinessPartnerAddressEntity.class, companyId, partnerId, "ownedRecord.id.detailId");
    }

    private List<BusinessPartnerChannelEntity> channels(UUID companyId, UUID partnerId) {
        return owned(BusinessPartnerChannelEntity.class, companyId, partnerId, "ownedRecord.id.detailId");
    }

    private List<BusinessPartnerContactEntity> contacts(UUID companyId, UUID partnerId) {
        return owned(BusinessPartnerContactEntity.class, companyId, partnerId, "ownedRecord.id.detailId");
    }

    private List<BusinessPartnerContactChannelEntity> contactChannels(UUID companyId, UUID partnerId) {
        return owned(
                BusinessPartnerContactChannelEntity.class,
                companyId,
                partnerId,
                "ownedRecord.id.contactId, ownedRecord.id.channelId");
    }

    private <T> List<T> owned(
            Class<T> type, UUID companyId, UUID partnerId, String orderBy) {
        return entityManager.createQuery("SELECT ownedRecord FROM " + type.getSimpleName() + " ownedRecord "
                        + "WHERE ownedRecord.id.companyId = :companyId "
                        + "AND ownedRecord.id.businessPartnerId = :partnerId ORDER BY " + orderBy, type)
                .setParameter("companyId", companyId)
                .setParameter("partnerId", partnerId)
                .getResultList();
    }
}
