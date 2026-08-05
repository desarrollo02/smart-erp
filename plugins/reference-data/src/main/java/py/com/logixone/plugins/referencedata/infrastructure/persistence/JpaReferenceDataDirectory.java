package py.com.logixone.plugins.referencedata.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.net.URI;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.referencedata.api.CatalogCompleteness;
import py.com.logixone.plugins.referencedata.api.CountryCode;
import py.com.logixone.plugins.referencedata.api.CountryReference;
import py.com.logixone.plugins.referencedata.api.CurrencyCode;
import py.com.logixone.plugins.referencedata.api.CurrencyReference;
import py.com.logixone.plugins.referencedata.api.ReferenceDataCatalog;
import py.com.logixone.plugins.referencedata.api.ReferenceDataDirectory;
import py.com.logixone.plugins.referencedata.api.ReferenceDataRelease;

/** Native read adapter over the plugin-owned schema; no foreign schema is queried. */
@ApplicationScoped
@Transactional(TxType.SUPPORTS)
public class JpaReferenceDataDirectory implements ReferenceDataDirectory {

    private static final String SCHEMA = ReferenceDataPersistenceNames.SCHEMA;

    @PersistenceContext(unitName = ReferenceDataPersistenceNames.UNIT_NAME)
    private EntityManager entityManager;

    public JpaReferenceDataDirectory() {
    }

    JpaReferenceDataDirectory(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public ReferenceDataRelease currentRelease(
            CompanyId companyId, ReferenceDataCatalog catalog) {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(catalog, "catalog");
        Object[] row = (Object[]) entityManager.createNativeQuery(
                        "SELECT release_id, standard_id, authority, source_uri, "
                                + "source_sha256, observed_on, completeness, entry_count "
                                + "FROM " + SCHEMA + ".catalog_release "
                                + "WHERE catalog_kind = :catalog AND current_release")
                .setParameter("catalog", catalog.name())
                .getSingleResult();
        return release(catalog, row);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CountryReference> countries(CompanyId companyId) {
        Objects.requireNonNull(companyId, "companyId");
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT e.alpha2_code, e.alpha3_code, e.numeric_code, e.display_name, "
                                + "r.release_id, COALESCE(p.enabled, TRUE) "
                                + "FROM " + SCHEMA + ".catalog_release r "
                                + "JOIN " + SCHEMA + ".country_entry e "
                                + "ON e.catalog_kind = r.catalog_kind AND e.release_id = r.release_id "
                                + "LEFT JOIN " + SCHEMA + ".company_country_policy p "
                                + "ON p.company_id = :company AND p.alpha2_code = e.alpha2_code "
                                + "WHERE r.catalog_kind = 'COUNTRY' AND r.current_release "
                                + "ORDER BY e.display_name, e.alpha2_code")
                .setParameter("company", companyId.value())
                .getResultList();
        return rows.stream().map(JpaReferenceDataDirectory::country).toList();
    }

    @Override
    public Optional<CountryReference> findCountry(CompanyId companyId, CountryCode code) {
        Objects.requireNonNull(code, "code");
        return countries(companyId).stream().filter(value -> value.code().equals(code)).findFirst();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CurrencyReference> currencies(CompanyId companyId) {
        Objects.requireNonNull(companyId, "companyId");
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT e.alphabetic_code, e.numeric_code, e.minor_unit, e.display_name, "
                                + "r.release_id, COALESCE(p.enabled, TRUE) "
                                + "FROM " + SCHEMA + ".catalog_release r "
                                + "JOIN " + SCHEMA + ".currency_entry e "
                                + "ON e.catalog_kind = r.catalog_kind AND e.release_id = r.release_id "
                                + "LEFT JOIN " + SCHEMA + ".company_currency_policy p "
                                + "ON p.company_id = :company AND p.alphabetic_code = e.alphabetic_code "
                                + "WHERE r.catalog_kind = 'CURRENCY' AND r.current_release "
                                + "ORDER BY e.display_name, e.alphabetic_code")
                .setParameter("company", companyId.value())
                .getResultList();
        return rows.stream().map(JpaReferenceDataDirectory::currency).toList();
    }

    @Override
    public Optional<CurrencyReference> findCurrency(CompanyId companyId, CurrencyCode code) {
        Objects.requireNonNull(code, "code");
        return currencies(companyId).stream().filter(value -> value.code().equals(code)).findFirst();
    }

    private static ReferenceDataRelease release(ReferenceDataCatalog catalog, Object[] row) {
        return new ReferenceDataRelease(
                catalog,
                string(row[0]),
                string(row[1]),
                string(row[2]),
                URI.create(string(row[3])),
                string(row[4]),
                localDate(row[5]),
                CatalogCompleteness.valueOf(string(row[6])),
                number(row[7]).intValue());
    }

    private static CountryReference country(Object[] row) {
        return new CountryReference(
                new CountryCode(string(row[0])),
                string(row[1]),
                string(row[2]),
                string(row[3]),
                string(row[4]),
                bool(row[5]));
    }

    private static CurrencyReference currency(Object[] row) {
        return new CurrencyReference(
                new CurrencyCode(string(row[0])),
                string(row[1]),
                number(row[2]).intValue(),
                string(row[3]),
                string(row[4]),
                bool(row[5]));
    }

    private static String string(Object value) {
        return Objects.toString(value);
    }

    private static Number number(Object value) {
        return (Number) value;
    }

    private static boolean bool(Object value) {
        return (Boolean) value;
    }

    private static LocalDate localDate(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return LocalDate.parse(string(value));
    }
}
