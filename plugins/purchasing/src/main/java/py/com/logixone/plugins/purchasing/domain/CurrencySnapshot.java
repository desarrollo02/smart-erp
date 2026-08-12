package py.com.logixone.plugins.purchasing.domain;

import java.util.Objects;
import py.com.logixone.plugins.referencedata.api.CurrencyCode;

/** Currency metadata frozen with the order. */
public record CurrencySnapshot(
        CurrencyCode code,
        int minorUnit,
        String displayName,
        String releaseId) {

    public CurrencySnapshot {
        Objects.requireNonNull(code, "code");
        if (minorUnit < 0 || minorUnit > 9) {
            throw new IllegalArgumentException("minorUnit must be between 0 and 9");
        }
        displayName = PurchasingValues.text(displayName, "currencyDisplayName", 160);
        releaseId = PurchasingValues.text(releaseId, "releaseId", 64);
    }
}
