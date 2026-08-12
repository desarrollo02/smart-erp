package py.com.logixone.plugins.commercialcatalog.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogReferenceAndSearchTest {

    @Test
    void referenceNormalizesCodesAndCopiesScopes() {
        Set<CatalogItemScope> scopes = EnumSet.of(CatalogItemScope.SALE);
        CatalogItemReference reference = new CatalogItemReference(
                new CatalogItemId(UUID.randomUUID()),
                " sku-01 ",
                " Producto demo ",
                CatalogItemType.PRODUCT,
                CatalogItemState.ACTIVE,
                scopes,
                " un ",
                3);
        scopes.clear();

        assertEquals("SKU-01", reference.code());
        assertEquals("UN", reference.baseUnitCode());
        assertEquals(Set.of(CatalogItemScope.SALE), reference.scopes());
    }

    @Test
    void referenceRequiresAtLeastOneScope() {
        assertThrows(IllegalArgumentException.class, () -> new CatalogItemReference(
                new CatalogItemId(UUID.randomUUID()),
                "SKU",
                "Demo",
                CatalogItemType.PRODUCT,
                CatalogItemState.ACTIVE,
                Set.of(),
                "UN",
                0));
    }

    @Test
    void searchCriteriaAndPageAreBoundedAndImmutable() {
        CatalogSearchCriteria criteria = new CatalogSearchCriteria(
                " demo ", Set.of(), Set.of(CatalogItemState.ACTIVE),
                Set.of(CatalogItemScope.PURCHASE), 0, 25);
        ArrayList<CatalogItemReference> mutable = new ArrayList<>();
        CatalogSearchPage page = new CatalogSearchPage(mutable, 0, 0, 25);
        mutable.add(null);

        assertEquals("demo", criteria.query());
        assertEquals(Set.of(CatalogItemScope.PURCHASE), criteria.scopes());
        assertEquals("1.1.0", CatalogContractVersion.CURRENT);
        assertEquals(List.of(), page.items());
        assertThrows(
                IllegalArgumentException.class,
                () -> new CatalogSearchCriteria("", Set.of(), Set.of(), 0, 101));
    }
}
