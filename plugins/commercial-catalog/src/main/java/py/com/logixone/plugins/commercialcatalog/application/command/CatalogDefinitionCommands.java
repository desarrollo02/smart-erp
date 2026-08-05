package py.com.logixone.plugins.commercialcatalog.application.command;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.plugins.commercialcatalog.application.definition.CatalogDefinitions;
import py.com.logixone.plugins.commercialcatalog.domain.CategoryId;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileId;
import py.com.logixone.plugins.commercialcatalog.domain.UnitCode;
import py.com.logixone.plugins.commercialcatalog.domain.VariantFamilyId;

/** Commands for controlled catalog definitions. */
public final class CatalogDefinitionCommands {

    private CatalogDefinitionCommands() {
    }

    public record RegisterUnit(UnitCode code, String displayName, int decimalScale) {
        public RegisterUnit {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(displayName, "displayName");
        }
    }

    public record RegisterCategory(
            Optional<CategoryId> parentId, String code, String displayName) {
        public RegisterCategory {
            parentId = Objects.requireNonNull(parentId, "parentId");
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(displayName, "displayName");
        }
    }

    public record RegisterBrand(String code, String displayName) {
        public RegisterBrand {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(displayName, "displayName");
        }
    }

    public record RegisterTag(String code, String displayName) {
        public RegisterTag {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(displayName, "displayName");
        }
    }

    public record ChangeSimpleState(
            CatalogDefinitions.SimpleKind kind,
            String identity,
            CatalogDefinitions.State targetState,
            long expectedVersion) {
        public ChangeSimpleState {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(identity, "identity");
            identity = identity.strip();
            if (identity.isEmpty() || identity.length() > 64) {
                throw new IllegalArgumentException("identity length must be between 1 and 64");
            }
            Objects.requireNonNull(targetState, "targetState");
            if (expectedVersion < 0) {
                throw new IllegalArgumentException("expectedVersion must not be negative");
            }
        }
    }

    public record ReviseSimpleDefinition(
            CatalogDefinitions.SimpleKind kind,
            String identity,
            String displayName,
            Optional<Integer> decimalScale,
            Optional<CategoryId> parentId,
            long expectedVersion) {
        public ReviseSimpleDefinition {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(identity, "identity");
            identity = identity.strip();
            if (identity.isEmpty() || identity.length() > 64) {
                throw new IllegalArgumentException("identity length must be between 1 and 64");
            }
            Objects.requireNonNull(displayName, "displayName");
            decimalScale = Objects.requireNonNull(decimalScale, "decimalScale");
            parentId = Objects.requireNonNull(parentId, "parentId");
            if (expectedVersion < 0) {
                throw new IllegalArgumentException("expectedVersion must not be negative");
            }
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

    public record ReplaceSimpleDefinition(
            CatalogDefinitions.SimpleKind kind,
            String identity,
            String replacementCode,
            String replacementDisplayName,
            Optional<Integer> replacementDecimalScale,
            Optional<CategoryId> replacementParentId,
            long expectedVersion) {
        public ReplaceSimpleDefinition {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(identity, "identity");
            identity = identity.strip();
            if (identity.isEmpty() || identity.length() > 64) {
                throw new IllegalArgumentException("identity length must be between 1 and 64");
            }
            Objects.requireNonNull(replacementCode, "replacementCode");
            Objects.requireNonNull(replacementDisplayName, "replacementDisplayName");
            replacementDecimalScale = Objects.requireNonNull(
                    replacementDecimalScale, "replacementDecimalScale");
            replacementParentId = Objects.requireNonNull(
                    replacementParentId, "replacementParentId");
            if (expectedVersion < 0) {
                throw new IllegalArgumentException("expectedVersion must not be negative");
            }
            switch (kind) {
                case UNIT -> {
                    int scale = replacementDecimalScale.orElseThrow(() ->
                            new IllegalArgumentException(
                                    "replacementDecimalScale is required for a unit"));
                    if (scale < 0 || scale > 12 || replacementParentId.isPresent()) {
                        throw new IllegalArgumentException("Invalid replacement unit fields");
                    }
                    if (new UnitCode(identity).equals(new UnitCode(replacementCode))) {
                        throw new IllegalArgumentException(
                                "A replacement unit requires a new code");
                    }
                }
                case CATEGORY -> {
                    if (replacementDecimalScale.isPresent()) {
                        throw new IllegalArgumentException(
                                "replacementDecimalScale does not apply to a category");
                    }
                    if (replacementParentId.isPresent()
                            && replacementParentId.orElseThrow().value().toString()
                                    .equals(identity)) {
                        throw new IllegalArgumentException(
                                "The replaced category cannot be the replacement parent");
                    }
                }
                case BRAND, TAG -> {
                    if (replacementDecimalScale.isPresent()
                            || replacementParentId.isPresent()) {
                        throw new IllegalArgumentException(
                                "Structural replacement fields do not apply to this definition");
                    }
                }
            }
        }
    }

    public record RegisterTaxProfile(
            String code,
            String displayName,
            String internalKindCode,
            String description,
            Instant validFrom,
            Optional<Instant> validUntil) {
        public RegisterTaxProfile {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(internalKindCode, "internalKindCode");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(validFrom, "validFrom");
            validUntil = Objects.requireNonNull(validUntil, "validUntil");
        }
    }

    public record ChangeTaxProfileState(
            TaxProfileId id,
            CatalogDefinitions.State targetState,
            long expectedVersion) {
        public ChangeTaxProfileState {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(targetState, "targetState");
            if (expectedVersion < 0) {
                throw new IllegalArgumentException("expectedVersion must not be negative");
            }
        }
    }

    public record ReviseTaxProfile(
            TaxProfileId id,
            String internalKindCode,
            String description,
            Instant validFrom,
            Optional<Instant> validUntil,
            long expectedVersion) {
        public ReviseTaxProfile {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(internalKindCode, "internalKindCode");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(validFrom, "validFrom");
            validUntil = Objects.requireNonNull(validUntil, "validUntil");
            if (expectedVersion < 0) {
                throw new IllegalArgumentException("expectedVersion must not be negative");
            }
        }
    }

    public record RegisterVariantFamily(
            String code,
            String displayName,
            List<CatalogDefinitions.VariantAttribute> attributes) {
        public RegisterVariantFamily {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(displayName, "displayName");
            attributes = List.copyOf(Objects.requireNonNull(attributes, "attributes"));
        }
    }

    public record ChangeVariantFamilyState(
            VariantFamilyId id,
            CatalogDefinitions.State targetState,
            long expectedVersion) {
        public ChangeVariantFamilyState {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(targetState, "targetState");
            if (expectedVersion < 0) {
                throw new IllegalArgumentException("expectedVersion must not be negative");
            }
        }
    }

    public record ReviseVariantFamily(
            VariantFamilyId id,
            String displayName,
            List<CatalogDefinitions.VariantAttribute> attributes,
            long expectedVersion) {
        public ReviseVariantFamily {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(displayName, "displayName");
            attributes = List.copyOf(Objects.requireNonNull(attributes, "attributes"));
            if (expectedVersion < 0) {
                throw new IllegalArgumentException("expectedVersion must not be negative");
            }
        }
    }
}
