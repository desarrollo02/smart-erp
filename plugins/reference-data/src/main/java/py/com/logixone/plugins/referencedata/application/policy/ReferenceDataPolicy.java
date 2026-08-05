package py.com.logixone.plugins.referencedata.application.policy;

import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.referencedata.api.CountryCode;
import py.com.logixone.plugins.referencedata.api.CurrencyCode;
import py.com.logixone.plugins.referencedata.api.ReferenceDataCatalog;

/** Effective enterprise override for one code in the current normative release. */
public record ReferenceDataPolicy(
        CompanyId companyId,
        ReferenceDataCatalog catalog,
        String code,
        boolean enabled,
        long version) {

    public ReferenceDataPolicy {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(catalog, "catalog");
        code = canonicalCode(catalog, code);
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }

    public static ReferenceDataPolicy defaultEnabled(
            CompanyId companyId, ReferenceDataCatalog catalog, String code) {
        return new ReferenceDataPolicy(companyId, catalog, code, true, 0);
    }

    public static String canonicalCode(ReferenceDataCatalog catalog, String code) {
        Objects.requireNonNull(catalog, "catalog");
        return switch (catalog) {
            case COUNTRY -> new CountryCode(code).value();
            case CURRENCY -> new CurrencyCode(code).value();
        };
    }
}
