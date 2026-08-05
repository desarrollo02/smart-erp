package py.com.logixone.plugins.businesspartners.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerKind;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerRole;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;

class BusinessPartnerTest {

    @Test
    void restoresItsSnapshotWithoutExecutingCommandsOrChangingTheVersion() {
        BusinessPartner original = newOrganization();
        original.assignRole(0, BusinessPartnerRole.CLIENT, Optional.of(new BusinessPartnerCode("CLI-1")));
        original.addIdentification(1, BusinessPartnerIdentification.create(
                new BusinessPartnerDetailId(UUID.randomUUID()),
                new BusinessPartnerAttributeCode("tax_id"),
                Optional.of("PY"),
                "80001234-5",
                Optional.of("5"),
                Optional.empty()));

        BusinessPartner restored = BusinessPartner.restore(original.snapshot());

        assertEquals(original.snapshot(), restored.snapshot());
        assertEquals(2, restored.version());
    }

    @Test
    void allowsNoRoleAndThenIndependentClientAndSupplierRoles() {
        BusinessPartner partner = newOrganization();

        assertTrue(partner.roles().isEmpty());
        assertTrue(partner.toReference().roles().isEmpty());

        partner.assignRole(0, BusinessPartnerRole.CLIENT, Optional.empty());
        partner.assignRole(
                1,
                BusinessPartnerRole.SUPPLIER,
                Optional.of(new BusinessPartnerCode("sup-9")));
        partner.changeRoleState(2, BusinessPartnerRole.CLIENT, BusinessPartnerState.INACTIVE);

        assertEquals(3, partner.version());
        assertEquals(2, partner.roles().size());
        assertEquals(BusinessPartnerState.INACTIVE,
                partner.roles().get(BusinessPartnerRole.CLIENT).state());
        assertEquals(BusinessPartnerState.ACTIVE,
                partner.roles().get(BusinessPartnerRole.SUPPLIER).state());
        assertEquals(Set.of(BusinessPartnerRole.SUPPLIER), partner.toReference().roles());
    }

    @Test
    void inactivationPreservesRolesAndBlocksNewOperationalChanges() {
        BusinessPartner partner = newOrganization();
        partner.assignRole(0, BusinessPartnerRole.CLIENT, Optional.empty());
        partner.inactivate(1);

        assertEquals(BusinessPartnerState.INACTIVE, partner.state());
        assertTrue(partner.roles().containsKey(BusinessPartnerRole.CLIENT));
        assertThrows(
                IllegalStateException.class,
                () -> partner.assignRole(2, BusinessPartnerRole.SUPPLIER, Optional.empty()));
        assertThrows(
                IllegalStateException.class,
                () -> partner.changeCode(2, new BusinessPartnerCode("BP-NEW")));

        partner.reactivate(2);
        partner.assignRole(3, BusinessPartnerRole.SUPPLIER, Optional.empty());

        assertEquals(BusinessPartnerState.ACTIVE, partner.state());
        assertEquals(4, partner.version());
    }

    @Test
    void rejectsStaleVersionsBeforeOverwritingData() {
        BusinessPartner partner = newOrganization();
        partner.rename(
                0,
                new BusinessPartnerName("New display name"),
                Optional.of(new BusinessPartnerName("New legal name")),
                Optional.empty());

        ConcurrentBusinessPartnerChangeException failure = assertThrows(
                ConcurrentBusinessPartnerChangeException.class,
                () -> partner.changeCode(0, new BusinessPartnerCode("BP-NEW")));

        assertEquals(0, failure.expectedVersion());
        assertEquals(1, failure.actualVersion());
        assertEquals("BP-001", partner.code().value());
    }

    @Test
    void exposesAnImmutableRoleViewAndMinimalPublicReference() {
        BusinessPartner partner = newOrganization();
        partner.assignRole(0, BusinessPartnerRole.CLIENT, Optional.empty());

        assertThrows(
                UnsupportedOperationException.class,
                () -> partner.roles().clear());
        assertEquals("BP-001", partner.toReference().code());
        assertEquals("Acme", partner.toReference().displayName());
        assertFalse(partner.toReference().roles().isEmpty());
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
}
