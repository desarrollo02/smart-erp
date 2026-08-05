package py.com.logixone.plugins.businesspartners.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BusinessPartnerCodeTest {

    @Test
    void normalizesUnicodeWhitespaceAndCaseDeterministically() {
        assertEquals("BP-001", new BusinessPartnerCode("  bp-001  ").value());
        assertEquals("BP-001", new BusinessPartnerCode("ＢＰ－００１").value());
    }

    @Test
    void rejectsBlankWhitespaceControlAndOversizedCodes() {
        assertThrows(IllegalArgumentException.class, () -> new BusinessPartnerCode("  "));
        assertThrows(IllegalArgumentException.class, () -> new BusinessPartnerCode("BP 001"));
        assertThrows(IllegalArgumentException.class, () -> new BusinessPartnerCode("BP\n001"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new BusinessPartnerCode("A".repeat(BusinessPartnerCode.MAX_LENGTH + 1)));
    }

    @Test
    void normalizesNamesWithoutMakingEmailMandatory() {
        assertEquals("María Acosta", new BusinessPartnerName("  María   Acosta ").value());
        assertThrows(IllegalArgumentException.class, () -> new BusinessPartnerName("\t"));
    }
}
