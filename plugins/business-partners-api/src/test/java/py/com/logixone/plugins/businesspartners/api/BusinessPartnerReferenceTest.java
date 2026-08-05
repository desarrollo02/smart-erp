package py.com.logixone.plugins.businesspartners.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BusinessPartnerReferenceTest {

    @Test
    void exposesAnImmutableMinimalProjectionAndContractVersion() {
        Set<BusinessPartnerRole> sourceRoles = new HashSet<>();
        sourceRoles.add(BusinessPartnerRole.CLIENT);
        BusinessPartnerReference reference = new BusinessPartnerReference(
                new BusinessPartnerId(UUID.randomUUID()),
                "BP-001",
                "Acme",
                BusinessPartnerKind.ORGANIZATION,
                BusinessPartnerState.ACTIVE,
                sourceRoles,
                3);

        sourceRoles.add(BusinessPartnerRole.SUPPLIER);

        assertEquals(Set.of(BusinessPartnerRole.CLIENT), reference.roles());
        assertThrows(
                UnsupportedOperationException.class,
                () -> reference.roles().add(BusinessPartnerRole.SUPPLIER));
        assertEquals("1.0.0", BusinessPartnerContractVersion.CURRENT);
    }

    @Test
    void acceptsAReferenceWithoutCommercialRoles() {
        BusinessPartnerReference reference = new BusinessPartnerReference(
                new BusinessPartnerId(UUID.randomUUID()),
                "BP-002",
                "Future partner",
                BusinessPartnerKind.ORGANIZATION,
                BusinessPartnerState.ACTIVE,
                Set.of(),
                0);

        assertTrue(reference.roles().isEmpty());
    }

    @Test
    void rejectsIncompleteOrInvalidReferences() {
        BusinessPartnerId id = new BusinessPartnerId(UUID.randomUUID());

        assertThrows(
                IllegalArgumentException.class,
                () -> new BusinessPartnerReference(
                        id,
                        " ",
                        "Name",
                        BusinessPartnerKind.NATURAL_PERSON,
                        BusinessPartnerState.ACTIVE,
                        Set.of(),
                        0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new BusinessPartnerReference(
                        id,
                        "BP-003",
                        "Name",
                        BusinessPartnerKind.NATURAL_PERSON,
                        BusinessPartnerState.ACTIVE,
                        Set.of(),
                        -1));
        assertThrows(
                NullPointerException.class,
                () -> new BusinessPartnerReference(
                        id,
                        "BP-003",
                        "Name",
                        BusinessPartnerKind.NATURAL_PERSON,
                        BusinessPartnerState.ACTIVE,
                        null,
                        0));
    }
}
