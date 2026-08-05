package py.com.logixone.plugins.businesspartners.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionKind;

@Embeddable
public class BusinessPartnerDefinitionEntityId implements Serializable {

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "definition_kind", nullable = false, updatable = false, length = 32)
    private BusinessPartnerDefinitionKind kind;

    @Column(name = "code", nullable = false, updatable = false, length = 48)
    private String code;

    protected BusinessPartnerDefinitionEntityId() {
    }

    BusinessPartnerDefinitionEntityId(
            UUID companyId, BusinessPartnerDefinitionKind kind, String code) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.code = Objects.requireNonNull(code, "code");
    }

    UUID companyId() {
        return companyId;
    }

    BusinessPartnerDefinitionKind kind() {
        return kind;
    }

    String code() {
        return code;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof BusinessPartnerDefinitionEntityId that
                && Objects.equals(companyId, that.companyId)
                && kind == that.kind
                && Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, kind, code);
    }
}
