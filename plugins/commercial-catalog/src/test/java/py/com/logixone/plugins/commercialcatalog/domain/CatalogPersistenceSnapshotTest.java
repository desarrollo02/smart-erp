package py.com.logixone.plugins.commercialcatalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemScope;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemState;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemType;
import py.com.logixone.plugins.commercialcatalog.api.CatalogTaxMode;
import py.com.logixone.plugins.commercialcatalog.api.PriceEntryId;
import py.com.logixone.plugins.commercialcatalog.api.PriceListId;

class CatalogPersistenceSnapshotTest {

    @Test
    void restoresACompleteCatalogItemWithoutChangingItsPersistedVersion() {
        CatalogItem item = CatalogItem.create(
                company(), new CatalogItemId(uuid(2)), new CatalogItemCode("SKU-1"),
                new CatalogItemName("Blue shirt"), "Cotton", CatalogItemType.PRODUCT,
                Set.of(CatalogItemScope.PURCHASE, CatalogItemScope.SALE), new UnitCode("EA"),
                new TaxProfileReference(new TaxProfileId(uuid(3)), 2));
        item.addIdentifier(CatalogItemIdentifier.active(
                new CatalogDetailId(uuid(4)), "EAN", "784 001"), 0);
        item.addUnitConversion(new ItemUnitConversion(
                new UnitCode("BOX"), new BigDecimal("12"), Set.of(UnitPurpose.SALE),
                Set.of(UnitPurpose.SALE), true), 1);
        item.classify(new CatalogClassification(
                new CategoryId(uuid(5)), Set.of(new CategoryId(uuid(6))),
                Optional.of(new BrandId(uuid(7))), Set.of(new TagId(uuid(8)))), 2);
        item.assignVariant(new CatalogVariant(
                new VariantFamilyId(uuid(9)),
                4,
                Map.of(new VariantAttributeCode("COLOR"),
                        new VariantAttributeValue(VariantValueType.TEXT, "Blue"))), 3);
        item.inactivate(Optional.of(new CatalogItemId(uuid(10))), 4);

        CatalogItem restored = CatalogItem.restore(item.snapshot());

        assertNotSame(item, restored);
        assertEquals(item.snapshot(), restored.snapshot());
        assertEquals(CatalogItemState.INACTIVE, restored.state());
        assertEquals(5, restored.version());
    }

    @Test
    void restoresPriceEntriesAndRejectsAnAmbiguousPersistedSnapshot() {
        PriceList list = PriceList.create(
                company(), new PriceListId(uuid(20)), new PriceListCode("RETAIL"),
                new PriceListName("Retail"), "PYG", CatalogTaxMode.TAX_INCLUDED,
                2, RoundingMode.HALF_UP);
        PriceEntry entry = PriceEntry.active(
                new PriceEntryId(uuid(21)), new CatalogItemId(uuid(2)), new UnitCode("EA"),
                BigDecimal.ONE, new BigDecimal("15000"), Instant.parse("2026-01-01T00:00:00Z"),
                Optional.empty());
        list.addEntry(entry, 0);

        PriceList restored = PriceList.restore(list.snapshot());

        assertEquals(list.snapshot(), restored.snapshot());
        PriceEntry overlap = PriceEntry.active(
                new PriceEntryId(uuid(22)), entry.itemId(), entry.unit(), BigDecimal.ONE,
                new BigDecimal("16000"), Instant.parse("2026-02-01T00:00:00Z"), Optional.empty());
        PriceListSnapshot invalid = new PriceListSnapshot(
                company(), list.id(), list.code(), list.name(), list.currency(), list.taxMode(),
                list.scale(), list.roundingMode(), PriceListState.ACTIVE,
                java.util.List.of(entry, overlap), 2);
        assertThrows(IllegalArgumentException.class, () -> PriceList.restore(invalid));
    }

    private static CompanyId company() { return new CompanyId(uuid(1)); }
    private static UUID uuid(long suffix) { return new UUID(0, suffix); }
}
