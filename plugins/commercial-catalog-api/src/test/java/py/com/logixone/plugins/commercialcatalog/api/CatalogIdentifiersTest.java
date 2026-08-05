package py.com.logixone.plugins.commercialcatalog.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogIdentifiersTest {

    private static final String CANONICAL = "123e4567-e89b-42d3-a456-426614174000";

    @Test
    void publicIdsParseAndRenderCanonicalUuids() {
        assertEquals(CANONICAL, CatalogItemId.parse(CANONICAL).toString());
        assertEquals(CANONICAL, PriceListId.parse(CANONICAL).toString());
        assertEquals(CANONICAL, PriceEntryId.parse(CANONICAL).toString());
        assertEquals(UUID.fromString(CANONICAL), CatalogItemId.parse(CANONICAL).value());
    }

    @Test
    void publicIdsRejectMalformedOrNonCanonicalValues() {
        assertThrows(IllegalArgumentException.class, () -> CatalogItemId.parse("item-one"));
        assertThrows(
                IllegalArgumentException.class,
                () -> PriceListId.parse(CANONICAL.toUpperCase()));
        assertThrows(NullPointerException.class, () -> new PriceEntryId(null));
    }
}
