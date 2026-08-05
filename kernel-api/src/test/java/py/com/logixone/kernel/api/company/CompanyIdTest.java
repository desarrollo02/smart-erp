package py.com.logixone.kernel.api.company;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompanyIdTest {

    private static final String CANONICAL = "123e4567-e89b-42d3-a456-426614174000";

    @Test
    void parsesAndRendersOnlyTheCanonicalUuidRepresentation() {
        CompanyId id = CompanyId.parse(CANONICAL);

        assertEquals(UUID.fromString(CANONICAL), id.value());
        assertEquals(CANONICAL, id.toString());
    }

    @Test
    void rejectsNullMalformedAndNonCanonicalValues() {
        assertThrows(NullPointerException.class, () -> CompanyId.parse(null));
        assertThrows(IllegalArgumentException.class, () -> CompanyId.parse("company-one"));
        assertThrows(IllegalArgumentException.class, () -> CompanyId.parse(CANONICAL.toUpperCase()));
        assertThrows(NullPointerException.class, () -> new CompanyId(null));
    }

    @Test
    void providesDeterministicOrderingAndAReadOnlyContextContract() {
        CompanyId first = CompanyId.parse("00000000-0000-0000-0000-000000000001");
        CompanyId second = CompanyId.parse("00000000-0000-0000-0000-000000000002");
        CompanyContext context = () -> first;

        assertEquals(first, context.requiredCompanyId());
        assertEquals(-1, Integer.signum(first.compareTo(second)));
    }
}
