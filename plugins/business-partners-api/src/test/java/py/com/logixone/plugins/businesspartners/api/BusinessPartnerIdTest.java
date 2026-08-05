package py.com.logixone.plugins.businesspartners.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class BusinessPartnerIdTest {

    private static final String CANONICAL = "123e4567-e89b-42d3-a456-426614174000";

    @Test
    void parsesAndRendersOnlyCanonicalUuidValues() {
        BusinessPartnerId id = BusinessPartnerId.parse(CANONICAL);

        assertEquals(UUID.fromString(CANONICAL), id.value());
        assertEquals(CANONICAL, id.toString());
    }

    @Test
    void rejectsNullMalformedAndNonCanonicalValues() {
        assertThrows(NullPointerException.class, () -> BusinessPartnerId.parse(null));
        assertThrows(IllegalArgumentException.class, () -> BusinessPartnerId.parse("partner-one"));
        assertThrows(
                IllegalArgumentException.class,
                () -> BusinessPartnerId.parse(CANONICAL.toUpperCase()));
        assertThrows(NullPointerException.class, () -> new BusinessPartnerId(null));
    }
}
