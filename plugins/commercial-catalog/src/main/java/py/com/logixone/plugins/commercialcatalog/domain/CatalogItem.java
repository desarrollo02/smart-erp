package py.com.logixone.plugins.commercialcatalog.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemReference;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemScope;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemType;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversionResult;

/** Aggregate root shared by products and services without inventory concerns. */
public final class CatalogItem {
    private final CompanyId companyId;
    private final CatalogItemId id;
    private final CatalogItemType type;
    private CatalogItemCode code;
    private CatalogItemName name;
    private String description;
    private Set<CatalogItemScope> scopes;
    private final UnitCode baseUnit;
    private TaxProfileReference taxProfile;
    private CatalogClassification classification;
    private CatalogVariant variant;
    private CatalogItemState state;
    private CatalogItemId replacementId;
    private final Map<CatalogDetailId, CatalogItemIdentifier> identifiers = new LinkedHashMap<>();
    private final Map<UnitCode, ItemUnitConversion> conversions = new LinkedHashMap<>();
    private long version;

    private CatalogItem(
            CompanyId companyId,
            CatalogItemId id,
            CatalogItemCode code,
            CatalogItemName name,
            String description,
            CatalogItemType type,
            Set<CatalogItemScope> scopes,
            UnitCode baseUnit,
            TaxProfileReference taxProfile) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.id = Objects.requireNonNull(id, "id");
        this.code = Objects.requireNonNull(code, "code");
        this.name = Objects.requireNonNull(name, "name");
        this.description = DomainValues.optionalText(description, "description", 1000);
        this.type = Objects.requireNonNull(type, "type");
        this.scopes = copyScopes(scopes);
        this.baseUnit = Objects.requireNonNull(baseUnit, "baseUnit");
        this.taxProfile = Objects.requireNonNull(taxProfile, "taxProfile");
        this.state = CatalogItemState.ACTIVE;
    }

    public static CatalogItem create(
            CompanyId companyId,
            CatalogItemId id,
            CatalogItemCode code,
            CatalogItemName name,
            String description,
            CatalogItemType type,
            Set<CatalogItemScope> scopes,
            UnitCode baseUnit,
            TaxProfileReference taxProfile) {
        return new CatalogItem(companyId, id, code, name, description, type, scopes, baseUnit, taxProfile);
    }

    public static CatalogItem restore(CatalogItemSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        CatalogItem item = new CatalogItem(
                snapshot.companyId(),
                snapshot.id(),
                snapshot.code(),
                snapshot.name(),
                snapshot.description(),
                snapshot.type(),
                snapshot.scopes(),
                snapshot.baseUnit(),
                snapshot.taxProfile());
        snapshot.identifiers().forEach(identifier ->
                item.addIdentifier(identifier, item.version));
        snapshot.conversions().forEach(conversion ->
                item.addUnitConversion(conversion, item.version));
        snapshot.classification().ifPresent(classification ->
                item.classify(classification, item.version));
        snapshot.variant().ifPresent(variant ->
                item.assignVariant(variant, item.version));
        item.state = snapshot.state();
        item.replacementId = snapshot.replacementId().orElse(null);
        item.version = snapshot.version();
        return item;
    }

    public void reviseIdentity(
            CatalogItemCode code,
            CatalogItemName name,
            String description,
            Set<CatalogItemScope> scopes,
            long expectedVersion) {
        verifyVersion(expectedVersion);
        requireActive();
        this.code = Objects.requireNonNull(code, "code");
        this.name = Objects.requireNonNull(name, "name");
        this.description = DomainValues.optionalText(description, "description", 1000);
        this.scopes = copyScopes(scopes);
        version++;
    }

    public void addIdentifier(CatalogItemIdentifier identifier, long expectedVersion) {
        verifyVersion(expectedVersion);
        requireActive();
        Objects.requireNonNull(identifier, "identifier");
        if (identifiers.containsKey(identifier.id())) {
            throw new IllegalArgumentException("Identifier id already exists in this item");
        }
        boolean duplicate = identifiers.values().stream()
                .filter(CatalogItemIdentifier::active)
                .anyMatch(existing -> existing.typeCode().equals(identifier.typeCode())
                        && existing.normalizedValue().equals(identifier.normalizedValue()));
        if (duplicate) {
            throw new IllegalArgumentException("Active identifier is duplicated in this item");
        }
        identifiers.put(identifier.id(), identifier);
        version++;
    }

    public void inactivateIdentifier(CatalogDetailId identifierId, long expectedVersion) {
        verifyVersion(expectedVersion);
        CatalogItemIdentifier identifier = identifiers.get(Objects.requireNonNull(identifierId, "identifierId"));
        if (identifier == null) {
            throw new IllegalArgumentException("Identifier does not belong to this item");
        }
        if (identifier.active()) {
            identifiers.put(identifierId, identifier.inactivate());
            version++;
        }
    }

    public void addUnitConversion(ItemUnitConversion conversion, long expectedVersion) {
        verifyVersion(expectedVersion);
        requireActive();
        Objects.requireNonNull(conversion, "conversion");
        if (conversion.unit().equals(baseUnit)) {
            throw new IllegalArgumentException("Base unit conversion is implicit and cannot be added");
        }
        if (conversions.containsKey(conversion.unit())) {
            throw new IllegalArgumentException("Unit conversion already exists in this item");
        }
        boolean ambiguousDefault = conversions.values().stream()
                .filter(ItemUnitConversion::active)
                .anyMatch(existing -> existing.defaultFor().stream().anyMatch(conversion.defaultFor()::contains));
        if (ambiguousDefault) {
            throw new IllegalArgumentException("Only one active default unit is allowed per purpose");
        }
        conversions.put(conversion.unit(), conversion);
        version++;
    }

    public CatalogUnitConversionResult convert(
            UnitCode sourceUnit, UnitCode targetUnit, BigDecimal quantity) {
        Objects.requireNonNull(sourceUnit, "sourceUnit");
        Objects.requireNonNull(targetUnit, "targetUnit");
        BigDecimal sourceQuantity = DomainValues.positive(quantity, "quantity");
        BigDecimal factor = factorToBase(sourceUnit).divide(factorToBase(targetUnit), MathContext.DECIMAL128);
        return new CatalogUnitConversionResult(
                id,
                sourceUnit.value(),
                targetUnit.value(),
                sourceQuantity,
                factor,
                sourceQuantity.multiply(factor, MathContext.DECIMAL128),
                version);
    }

    public void classify(CatalogClassification classification, long expectedVersion) {
        verifyVersion(expectedVersion);
        requireActive();
        this.classification = Objects.requireNonNull(classification, "classification");
        version++;
    }

    public void assignTaxProfile(TaxProfileReference taxProfile, long expectedVersion) {
        verifyVersion(expectedVersion);
        requireActive();
        this.taxProfile = Objects.requireNonNull(taxProfile, "taxProfile");
        version++;
    }

    public void assignVariant(CatalogVariant variant, long expectedVersion) {
        verifyVersion(expectedVersion);
        requireActive();
        this.variant = Objects.requireNonNull(variant, "variant");
        version++;
    }

    public void inactivate(Optional<CatalogItemId> replacementId, long expectedVersion) {
        verifyVersion(expectedVersion);
        Objects.requireNonNull(replacementId, "replacementId");
        replacementId.ifPresent(replacement -> {
            if (replacement.equals(id)) {
                throw new IllegalArgumentException("An item cannot replace itself");
            }
        });
        if (state == CatalogItemState.ACTIVE || !Objects.equals(this.replacementId, replacementId.orElse(null))) {
            state = CatalogItemState.INACTIVE;
            this.replacementId = replacementId.orElse(null);
            version++;
        }
    }

    public void reactivate(long expectedVersion) {
        verifyVersion(expectedVersion);
        if (state == CatalogItemState.INACTIVE) {
            state = CatalogItemState.ACTIVE;
            replacementId = null;
            version++;
        }
    }

    public CatalogItemReference reference() {
        return new CatalogItemReference(id, code.value(), name.value(), type, state, scopes, baseUnit.value(), version);
    }

    public CatalogItemSnapshot snapshot() {
        return new CatalogItemSnapshot(
                companyId,
                id,
                code,
                name,
                description,
                type,
                scopes,
                baseUnit,
                taxProfile,
                Optional.ofNullable(classification),
                Optional.ofNullable(variant),
                state,
                Optional.ofNullable(replacementId),
                identifiers.values().stream().toList(),
                conversions.values().stream().toList(),
                version);
    }

    private BigDecimal factorToBase(UnitCode unit) {
        if (baseUnit.equals(unit)) {
            return BigDecimal.ONE;
        }
        ItemUnitConversion conversion = conversions.get(unit);
        if (conversion == null || !conversion.active()) {
            throw new IllegalArgumentException("No active conversion exists for unit " + unit);
        }
        return conversion.toBaseFactor();
    }

    private void verifyVersion(long expectedVersion) {
        if (expectedVersion != version) {
            throw new ConcurrentCatalogChangeException(expectedVersion, version);
        }
    }

    private void requireActive() {
        if (state != CatalogItemState.ACTIVE) {
            throw new IllegalStateException("Inactive catalog item cannot be modified");
        }
    }

    private static Set<CatalogItemScope> copyScopes(Set<CatalogItemScope> scopes) {
        Set<CatalogItemScope> copy = Set.copyOf(Objects.requireNonNull(scopes, "scopes"));
        if (copy.isEmpty()) {
            throw new IllegalArgumentException("Catalog item must enable purchase, sale or both");
        }
        return copy;
    }

    public CompanyId companyId() { return companyId; }
    public CatalogItemId id() { return id; }
    public CatalogItemCode code() { return code; }
    public CatalogItemName name() { return name; }
    public String description() { return description; }
    public CatalogItemType type() { return type; }
    public Set<CatalogItemScope> scopes() { return scopes; }
    public UnitCode baseUnit() { return baseUnit; }
    public TaxProfileReference taxProfile() { return taxProfile; }
    public Optional<CatalogClassification> classification() { return Optional.ofNullable(classification); }
    public Optional<CatalogVariant> variant() { return Optional.ofNullable(variant); }
    public CatalogItemState state() { return state; }
    public Optional<CatalogItemId> replacementId() { return Optional.ofNullable(replacementId); }
    public Map<CatalogDetailId, CatalogItemIdentifier> identifiers() { return Map.copyOf(identifiers); }
    public Map<UnitCode, ItemUnitConversion> conversions() { return Map.copyOf(conversions); }
    public long version() { return version; }
}
