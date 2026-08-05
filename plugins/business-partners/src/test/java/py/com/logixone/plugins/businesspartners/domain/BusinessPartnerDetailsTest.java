package py.com.logixone.plugins.businesspartners.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerKind;

class BusinessPartnerDetailsTest {

    private static final BusinessPartnerAttributeCode TAX_ID = code("tax_id");
    private static final BusinessPartnerAttributeCode LEGAL = code("legal");
    private static final BusinessPartnerAttributeCode BILLING = code("billing");
    private static final BusinessPartnerAttributeCode EMAIL = code("email");
    private static final BusinessPartnerAttributeCode GENERAL = code("general");

    @Test
    void preservesPresentedIdentificationAndReportsDuplicateCandidates() {
        BusinessPartner partner = newOrganization();
        BusinessPartnerIdentification first = identification(
                "00000000-0000-0000-0000-000000000201", "800.123-4");
        BusinessPartnerIdentification candidate = identification(
                "00000000-0000-0000-0000-000000000202", "8001234");

        partner.addIdentification(0, first);

        assertEquals("800.123-4", first.presentedValue());
        assertEquals("8001234", first.normalizedValue());
        assertTrue(partner.hasPotentialDuplicate(candidate));

        partner.addIdentification(1, candidate);
        assertEquals(2, partner.identifications().size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> partner.identifications().clear());
    }

    @Test
    void replacingAPrimaryAddressIsExplicitAndScopedByTypeAndPurpose() {
        BusinessPartner partner = newOrganization();
        BusinessPartnerAddress first = address(
                "00000000-0000-0000-0000-000000000301", LEGAL, BILLING, true);
        BusinessPartnerAddress second = address(
                "00000000-0000-0000-0000-000000000302", LEGAL, BILLING, true);
        BusinessPartnerAddress delivery = address(
                "00000000-0000-0000-0000-000000000303", LEGAL, code("delivery"), true);

        partner.addAddress(0, first);
        partner.addAddress(1, second);
        partner.addAddress(2, delivery);

        assertFalse(partner.addresses().get(0).primary());
        assertTrue(partner.addresses().get(1).primary());
        assertTrue(partner.addresses().get(2).primary());
        assertEquals(Optional.of("PY"), partner.addresses().get(1).countryCode());
    }

    @Test
    void replacingAPrimaryChannelDoesNotRequireUniversalEmail() {
        BusinessPartner partner = newOrganization();
        BusinessPartnerContactChannel first = channel(
                "00000000-0000-0000-0000-000000000401", "first@example.test", true);
        BusinessPartnerContactChannel second = channel(
                "00000000-0000-0000-0000-000000000402", "+595981000000", true);

        partner.addContactChannel(0, first);
        partner.addContactChannel(1, second);

        assertFalse(partner.channels().get(0).primary());
        assertTrue(partner.channels().get(1).primary());

        BusinessPartner withoutEmail = newOrganization();
        assertTrue(withoutEmail.channels().isEmpty());
    }

    @Test
    void keepsNamedContactsLightweightAndTheirCollectionsImmutable() {
        BusinessPartner partner = newOrganization();
        BusinessPartnerContact contact = new BusinessPartnerContact(
                detail("00000000-0000-0000-0000-000000000501"),
                new BusinessPartnerName("Ana Pérez"),
                Optional.of(new BusinessPartnerName("Compras")),
                List.of(channel(
                        "00000000-0000-0000-0000-000000000502",
                        "ana@example.test",
                        true)),
                true);

        partner.addContact(0, contact);

        assertEquals("Ana Pérez", partner.contacts().getFirst().name().value());
        assertThrows(
                UnsupportedOperationException.class,
                () -> partner.contacts().getFirst().channels().clear());
    }

    @Test
    void rejectsTwoPrimaryChannelsOfTheSameCategoryInsideOneContact() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BusinessPartnerContact(
                        detail("00000000-0000-0000-0000-000000000601"),
                        new BusinessPartnerName("Contact"),
                        Optional.empty(),
                        List.of(
                                channel(
                                        "00000000-0000-0000-0000-000000000602",
                                        "one@example.test",
                                        true),
                                channel(
                                        "00000000-0000-0000-0000-000000000603",
                                        "two@example.test",
                                        true)),
                        true));
    }

    private static BusinessPartnerIdentification identification(String id, String value) {
        return BusinessPartnerIdentification.create(
                detail(id),
                TAX_ID,
                Optional.of("py"),
                value,
                Optional.empty(),
                Optional.of(LocalDate.of(2030, 12, 31)));
    }

    private static BusinessPartnerAddress address(
            String id,
            BusinessPartnerAttributeCode type,
            BusinessPartnerAttributeCode purpose,
            boolean primary) {
        return new BusinessPartnerAddress(
                detail(id),
                type,
                purpose,
                "Av. Principal",
                Optional.empty(),
                Optional.of("123"),
                Optional.empty(),
                Optional.of("py"),
                Optional.of("Central"),
                Optional.of("Asunción"),
                true,
                primary);
    }

    private static BusinessPartnerContactChannel channel(
            String id, String value, boolean primary) {
        return new BusinessPartnerContactChannel(
                detail(id), EMAIL, GENERAL, value, true, primary);
    }

    private static BusinessPartner newOrganization() {
        return BusinessPartner.create(
                new CompanyId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                new BusinessPartnerId(UUID.fromString("00000000-0000-0000-0000-000000000101")),
                new BusinessPartnerCode("bp-001"),
                BusinessPartnerKind.ORGANIZATION,
                new BusinessPartnerName("Acme"),
                Optional.of(new BusinessPartnerName("Acme Sociedad Anónima")),
                Optional.empty());
    }

    private static BusinessPartnerDetailId detail(String value) {
        return new BusinessPartnerDetailId(UUID.fromString(value));
    }

    private static BusinessPartnerAttributeCode code(String value) {
        return new BusinessPartnerAttributeCode(value);
    }
}
