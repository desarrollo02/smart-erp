package py.com.logixone.plugins.inventory.domain;

import java.math.BigDecimal;
import java.util.Objects;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.inventory.api.StockAvailability;
import py.com.logixone.plugins.inventory.api.StockKey;

/** Mutable projection guarded by non-negative physical and available invariants. */
public final class InventoryBalance {
    private final CompanyId companyId;
    private final StockKey key;
    private final String baseUnitCode;
    private BigDecimal physicalQuantity = BigDecimal.ZERO;
    private BigDecimal reservedQuantity = BigDecimal.ZERO;
    private long version;

    private InventoryBalance(CompanyId companyId, StockKey key, String baseUnitCode) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.key = Objects.requireNonNull(key, "key");
        this.baseUnitCode = InventoryValues.code(baseUnitCode, "baseUnitCode", 16);
    }

    public static InventoryBalance empty(CompanyId companyId, StockKey key, String baseUnitCode) {
        return new InventoryBalance(companyId, key, baseUnitCode);
    }

    public static InventoryBalance restore(InventoryBalanceSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        InventoryBalance balance = new InventoryBalance(
                snapshot.companyId(), snapshot.key(), snapshot.baseUnitCode());
        balance.physicalQuantity = snapshot.physicalQuantity();
        balance.reservedQuantity = snapshot.reservedQuantity();
        balance.version = snapshot.version();
        return balance;
    }

    public void receive(BigDecimal quantity, long expectedVersion) {
        verifyVersion(expectedVersion);
        physicalQuantity = physicalQuantity.add(InventoryValues.quantity(quantity, "quantity", true));
        version++;
    }

    public void issue(BigDecimal quantity, long expectedVersion) {
        verifyVersion(expectedVersion);
        BigDecimal nextPhysical = physicalQuantity.subtract(
                InventoryValues.quantity(quantity, "quantity", true));
        assertNonNegative(nextPhysical, reservedQuantity);
        physicalQuantity = nextPhysical;
        version++;
    }

    /** Applies one aggregated physical delta so one command advances the persisted version once. */
    public void adjustPhysical(BigDecimal delta, long expectedVersion) {
        verifyVersion(expectedVersion);
        BigDecimal normalized = InventoryValues.signedQuantity(delta, "delta");
        BigDecimal nextPhysical = physicalQuantity.add(normalized);
        assertNonNegative(nextPhysical, reservedQuantity);
        physicalQuantity = nextPhysical;
        version++;
    }

    public void reserve(BigDecimal quantity, long expectedVersion) {
        verifyVersion(expectedVersion);
        BigDecimal nextReserved = reservedQuantity.add(
                InventoryValues.quantity(quantity, "quantity", true));
        assertNonNegative(physicalQuantity, nextReserved);
        reservedQuantity = nextReserved;
        version++;
    }

    public void consumeReserved(BigDecimal quantity, long expectedVersion) {
        verifyVersion(expectedVersion);
        BigDecimal normalized = InventoryValues.quantity(quantity, "quantity", true);
        BigDecimal nextPhysical = physicalQuantity.subtract(normalized);
        BigDecimal nextReserved = reservedQuantity.subtract(normalized);
        assertNonNegative(nextPhysical, nextReserved);
        physicalQuantity = nextPhysical;
        reservedQuantity = nextReserved;
        version++;
    }

    public void release(BigDecimal quantity, long expectedVersion) {
        verifyVersion(expectedVersion);
        BigDecimal nextReserved = reservedQuantity.subtract(
                InventoryValues.quantity(quantity, "quantity", true));
        assertNonNegative(physicalQuantity, nextReserved);
        reservedQuantity = nextReserved;
        version++;
    }

    public StockAvailability availability() {
        return new StockAvailability(
                key, baseUnitCode, physicalQuantity, reservedQuantity,
                physicalQuantity.subtract(reservedQuantity), version);
    }

    private void verifyVersion(long expectedVersion) {
        if (expectedVersion != version) {
            throw new ConcurrentInventoryChangeException(expectedVersion, version);
        }
    }

    private static void assertNonNegative(BigDecimal physical, BigDecimal reserved) {
        if (physical.signum() < 0 || reserved.signum() < 0 || physical.subtract(reserved).signum() < 0) {
            throw new IllegalStateException("Physical, reserved and available quantities must not be negative");
        }
    }

    public CompanyId companyId() { return companyId; }
    public StockKey key() { return key; }
    public String baseUnitCode() { return baseUnitCode; }
    public BigDecimal physicalQuantity() { return physicalQuantity; }
    public BigDecimal reservedQuantity() { return reservedQuantity; }
    public BigDecimal availableQuantity() { return physicalQuantity.subtract(reservedQuantity); }
    public long version() { return version; }
    public InventoryBalanceSnapshot snapshot() {
        return new InventoryBalanceSnapshot(
                companyId, key, baseUnitCode, physicalQuantity, reservedQuantity, version);
    }
}
