package py.com.logixone.plugins.businesspartners.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.businesspartners.application.port.BusinessPartnerCodeSequenceRepository;

@ApplicationScoped
@Transactional
public class JpaBusinessPartnerCodeSequenceRepository
        implements BusinessPartnerCodeSequenceRepository {

    private static final Pattern SCOPE = Pattern.compile("[a-z][a-z0-9_]{0,63}");

    @PersistenceContext(unitName = BusinessPartnersPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaBusinessPartnerCodeSequenceRepository() {
    }

    JpaBusinessPartnerCodeSequenceRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public long nextValue(CompanyId companyId, String sequenceScope) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(sequenceScope, "sequenceScope");
        String normalized = Normalizer.normalize(sequenceScope, Normalizer.Form.NFKC)
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!SCOPE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid business partner sequence scope");
        }
        Number allocated = (Number) entityManager.createNativeQuery("""
                        INSERT INTO plg_business_partners.business_partner_code_sequence
                            (company_id, sequence_scope, next_value)
                        VALUES (:companyId, :sequenceScope, 2)
                        ON CONFLICT (company_id, sequence_scope)
                        DO UPDATE SET next_value =
                                plg_business_partners.business_partner_code_sequence.next_value + 1,
                                      updated_at = CURRENT_TIMESTAMP
                        RETURNING next_value - 1
                        """)
                .setParameter("companyId", companyId.value())
                .setParameter("sequenceScope", normalized)
                .getSingleResult();
        return allocated.longValue();
    }
}
