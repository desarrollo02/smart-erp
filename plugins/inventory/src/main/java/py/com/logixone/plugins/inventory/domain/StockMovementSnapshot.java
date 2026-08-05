package py.com.logixone.plugins.inventory.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockMovementId;
import py.com.logixone.plugins.inventory.api.StockMovementRequest;

public record StockMovementSnapshot(
        CompanyId companyId,
        StockMovementId id,
        StockMovementRequest request,
        Instant postedAt,
        List<StockMovementLineSnapshot> lines) {
    public StockMovementSnapshot {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(postedAt, "postedAt");
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        if (lines.size() != request.lines().size()) {
            throw new IllegalArgumentException("Snapshot lines must match movement request lines");
        }
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).lineNumber() != index + 1
                    || !lines.get(index).line().equals(request.lines().get(index))) {
                throw new IllegalArgumentException("Snapshot lines must preserve request order");
            }
        }
    }
}
