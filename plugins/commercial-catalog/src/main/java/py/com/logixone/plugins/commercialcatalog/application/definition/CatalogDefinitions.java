package py.com.logixone.plugins.commercialcatalog.application.definition;

import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import py.com.logixone.plugins.commercialcatalog.domain.BrandId;
import py.com.logixone.plugins.commercialcatalog.domain.CategoryId;
import py.com.logixone.plugins.commercialcatalog.domain.TagId;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileId;
import py.com.logixone.plugins.commercialcatalog.domain.UnitCode;
import py.com.logixone.plugins.commercialcatalog.domain.VariantAttributeCode;
import py.com.logixone.plugins.commercialcatalog.domain.VariantFamilyId;
import py.com.logixone.plugins.commercialcatalog.domain.VariantValueType;

/** Neutral projections for company-owned catalog definitions. */
public final class CatalogDefinitions {

    private CatalogDefinitions() {
    }

    public enum State { ACTIVE, INACTIVE }

    public enum SimpleKind { UNIT, CATEGORY, BRAND, TAG }

    public record Lifecycle(
            SimpleKind kind,
            String identity,
            State state,
            long version,
            boolean changed) {
        public Lifecycle {
            Objects.requireNonNull(kind, "kind");
            identity = text(identity, "identity", 64);
            Objects.requireNonNull(state, "state");
            requireVersion(version);
        }
    }

    public record SimpleRevision(
            SimpleKind kind,
            String identity,
            long version,
            String displayName,
            Optional<Integer> decimalScale,
            Optional<CategoryId> parentId,
            State state,
            boolean current) {
        public SimpleRevision {
            Objects.requireNonNull(kind, "kind");
            identity = text(identity, "identity", 64);
            requireVersion(version);
            displayName = text(displayName, "displayName",
                    kind == SimpleKind.UNIT || kind == SimpleKind.TAG ? 120 : 200);
            decimalScale = Objects.requireNonNull(decimalScale, "decimalScale");
            parentId = Objects.requireNonNull(parentId, "parentId");
            Objects.requireNonNull(state, "state");
            switch (kind) {
                case UNIT -> {
                    int scale = decimalScale.orElseThrow(() ->
                            new IllegalArgumentException("decimalScale is required for a unit"));
                    if (scale < 0 || scale > 12 || parentId.isPresent()) {
                        throw new IllegalArgumentException("Invalid unit revision fields");
                    }
                }
                case CATEGORY -> {
                    if (decimalScale.isPresent()) {
                        throw new IllegalArgumentException(
                                "decimalScale does not apply to a category");
                    }
                    if (parentId.isPresent()
                            && parentId.orElseThrow().value().toString().equals(identity)) {
                        throw new IllegalArgumentException(
                                "A category cannot be its own parent");
                    }
                }
                case BRAND, TAG -> {
                    if (decimalScale.isPresent() || parentId.isPresent()) {
                        throw new IllegalArgumentException(
                                "Structural fields do not apply to this definition");
                    }
                }
            }
        }
    }

    public record ReplacementCandidate(
            SimpleKind kind,
            String identity,
            String code,
            String displayName,
            Optional<Integer> decimalScale,
            Optional<CategoryId> parentId) {
        public ReplacementCandidate {
            Objects.requireNonNull(kind, "kind");
            decimalScale = Objects.requireNonNull(decimalScale, "decimalScale");
            parentId = Objects.requireNonNull(parentId, "parentId");
            switch (kind) {
                case UNIT -> {
                    code = new UnitCode(code).value();
                    identity = new UnitCode(identity).value();
                    if (!identity.equals(code)) {
                        throw new IllegalArgumentException(
                                "A unit replacement identity must equal its code");
                    }
                    displayName = text(displayName, "displayName", 120);
                    int scale = decimalScale.orElseThrow(() ->
                            new IllegalArgumentException("decimalScale is required for a unit"));
                    if (scale < 0 || scale > 12 || parentId.isPresent()) {
                        throw new IllegalArgumentException("Invalid unit replacement fields");
                    }
                }
                case CATEGORY -> {
                    identity = UUID.fromString(identity).toString();
                    code = normalizedCode(code, "code", 64);
                    displayName = text(displayName, "displayName", 200);
                    if (decimalScale.isPresent()) {
                        throw new IllegalArgumentException(
                                "decimalScale does not apply to a category");
                    }
                    if (parentId.isPresent()
                            && parentId.orElseThrow().value().toString().equals(identity)) {
                        throw new IllegalArgumentException(
                                "A category cannot be its own parent");
                    }
                }
                case BRAND -> {
                    identity = UUID.fromString(identity).toString();
                    code = normalizedCode(code, "code", 64);
                    displayName = text(displayName, "displayName", 200);
                    if (decimalScale.isPresent() || parentId.isPresent()) {
                        throw new IllegalArgumentException(
                                "Structural fields do not apply to a brand");
                    }
                }
                case TAG -> {
                    identity = UUID.fromString(identity).toString();
                    code = normalizedCode(code, "code", 64);
                    displayName = text(displayName, "displayName", 120);
                    if (decimalScale.isPresent() || parentId.isPresent()) {
                        throw new IllegalArgumentException(
                                "Structural fields do not apply to a tag");
                    }
                }
            }
        }
    }

    public record Replacement(
            SimpleKind kind,
            String previousIdentity,
            long previousVersion,
            String replacementIdentity,
            String replacementCode,
            long replacementVersion) {
        public Replacement {
            Objects.requireNonNull(kind, "kind");
            previousIdentity = text(previousIdentity, "previousIdentity", 64);
            replacementIdentity = text(replacementIdentity, "replacementIdentity", 64);
            replacementCode = text(replacementCode, "replacementCode", 64);
            requireVersion(previousVersion);
            requireVersion(replacementVersion);
            if (previousIdentity.equals(replacementIdentity)) {
                throw new IllegalArgumentException(
                        "A replacement requires a different identity");
            }
        }
    }

    public record ReplacementLink(
            SimpleKind kind, String previousIdentity, String replacementIdentity) {
        public ReplacementLink {
            Objects.requireNonNull(kind, "kind");
            previousIdentity = text(previousIdentity, "previousIdentity", 64);
            replacementIdentity = text(replacementIdentity, "replacementIdentity", 64);
            if (previousIdentity.equals(replacementIdentity)) {
                throw new IllegalArgumentException(
                        "A replacement link requires different identities");
            }
        }
    }

    public record Unit(
            UnitCode code, String displayName, int decimalScale, State state, long version) {
        public Unit {
            Objects.requireNonNull(code, "code");
            displayName = text(displayName, "displayName", 120);
            if (decimalScale < 0 || decimalScale > 12) {
                throw new IllegalArgumentException("decimalScale must be between 0 and 12");
            }
            Objects.requireNonNull(state, "state");
            requireVersion(version);
        }
    }

    public record Category(
            CategoryId id,
            Optional<CategoryId> parentId,
            String code,
            String displayName,
            State state,
            long version) {
        public Category {
            Objects.requireNonNull(id, "id");
            parentId = Objects.requireNonNull(parentId, "parentId");
            if (parentId.filter(id::equals).isPresent()) {
                throw new IllegalArgumentException("A category cannot be its own parent");
            }
            code = normalizedCode(code, "code", 64);
            displayName = text(displayName, "displayName", 200);
            Objects.requireNonNull(state, "state");
            requireVersion(version);
        }
    }

    public record Brand(
            BrandId id, String code, String displayName, State state, long version) {
        public Brand {
            Objects.requireNonNull(id, "id");
            code = normalizedCode(code, "code", 64);
            displayName = text(displayName, "displayName", 200);
            Objects.requireNonNull(state, "state");
            requireVersion(version);
        }
    }

    public record Tag(
            TagId id, String code, String displayName, State state, long version) {
        public Tag {
            Objects.requireNonNull(id, "id");
            code = normalizedCode(code, "code", 64);
            displayName = text(displayName, "displayName", 120);
            Objects.requireNonNull(state, "state");
            requireVersion(version);
        }
    }

    public record TaxProfile(
            TaxProfileId id,
            String code,
            String displayName,
            String internalKindCode,
            String description,
            Instant validFrom,
            Optional<Instant> validUntil,
            State state,
            long version) {
        public TaxProfile {
            Objects.requireNonNull(id, "id");
            code = normalizedCode(code, "code", 64);
            displayName = text(displayName, "displayName", 200);
            internalKindCode = normalizedCode(internalKindCode, "internalKindCode", 48);
            description = text(description, "description", 250);
            Objects.requireNonNull(validFrom, "validFrom");
            validUntil = Objects.requireNonNull(validUntil, "validUntil");
            validUntil.ifPresent(end -> {
                if (!end.isAfter(validFrom)) {
                    throw new IllegalArgumentException("validUntil must be after validFrom");
                }
            });
            Objects.requireNonNull(state, "state");
            requireVersion(version);
        }
    }

    public record TaxProfileRevision(
            TaxProfileId profileId,
            long version,
            String internalKindCode,
            String description,
            Instant validFrom,
            Optional<Instant> validUntil,
            boolean current) {
        public TaxProfileRevision {
            Objects.requireNonNull(profileId, "profileId");
            requireVersion(version);
            internalKindCode = normalizedCode(internalKindCode, "internalKindCode", 48);
            description = text(description, "description", 250);
            Objects.requireNonNull(validFrom, "validFrom");
            validUntil = Objects.requireNonNull(validUntil, "validUntil");
            validUntil.ifPresent(end -> {
                if (!end.isAfter(validFrom)) {
                    throw new IllegalArgumentException("validUntil must be after validFrom");
                }
            });
        }
    }

    public record VariantAttribute(
            VariantAttributeCode code,
            String displayName,
            VariantValueType valueType,
            boolean required,
            int position) {
        public VariantAttribute {
            Objects.requireNonNull(code, "code");
            displayName = text(displayName, "displayName", 120);
            Objects.requireNonNull(valueType, "valueType");
            if (position < 0) {
                throw new IllegalArgumentException("position must not be negative");
            }
        }
    }

    public record VariantFamily(
            VariantFamilyId id,
            String code,
            String displayName,
            List<VariantAttribute> attributes,
            State state,
            long version) {
        public VariantFamily {
            Objects.requireNonNull(id, "id");
            code = normalizedCode(code, "code", 64);
            displayName = text(displayName, "displayName", 200);
            attributes = variantAttributes(attributes);
            Objects.requireNonNull(state, "state");
            requireVersion(version);
        }
    }

    public record VariantFamilyRevision(
            VariantFamilyId familyId,
            long version,
            String displayName,
            List<VariantAttribute> attributes,
            State state,
            boolean current) {
        public VariantFamilyRevision {
            Objects.requireNonNull(familyId, "familyId");
            requireVersion(version);
            displayName = text(displayName, "displayName", 200);
            attributes = variantAttributes(attributes);
            Objects.requireNonNull(state, "state");
        }
    }

    public record Snapshot(
            List<Unit> units,
            List<Category> categories,
            List<Brand> brands,
            List<Tag> tags,
            List<TaxProfile> taxProfiles,
            List<VariantFamily> variantFamilies,
            List<ReplacementLink> replacements) {
        public Snapshot {
            units = List.copyOf(Objects.requireNonNull(units, "units"));
            categories = List.copyOf(Objects.requireNonNull(categories, "categories"));
            brands = List.copyOf(Objects.requireNonNull(brands, "brands"));
            tags = List.copyOf(Objects.requireNonNull(tags, "tags"));
            taxProfiles = List.copyOf(Objects.requireNonNull(taxProfiles, "taxProfiles"));
            variantFamilies = List.copyOf(Objects.requireNonNull(
                    variantFamilies, "variantFamilies"));
            replacements = List.copyOf(Objects.requireNonNull(replacements, "replacements"));
        }

        public Snapshot(
                List<Unit> units,
                List<Category> categories,
                List<Brand> brands,
                List<Tag> tags,
                List<TaxProfile> taxProfiles,
                List<VariantFamily> variantFamilies) {
            this(units, categories, brands, tags, taxProfiles, variantFamilies, List.of());
        }
    }

    private static String text(String value, String name, int maxLength) {
        Objects.requireNonNull(value, name);
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    name + " length must be between 1 and " + maxLength);
        }
        return normalized;
    }

    private static String normalizedCode(String value, String name, int maxLength) {
        return text(value, name, maxLength).toUpperCase(Locale.ROOT);
    }

    private static List<VariantAttribute> variantAttributes(List<VariantAttribute> values) {
        List<VariantAttribute> attributes = List.copyOf(
                Objects.requireNonNull(values, "attributes"));
        if (attributes.isEmpty() || attributes.size() > 8) {
            throw new IllegalArgumentException(
                    "A variant family must contain between 1 and 8 attributes");
        }
        if (attributes.stream().map(VariantAttribute::code).distinct().count()
                != attributes.size()) {
            throw new IllegalArgumentException("Variant attribute codes must be unique");
        }
        if (attributes.stream().map(VariantAttribute::position).distinct().count()
                != attributes.size()) {
            throw new IllegalArgumentException("Variant attribute positions must be unique");
        }
        return attributes;
    }

    private static void requireVersion(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }
}
