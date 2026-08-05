package py.com.logixone.plugins.commercialcatalog.application.command;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemScope;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemType;
import py.com.logixone.plugins.commercialcatalog.api.CatalogTaxMode;
import py.com.logixone.plugins.commercialcatalog.api.PriceEntryId;
import py.com.logixone.plugins.commercialcatalog.api.PriceListId;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogClassification;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogDetailId;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemCode;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemName;
import py.com.logixone.plugins.commercialcatalog.domain.ItemUnitConversion;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListCode;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListName;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListState;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileReference;
import py.com.logixone.plugins.commercialcatalog.domain.UnitCode;
import py.com.logixone.plugins.commercialcatalog.domain.VariantAttributeCode;
import py.com.logixone.plugins.commercialcatalog.domain.VariantFamilyId;

/** Validated command payloads for catalog items and price lists. */
public final class CatalogCommands {

    private CatalogCommands() {
    }

    public record RegisterItem(
            Optional<CatalogItemCode> code,
            CatalogItemName name,
            String description,
            CatalogItemType type,
            Set<CatalogItemScope> scopes,
            UnitCode baseUnit,
            TaxProfileReference taxProfile) {
        public RegisterItem {
            code = Objects.requireNonNull(code, "code");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(type, "type");
            scopes = Set.copyOf(Objects.requireNonNull(scopes, "scopes"));
            Objects.requireNonNull(baseUnit, "baseUnit");
            Objects.requireNonNull(taxProfile, "taxProfile");
        }
    }

    public record ReviseItem(
            CatalogItemId id,
            long expectedVersion,
            CatalogItemCode code,
            CatalogItemName name,
            String description,
            Set<CatalogItemScope> scopes) {
        public ReviseItem {
            requireItemMutation(id, expectedVersion);
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(description, "description");
            scopes = Set.copyOf(Objects.requireNonNull(scopes, "scopes"));
        }
    }

    public record AddIdentifier(
            CatalogItemId id,
            long expectedVersion,
            String typeCode,
            String presentedValue) {
        public AddIdentifier {
            requireItemMutation(id, expectedVersion);
            Objects.requireNonNull(typeCode, "typeCode");
            Objects.requireNonNull(presentedValue, "presentedValue");
        }
    }

    public record InactivateIdentifier(
            CatalogItemId id, long expectedVersion, CatalogDetailId identifierId) {
        public InactivateIdentifier {
            requireItemMutation(id, expectedVersion);
            Objects.requireNonNull(identifierId, "identifierId");
        }
    }

    public record AddUnitConversion(
            CatalogItemId id, long expectedVersion, ItemUnitConversion conversion) {
        public AddUnitConversion {
            requireItemMutation(id, expectedVersion);
            Objects.requireNonNull(conversion, "conversion");
        }
    }

    public record Classify(
            CatalogItemId id, long expectedVersion, CatalogClassification classification) {
        public Classify {
            requireItemMutation(id, expectedVersion);
            Objects.requireNonNull(classification, "classification");
        }
    }

    public record AssignTaxProfile(
            CatalogItemId id, long expectedVersion, TaxProfileReference taxProfile) {
        public AssignTaxProfile {
            requireItemMutation(id, expectedVersion);
            Objects.requireNonNull(taxProfile, "taxProfile");
        }
    }

    public record AssignVariant(
            CatalogItemId id,
            long expectedVersion,
            VariantFamilyId familyId,
            long familyVersion,
            Map<VariantAttributeCode, String> attributes) {
        public AssignVariant {
            requireItemMutation(id, expectedVersion);
            Objects.requireNonNull(familyId, "familyId");
            requireVersion(familyVersion);
            attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes"));
            if (attributes.isEmpty()) {
                throw new IllegalArgumentException("Variant attributes must not be empty");
            }
        }
    }

    public record ChangeItemLifecycle(
            CatalogItemId id,
            long expectedVersion,
            CatalogItemState state,
            Optional<CatalogItemId> replacementId) {
        public ChangeItemLifecycle {
            requireItemMutation(id, expectedVersion);
            Objects.requireNonNull(state, "state");
            replacementId = Objects.requireNonNull(replacementId, "replacementId");
            if (state == CatalogItemState.ACTIVE && replacementId.isPresent()) {
                throw new IllegalArgumentException("A reactivation cannot define a replacement");
            }
        }
    }

    public record RegisterPriceList(
            Optional<PriceListCode> code,
            PriceListName name,
            String currency,
            CatalogTaxMode taxMode,
            int scale,
            RoundingMode roundingMode) {
        public RegisterPriceList {
            code = Objects.requireNonNull(code, "code");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(currency, "currency");
            Objects.requireNonNull(taxMode, "taxMode");
            Objects.requireNonNull(roundingMode, "roundingMode");
        }
    }

    public record RenamePriceList(
            PriceListId id, long expectedVersion, PriceListName name) {
        public RenamePriceList {
            requirePriceListMutation(id, expectedVersion);
            Objects.requireNonNull(name, "name");
        }
    }

    public record AddPriceEntry(
            PriceListId id,
            long expectedVersion,
            CatalogItemId itemId,
            UnitCode unit,
            BigDecimal minimumQuantity,
            BigDecimal amount,
            Instant validFrom,
            Optional<Instant> validUntil) {
        public AddPriceEntry {
            requirePriceListMutation(id, expectedVersion);
            Objects.requireNonNull(itemId, "itemId");
            Objects.requireNonNull(unit, "unit");
            Objects.requireNonNull(minimumQuantity, "minimumQuantity");
            Objects.requireNonNull(amount, "amount");
            Objects.requireNonNull(validFrom, "validFrom");
            validUntil = Objects.requireNonNull(validUntil, "validUntil");
        }
    }

    public record InactivatePriceEntry(
            PriceListId id, long expectedVersion, PriceEntryId entryId) {
        public InactivatePriceEntry {
            requirePriceListMutation(id, expectedVersion);
            Objects.requireNonNull(entryId, "entryId");
        }
    }

    public record ChangePriceListLifecycle(
            PriceListId id, long expectedVersion, PriceListState state) {
        public ChangePriceListLifecycle {
            requirePriceListMutation(id, expectedVersion);
            Objects.requireNonNull(state, "state");
        }
    }

    private static void requireItemMutation(CatalogItemId id, long expectedVersion) {
        Objects.requireNonNull(id, "id");
        requireVersion(expectedVersion);
    }

    private static void requirePriceListMutation(PriceListId id, long expectedVersion) {
        Objects.requireNonNull(id, "id");
        requireVersion(expectedVersion);
    }

    private static void requireVersion(long expectedVersion) {
        if (expectedVersion < 0) {
            throw new IllegalArgumentException("expectedVersion must not be negative");
        }
    }
}
