package py.com.logixone.plugins.purchasing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import py.com.logixone.plugins.purchasing.domain.CurrencySnapshot;
import py.com.logixone.plugins.referencedata.api.CurrencyCode;

@Embeddable
public class CurrencyEmbeddable {
    @Column(name = "currency_code", nullable = false, length = 3)
    private String code;
    @Column(name = "currency_minor_unit", nullable = false)
    private Integer minorUnit;
    @Column(name = "currency_name_snapshot", nullable = false, length = 160)
    private String displayName;
    @Column(name = "currency_release_id", nullable = false, length = 64)
    private String releaseId;

    protected CurrencyEmbeddable() {
    }

    static CurrencyEmbeddable from(CurrencySnapshot snapshot) {
        CurrencyEmbeddable value = new CurrencyEmbeddable();
        value.code = snapshot.code().value();
        value.minorUnit = snapshot.minorUnit();
        value.displayName = snapshot.displayName();
        value.releaseId = snapshot.releaseId();
        return value;
    }

    CurrencySnapshot snapshot() {
        return new CurrencySnapshot(new CurrencyCode(code), minorUnit, displayName, releaseId);
    }
}
