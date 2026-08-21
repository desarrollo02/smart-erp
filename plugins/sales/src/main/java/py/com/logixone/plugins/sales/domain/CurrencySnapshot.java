package py.com.logixone.plugins.sales.domain;

import java.util.Objects;
import py.com.logixone.plugins.referencedata.api.CurrencyCode;

public record CurrencySnapshot(CurrencyCode code, int minorUnit, String displayName, String releaseId) {
    public CurrencySnapshot {
        Objects.requireNonNull(code, "code");
        if (minorUnit < 0 || minorUnit > 9) throw new IllegalArgumentException("Invalid minorUnit");
        displayName = SalesValues.text(displayName, "currency name", 160); releaseId = SalesValues.text(releaseId, "releaseId", 64);
    }
}
