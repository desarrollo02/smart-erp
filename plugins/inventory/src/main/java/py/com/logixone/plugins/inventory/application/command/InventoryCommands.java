package py.com.logixone.plugins.inventory.application.command;

import java.math.BigDecimal;
import java.util.Objects;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.inventory.api.ExpiryPolicy;
import py.com.logixone.plugins.inventory.api.InventoryItemId;
import py.com.logixone.plugins.inventory.api.StockCountId;
import py.com.logixone.plugins.inventory.api.StockKey;
import py.com.logixone.plugins.inventory.api.StockLocationId;
import py.com.logixone.plugins.inventory.api.StockReservationId;
import py.com.logixone.plugins.inventory.api.TrackingMode;
import py.com.logixone.plugins.inventory.api.WarehouseId;
import py.com.logixone.plugins.inventory.domain.StockCountScope;
import py.com.logixone.plugins.inventory.domain.StockLocationType;

/** Validated application payloads; domain values perform detailed normalization. */
public final class InventoryCommands {
    private InventoryCommands() {
    }

    public record OpenWarehouse(String code, String name) {
        public OpenWarehouse {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(name, "name");
        }
    }

    public record AddLocation(
            WarehouseId warehouseId,
            long expectedVersion,
            String code,
            String name,
            StockLocationType type) {
        public AddLocation {
            mutation(warehouseId, expectedVersion);
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(type, "type");
        }
    }

    public record RenameWarehouse(WarehouseId warehouseId, long expectedVersion, String name) {
        public RenameWarehouse {
            mutation(warehouseId, expectedVersion);
            Objects.requireNonNull(name, "name");
        }
    }

    public record RenameLocation(
            WarehouseId warehouseId,
            StockLocationId locationId,
            long expectedVersion,
            long expectedLocationVersion,
            String name) {
        public RenameLocation {
            mutation(warehouseId, expectedVersion);
            Objects.requireNonNull(locationId, "locationId");
            version(expectedLocationVersion);
            Objects.requireNonNull(name, "name");
        }
    }

    public record InactivateWarehouse(WarehouseId warehouseId, long expectedVersion) {
        public InactivateWarehouse { mutation(warehouseId, expectedVersion); }
    }

    public record InactivateLocation(
            WarehouseId warehouseId,
            StockLocationId locationId,
            long expectedVersion,
            long expectedLocationVersion) {
        public InactivateLocation {
            mutation(warehouseId, expectedVersion);
            Objects.requireNonNull(locationId, "locationId");
            version(expectedLocationVersion);
        }
    }

    public record EnrollItem(
            CatalogItemId catalogItemId, TrackingMode trackingMode, ExpiryPolicy expiryPolicy) {
        public EnrollItem {
            Objects.requireNonNull(catalogItemId, "catalogItemId");
            Objects.requireNonNull(trackingMode, "trackingMode");
            Objects.requireNonNull(expiryPolicy, "expiryPolicy");
        }
    }

    public record InactivateItem(InventoryItemId itemId, long expectedVersion) {
        public InactivateItem {
            Objects.requireNonNull(itemId, "itemId");
            version(expectedVersion);
        }
    }

    public record RefreshItem(InventoryItemId itemId, long expectedVersion) {
        public RefreshItem {
            Objects.requireNonNull(itemId, "itemId");
            version(expectedVersion);
        }
    }

    public record ConsumeReservation(
            StockReservationId reservationId,
            long expectedVersion,
            BigDecimal quantity,
            String idempotencyKey) {
        public ConsumeReservation {
            reservationMutation(reservationId, expectedVersion, quantity, idempotencyKey);
        }
    }

    public record ReleaseReservation(
            StockReservationId reservationId,
            long expectedVersion,
            BigDecimal quantity,
            String idempotencyKey) {
        public ReleaseReservation {
            reservationMutation(reservationId, expectedVersion, quantity, idempotencyKey);
        }
    }

    public record ExpireReservation(
            StockReservationId reservationId,
            long expectedVersion,
            String idempotencyKey) {
        public ExpireReservation {
            Objects.requireNonNull(reservationId, "reservationId");
            version(expectedVersion);
            Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        }
    }

    public record DraftCount(StockCountScope scope) {
        public DraftCount { Objects.requireNonNull(scope, "scope"); }
    }

    public record AddCountLine(StockCountId countId, long expectedVersion, StockKey key) {
        public AddCountLine {
            countMutation(countId, expectedVersion);
            Objects.requireNonNull(key, "key");
        }
    }

    public record CountTransition(StockCountId countId, long expectedVersion) {
        public CountTransition { countMutation(countId, expectedVersion); }
    }

    public record RecordCount(
            StockCountId countId,
            long expectedVersion,
            StockKey key,
            BigDecimal countedQuantity) {
        public RecordCount {
            countMutation(countId, expectedVersion);
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(countedQuantity, "countedQuantity");
        }
    }

    private static void reservationMutation(
            StockReservationId id, long expectedVersion, BigDecimal quantity, String key) {
        Objects.requireNonNull(id, "reservationId");
        version(expectedVersion);
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(key, "idempotencyKey");
    }

    private static void countMutation(StockCountId id, long expectedVersion) {
        Objects.requireNonNull(id, "countId");
        version(expectedVersion);
    }

    private static void mutation(WarehouseId id, long expectedVersion) {
        Objects.requireNonNull(id, "warehouseId");
        version(expectedVersion);
    }

    private static void version(long expectedVersion) {
        if (expectedVersion < 0) {
            throw new IllegalArgumentException("expectedVersion must not be negative");
        }
    }
}
