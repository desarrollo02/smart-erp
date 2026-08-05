package py.com.logixone.plugins.businesspartners.application;

import java.util.List;
import java.util.Objects;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;

public record BusinessPartnerWarning(
        Code code,
        List<BusinessPartnerId> candidateIds) {

    public enum Code {
        POTENTIAL_DUPLICATE_IDENTIFICATION
    }

    public BusinessPartnerWarning {
        Objects.requireNonNull(code, "code");
        candidateIds = List.copyOf(Objects.requireNonNull(candidateIds, "candidateIds"));
        if (candidateIds.isEmpty()) {
            throw new IllegalArgumentException("A warning requires at least one candidate");
        }
    }
}
