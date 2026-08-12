package py.com.logixone.plugins.purchasing.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PurchasingApiContractTest {
    private static final String SUPPLIER_ID = "00000000-0000-0000-0000-000000000011";
    private static final String ITEM_ID = "00000000-0000-0000-0000-000000000021";

    @Test
    void identifiersRequireCanonicalUuidAndRemainTyped() {
        String canonical = "00000000-0000-0000-0000-000000000001";

        assertEquals(canonical, PurchaseRequestId.parse(canonical).toString());
        assertEquals(canonical, PurchaseOrderId.parse(canonical).toString());
        assertThrows(IllegalArgumentException.class,
                () -> PurchaseRequestId.parse("AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA"));
    }

    @Test
    void openRequestImportRequiresUniqueSourceLinesAndRejectsTerminalState() {
        PurchasingSourceReference source = source("REQ-7");
        PurchaseImportLine line = line("1", PurchaseLineKind.STOCK, Optional.of(ITEM_ID));

        OpenPurchaseRequestImport request = new OpenPurchaseRequestImport(
                source, "SC-7", LocalDate.of(2026, 8, 11), PurchaseRequestState.APPROVED,
                Optional.of("PYG"), List.of(line));

        assertEquals(PurchaseRequestState.APPROVED, request.state());
        assertThrows(IllegalArgumentException.class, () -> new OpenPurchaseRequestImport(
                source, "SC-7", LocalDate.of(2026, 8, 11), PurchaseRequestState.REJECTED,
                Optional.of("PYG"), List.of(line)));
        assertThrows(IllegalArgumentException.class, () -> new OpenPurchaseRequestImport(
                source, "SC-7", LocalDate.of(2026, 8, 11), PurchaseRequestState.DRAFT,
                Optional.of("PYG"), List.of(line, line)));
        assertThrows(IllegalArgumentException.class, () -> new OpenPurchaseRequestImport(
                source, "SC-8", LocalDate.of(2026, 8, 11), PurchaseRequestState.DRAFT,
                Optional.empty(), List.of(line)));
    }

    @Test
    void openOrderImportRequiresIssueDateAndTypedSupplierCurrencyData() {
        PurchaseImportLine service = line("1", PurchaseLineKind.SERVICE, Optional.empty());

        OpenPurchaseOrderImport order = new OpenPurchaseOrderImport(
                source("OC-9"), "OC-9", SUPPLIER_ID, "PROV-1", "Proveedor Uno",
                "PYG", 0, PurchaseOrderState.ISSUED,
                Optional.of(LocalDate.of(2026, 8, 11)), List.of(service));

        assertEquals("PYG", order.currencyCode());
        assertThrows(IllegalArgumentException.class, () -> new OpenPurchaseOrderImport(
                source("OC-9"), "OC-9", SUPPLIER_ID, "PROV-1", "Proveedor Uno",
                "PYG", 0, PurchaseOrderState.ISSUED, Optional.empty(), List.of(service)));
        PurchaseImportLine withoutPrice = new PurchaseImportLine(
                "2", PurchaseLineKind.SERVICE, Optional.empty(), "Servicio", "UN",
                BigDecimal.ONE, Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> new OpenPurchaseOrderImport(
                source("OC-10"), "OC-10", SUPPLIER_ID, "PROV-1", "Proveedor Uno",
                "PYG", 0, PurchaseOrderState.DRAFT, Optional.empty(), List.of(withoutPrice)));
    }

    @Test
    void stockImportLineRequiresCatalogIdentity() {
        assertThrows(IllegalArgumentException.class,
                () -> line("1", PurchaseLineKind.STOCK, Optional.empty()));
    }

    private static PurchasingSourceReference source(String key) {
        return new PurchasingSourceReference(
                "ORACLE_FORMS", key, Optional.of("a".repeat(64)));
    }

    private static PurchaseImportLine line(
            String key, PurchaseLineKind kind, Optional<String> catalogItemId) {
        return new PurchaseImportLine(
                key, kind, catalogItemId, "Descripción", "UN",
                new BigDecimal("2.5"), Optional.of(new BigDecimal("10.25")));
    }
}
