package py.com.logixone.plugins.purchasing.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.inventory.api.StockCondition;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptId;
import py.com.logixone.plugins.purchasing.api.GoodsReceiptLineId;
import py.com.logixone.plugins.purchasing.api.PurchaseLineKind;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderId;
import py.com.logixone.plugins.purchasing.api.PurchaseOrderLineId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestId;
import py.com.logixone.plugins.purchasing.api.PurchaseRequestLineId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnId;
import py.com.logixone.plugins.purchasing.api.SupplierReturnLineId;
import py.com.logixone.plugins.referencedata.api.CurrencyCode;

public final class PurchasingCommands {
    private PurchasingCommands() {
    }

    public record ItemInput(
            PurchaseLineKind kind,
            Optional<CatalogItemId> catalogItemId,
            String description,
            String presentedUnitCode) {
        public ItemInput {
            Objects.requireNonNull(kind, "kind");
            catalogItemId = Objects.requireNonNull(catalogItemId, "catalogItemId");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(presentedUnitCode, "presentedUnitCode");
            if (kind == PurchaseLineKind.STOCK && catalogItemId.isEmpty()) {
                throw new IllegalArgumentException("STOCK lines require catalogItemId");
            }
        }
    }

    public record ExpectedPriceInput(BigDecimal amount, CurrencyCode currencyCode) {
        public ExpectedPriceInput {
            Objects.requireNonNull(amount, "amount");
            Objects.requireNonNull(currencyCode, "currencyCode");
        }
    }

    public record RequestLineInput(
            PurchaseRequestLineId id,
            ItemInput item,
            BigDecimal quantity,
            Optional<ExpectedPriceInput> expectedPrice) {
        public RequestLineInput {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(item, "item");
            Objects.requireNonNull(quantity, "quantity");
            expectedPrice = Objects.requireNonNull(expectedPrice, "expectedPrice");
        }
    }

    public record CreateRequest(
            String idempotencyKey,
            String number,
            LocalDate requestedOn,
            List<RequestLineInput> lines) {
        public CreateRequest {
            idempotencyKey = PurchasingCommandValues.key(idempotencyKey, "idempotencyKey");
            Objects.requireNonNull(number, "number");
            Objects.requireNonNull(requestedOn, "requestedOn");
            lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        }
    }

    public record ReplaceRequestLines(
            String idempotencyKey,
            PurchaseRequestId requestId,
            long expectedVersion,
            List<RequestLineInput> lines) {
        public ReplaceRequestLines {
            idempotencyKey = PurchasingCommandValues.key(idempotencyKey, "idempotencyKey");
            Objects.requireNonNull(requestId, "requestId");
            lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        }
    }

    public record CloneRequest(
            String idempotencyKey,
            PurchaseRequestId sourceRequestId,
            String number,
            LocalDate requestedOn,
            List<PurchaseRequestLineId> newLineIds) {
        public CloneRequest {
            idempotencyKey = PurchasingCommandValues.key(idempotencyKey, "idempotencyKey");
            Objects.requireNonNull(sourceRequestId, "sourceRequestId");
            Objects.requireNonNull(number, "number");
            Objects.requireNonNull(requestedOn, "requestedOn");
            newLineIds = List.copyOf(Objects.requireNonNull(newLineIds, "newLineIds"));
        }
    }

    public record RequestTransition(
            String idempotencyKey,
            PurchaseRequestId requestId,
            long expectedVersion,
            Optional<String> reason) {
        public RequestTransition {
            idempotencyKey = PurchasingCommandValues.key(idempotencyKey, "idempotencyKey");
            Objects.requireNonNull(requestId, "requestId");
            reason = Objects.requireNonNull(reason, "reason");
        }
    }

    public record AllocationInput(
            PurchaseRequestId requestId,
            PurchaseRequestLineId requestLineId,
            BigDecimal quantity) {
        public AllocationInput {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(requestLineId, "requestLineId");
            Objects.requireNonNull(quantity, "quantity");
        }
    }

    public record OrderLineInput(
            PurchaseOrderLineId id,
            ItemInput item,
            BigDecimal orderedQuantity,
            BigDecimal unitPrice,
            List<AllocationInput> allocations) {
        public OrderLineInput {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(item, "item");
            Objects.requireNonNull(orderedQuantity, "orderedQuantity");
            Objects.requireNonNull(unitPrice, "unitPrice");
            allocations = List.copyOf(Objects.requireNonNull(allocations, "allocations"));
        }
    }

    public record CreateOrder(
            String idempotencyKey,
            String number,
            BusinessPartnerId supplierId,
            CurrencyCode currencyCode,
            List<OrderLineInput> lines,
            Optional<String> directOrderJustification) {
        public CreateOrder {
            idempotencyKey = PurchasingCommandValues.key(idempotencyKey, "idempotencyKey");
            Objects.requireNonNull(number, "number");
            Objects.requireNonNull(supplierId, "supplierId");
            Objects.requireNonNull(currencyCode, "currencyCode");
            lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
            directOrderJustification = Objects.requireNonNull(
                    directOrderJustification, "directOrderJustification");
        }
    }

    public record AddOrderLine(
            String idempotencyKey,
            PurchaseOrderId orderId,
            long expectedVersion,
            OrderLineInput line) {
        public AddOrderLine {
            idempotencyKey = PurchasingCommandValues.key(idempotencyKey, "idempotencyKey");
            Objects.requireNonNull(orderId, "orderId");
            if (expectedVersion < 0) {
                throw new IllegalArgumentException("expectedVersion must not be negative");
            }
            Objects.requireNonNull(line, "line");
        }
    }

    public record OrderTransition(
            String idempotencyKey,
            PurchaseOrderId orderId,
            long expectedVersion,
            Optional<String> reason) {
        public OrderTransition {
            idempotencyKey = PurchasingCommandValues.key(idempotencyKey, "idempotencyKey");
            Objects.requireNonNull(orderId, "orderId");
            reason = Objects.requireNonNull(reason, "reason");
        }
    }

    public record ReceiptLineInput(
            GoodsReceiptLineId id,
            PurchaseOrderLineId orderLineId,
            BigDecimal quantity,
            Optional<WarehouseId> warehouseId,
            Optional<StockLocationId> locationId,
            Optional<String> lotCode,
            Optional<String> serialNumber,
            Optional<LocalDate> expiryDate,
            Optional<StockCondition> condition) {
        public ReceiptLineInput {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(orderLineId, "orderLineId");
            Objects.requireNonNull(quantity, "quantity");
            warehouseId = Objects.requireNonNull(warehouseId, "warehouseId");
            locationId = Objects.requireNonNull(locationId, "locationId");
            lotCode = Objects.requireNonNull(lotCode, "lotCode");
            serialNumber = Objects.requireNonNull(serialNumber, "serialNumber");
            expiryDate = Objects.requireNonNull(expiryDate, "expiryDate");
            condition = Objects.requireNonNull(condition, "condition");
        }
    }

    public record CreateReceipt(
            String idempotencyKey,
            String number,
            PurchaseOrderId orderId,
            List<ReceiptLineInput> lines) {
        public CreateReceipt {
            idempotencyKey = PurchasingCommandValues.key(idempotencyKey, "idempotencyKey");
            Objects.requireNonNull(number, "number");
            Objects.requireNonNull(orderId, "orderId");
            lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        }
    }

    public record ConfirmReceipt(
            String idempotencyKey,
            GoodsReceiptId receiptId,
            long expectedReceiptVersion,
            long expectedOrderVersion) {
        public ConfirmReceipt {
            idempotencyKey = PurchasingCommandValues.key(idempotencyKey, "idempotencyKey");
            Objects.requireNonNull(receiptId, "receiptId");
        }
    }

    public record ReturnLineInput(
            SupplierReturnLineId id,
            GoodsReceiptId receiptId,
            GoodsReceiptLineId receiptLineId,
            PurchaseOrderLineId orderLineId,
            BigDecimal quantity) {
        public ReturnLineInput {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(receiptId, "receiptId");
            Objects.requireNonNull(receiptLineId, "receiptLineId");
            Objects.requireNonNull(orderLineId, "orderLineId");
            Objects.requireNonNull(quantity, "quantity");
        }
    }

    public record CreateSupplierReturn(
            String idempotencyKey,
            String number,
            PurchaseOrderId orderId,
            String reason,
            List<ReturnLineInput> lines) {
        public CreateSupplierReturn {
            idempotencyKey = PurchasingCommandValues.key(idempotencyKey, "idempotencyKey");
            Objects.requireNonNull(number, "number");
            Objects.requireNonNull(orderId, "orderId");
            Objects.requireNonNull(reason, "reason");
            lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        }
    }

    public record ConfirmSupplierReturn(
            String idempotencyKey,
            SupplierReturnId supplierReturnId,
            long expectedReturnVersion,
            long expectedOrderVersion) {
        public ConfirmSupplierReturn {
            idempotencyKey = PurchasingCommandValues.key(idempotencyKey, "idempotencyKey");
            Objects.requireNonNull(supplierReturnId, "supplierReturnId");
        }
    }
}
