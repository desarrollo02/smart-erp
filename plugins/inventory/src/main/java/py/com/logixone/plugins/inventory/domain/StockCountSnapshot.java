package py.com.logixone.plugins.inventory.domain;

import java.util.List;
import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockCountId;

public record StockCountSnapshot(
        CompanyId companyId,
        StockCountId id,
        StockCountScope scope,
        StockCountState state,
        long version,
        List<StockCountLineSnapshot> lines) {
    public StockCountSnapshot {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(state, "state");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
    }
}
