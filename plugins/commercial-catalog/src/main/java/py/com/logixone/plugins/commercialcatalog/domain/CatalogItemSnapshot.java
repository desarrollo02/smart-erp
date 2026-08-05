package py.com.logixone.plugins.commercialcatalog.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemScope;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemType;

/** Complete neutral state used only across the domain/persistence boundary. */
public record CatalogItemSnapshot(
        CompanyId companyId,
        CatalogItemId id,
        CatalogItemCode code,
        CatalogItemName name,
        String description,
        CatalogItemType type,
        Set<CatalogItemScope> scopes,
        UnitCode baseUnit,
        TaxProfileReference taxProfile,
        Optional<CatalogClassification> classification,
        Optional<CatalogVariant> variant,
        CatalogItemState state,
        Optional<CatalogItemId> replacementId,
        List<CatalogItemIdentifier> identifiers,
        List<ItemUnitConversion> conversions,
        long version) {

    public CatalogItemSnapshot {
        Objects.requireNonNull(companyId, "companyId");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        description = DomainValues.optionalText(description, "description", 1000);
        Objects.requireNonNull(type, "type");
        scopes = Set.copyOf(Objects.requireNonNull(scopes, "scopes"));
        if (scopes.isEmpty()) {
            throw new IllegalArgumentException("Catalog item must have at least one scope");
        }
        Objects.requireNonNull(baseUnit, "baseUnit");
        Objects.requireNonNull(taxProfile, "taxProfile");
        classification = Objects.requireNonNull(classification, "classification");
        variant = Objects.requireNonNull(variant, "variant");
        Objects.requireNonNull(state, "state");
        replacementId = Objects.requireNonNull(replacementId, "replacementId");
        if (state == CatalogItemState.ACTIVE && replacementId.isPresent()) {
            throw new IllegalArgumentException("An active catalog item cannot have a replacement");
        }
        if (replacementId.filter(id::equals).isPresent()) {
            throw new IllegalArgumentException("A catalog item cannot replace itself");
        }
        identifiers = List.copyOf(Objects.requireNonNull(identifiers, "identifiers"));
        conversions = List.copyOf(Objects.requireNonNull(conversions, "conversions"));
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
