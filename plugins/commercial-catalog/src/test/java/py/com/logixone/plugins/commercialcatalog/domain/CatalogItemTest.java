package py.com.logixone.plugins.commercialcatalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
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

class CatalogItemTest {

    @Test
    void createsOneAggregateForAProductOrServiceWithImmutableTypeAndRequiredScope() {
        CatalogItem item = item(CatalogItemType.SERVICE, Set.of(CatalogItemScope.SALE));

        assertEquals(CatalogItemType.SERVICE, item.type());
        assertEquals("CONSULTING", item.code().value());
        assertEquals("Consulting", item.name().value());
        assertEquals(CatalogItemState.ACTIVE, item.state());
        assertEquals(0, item.version());
        assertThrows(IllegalArgumentException.class,
                () -> item(CatalogItemType.PRODUCT, Set.of()));
    }

    @Test
    void ownsTypedIdentifiersAndRejectsAnActiveDuplicate() {
        CatalogItem item = item(CatalogItemType.PRODUCT, Set.of(CatalogItemScope.PURCHASE, CatalogItemScope.SALE));
        CatalogItemIdentifier identifier = CatalogItemIdentifier.active(
                detailId(10), "ean", " 7 841234 ");

        item.addIdentifier(identifier, 0);

        assertEquals("EAN", item.identifiers().get(detailId(10)).typeCode());
        assertEquals("7841234", item.identifiers().get(detailId(10)).normalizedValue());
        assertThrows(IllegalArgumentException.class, () -> item.addIdentifier(
                CatalogItemIdentifier.active(detailId(11), "EAN", "7841234"), 1));

        item.inactivateIdentifier(detailId(10), 1);
        item.addIdentifier(CatalogItemIdentifier.active(detailId(11), "EAN", "7841234"), 2);
        assertEquals(3, item.version());
    }

    @Test
    void convertsThroughBaseUnitAndAllowsOnlyOneDefaultForEachPurpose() {
        CatalogItem item = item(CatalogItemType.PRODUCT, Set.of(CatalogItemScope.SALE));
        item.addUnitConversion(new ItemUnitConversion(
                new UnitCode("BOX"), new BigDecimal("12"),
                Set.of(UnitPurpose.SALE), Set.of(UnitPurpose.SALE), true), 0);
        item.addUnitConversion(new ItemUnitConversion(
                new UnitCode("PALLET"), new BigDecimal("120"),
                Set.of(UnitPurpose.PURCHASE), Set.of(UnitPurpose.PURCHASE), true), 1);

        var result = item.convert(new UnitCode("PALLET"), new UnitCode("BOX"), new BigDecimal("2"));
        assertEquals(0, new BigDecimal("20").compareTo(result.convertedQuantity()));
        assertEquals(2, result.itemVersion());

        assertThrows(IllegalArgumentException.class, () -> item.addUnitConversion(
                new ItemUnitConversion(new UnitCode("PACK"), new BigDecimal("6"),
                        Set.of(UnitPurpose.SALE), Set.of(UnitPurpose.SALE), true), 2));
        assertThrows(IllegalArgumentException.class, () -> item.addUnitConversion(
                new ItemUnitConversion(new UnitCode("EA"), BigDecimal.ONE,
                        Set.of(UnitPurpose.SALE), Set.of(), true), 2));
    }

    @Test
    void assignsControlledClassificationTaxProfileAndExplicitVariant() {
        CatalogItem item = item(CatalogItemType.PRODUCT, Set.of(CatalogItemScope.SALE));
        CategoryId main = new CategoryId(uuid(20));
        CatalogClassification classification = new CatalogClassification(
                main,
                Set.of(new CategoryId(uuid(21))),
                Optional.of(new BrandId(uuid(22))),
                Set.of(new TagId(uuid(23))));
        CatalogVariant variant = new CatalogVariant(
                new VariantFamilyId(uuid(24)),
                3,
                Map.of(
                        new VariantAttributeCode("color"), new VariantAttributeValue(VariantValueType.TEXT, "Blue"),
                        new VariantAttributeCode("size"), new VariantAttributeValue(VariantValueType.NUMBER, "42.0")));

        item.classify(classification, 0);
        item.assignVariant(variant, 1);
        TaxProfileReference changedTax = new TaxProfileReference(new TaxProfileId(uuid(25)), 4);
        item.assignTaxProfile(changedTax, 2);

        assertEquals(classification, item.classification().orElseThrow());
        assertEquals("42", item.variant().orElseThrow().attributes().get(new VariantAttributeCode("SIZE")).value());
        assertEquals(changedTax, item.taxProfile());
        assertThrows(IllegalArgumentException.class,
                () -> new CatalogClassification(main, Set.of(main), Optional.empty(), Set.of()));
    }

    @Test
    void inactivatesWithReplacementPreservesIdentityAndRejectsStaleMutation() {
        CatalogItem item = item(CatalogItemType.PRODUCT, Set.of(CatalogItemScope.SALE));
        CatalogItemId replacement = new CatalogItemId(uuid(30));

        assertThrows(IllegalArgumentException.class, () -> item.inactivate(Optional.of(item.id()), 0));
        item.inactivate(Optional.of(replacement), 0);

        assertEquals(CatalogItemState.INACTIVE, item.reference().state());
        assertEquals(replacement, item.replacementId().orElseThrow());
        assertEquals(1, item.version());
        assertThrows(IllegalStateException.class, () -> item.reviseIdentity(
                new CatalogItemCode("NEW"), new CatalogItemName("New"), "", Set.of(CatalogItemScope.SALE), 1));
        ConcurrentCatalogChangeException stale = assertThrows(
                ConcurrentCatalogChangeException.class, () -> item.reactivate(0));
        assertEquals(0, stale.expectedVersion());
        assertEquals(1, stale.actualVersion());

        item.reactivate(1);
        assertTrue(item.replacementId().isEmpty());
        assertFalse(item.state() == CatalogItemState.INACTIVE);
    }

    private static CatalogItem item(CatalogItemType type, Set<CatalogItemScope> scopes) {
        return CatalogItem.create(
                new CompanyId(uuid(1)),
                new CatalogItemId(uuid(2)),
                new CatalogItemCode(" consulting "),
                new CatalogItemName(" Consulting "),
                "",
                type,
                scopes,
                new UnitCode("EA"),
                new TaxProfileReference(new TaxProfileId(uuid(3)), 0));
    }

    private static CatalogDetailId detailId(long suffix) {
        return new CatalogDetailId(uuid(suffix));
    }

    private static UUID uuid(long suffix) {
        return new UUID(0, suffix);
    }
}
