CREATE TABLE warehouse (
    company_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    warehouse_code VARCHAR(64) NOT NULL,
    warehouse_name VARCHAR(160) NOT NULL,
    active BOOLEAN NOT NULL,
    entity_version BIGINT NOT NULL,
    PRIMARY KEY (company_id, warehouse_id),
    CONSTRAINT uq_warehouse_code UNIQUE (company_id, warehouse_code),
    CONSTRAINT ck_warehouse_version CHECK (entity_version >= 0)
);

CREATE TABLE stock_location (
    company_id UUID NOT NULL,
    stock_location_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    location_code VARCHAR(64) NOT NULL,
    location_name VARCHAR(160) NOT NULL,
    location_type VARCHAR(24) NOT NULL,
    active BOOLEAN NOT NULL,
    entity_version BIGINT NOT NULL,
    PRIMARY KEY (company_id, stock_location_id),
    CONSTRAINT fk_stock_location_warehouse
        FOREIGN KEY (company_id, warehouse_id)
        REFERENCES warehouse (company_id, warehouse_id),
    CONSTRAINT uq_stock_location_owner
        UNIQUE (company_id, warehouse_id, stock_location_id),
    CONSTRAINT uq_stock_location_code
        UNIQUE (company_id, warehouse_id, location_code),
    CONSTRAINT ck_stock_location_type
        CHECK (location_type IN ('GENERAL', 'STORAGE', 'RECEIVING', 'DISPATCH')),
    CONSTRAINT ck_stock_location_general_identity
        CHECK ((location_type = 'GENERAL' AND location_code = 'GENERAL')
            OR (location_type <> 'GENERAL' AND location_code <> 'GENERAL')),
    CONSTRAINT ck_stock_location_version CHECK (entity_version >= 0)
);

CREATE UNIQUE INDEX uq_stock_location_single_general
    ON stock_location (company_id, warehouse_id)
    WHERE location_type = 'GENERAL';

CREATE TABLE inventory_item (
    company_id UUID NOT NULL,
    inventory_item_id UUID NOT NULL,
    catalog_item_id UUID NOT NULL,
    catalog_code_snapshot VARCHAR(64) NOT NULL,
    catalog_name_snapshot VARCHAR(240) NOT NULL,
    base_unit_code_snapshot VARCHAR(16) NOT NULL,
    catalog_item_version BIGINT NOT NULL,
    tracking_mode VARCHAR(16) NOT NULL,
    expiry_policy VARCHAR(16) NOT NULL,
    active BOOLEAN NOT NULL,
    entity_version BIGINT NOT NULL,
    PRIMARY KEY (company_id, inventory_item_id),
    CONSTRAINT uq_inventory_item_catalog UNIQUE (company_id, catalog_item_id),
    CONSTRAINT ck_inventory_item_tracking CHECK (tracking_mode IN ('NONE', 'LOT', 'SERIAL')),
    CONSTRAINT ck_inventory_item_expiry CHECK (expiry_policy IN ('NONE', 'OPTIONAL', 'REQUIRED')),
    CONSTRAINT ck_inventory_item_catalog_version CHECK (catalog_item_version >= 0),
    CONSTRAINT ck_inventory_item_version CHECK (entity_version >= 0)
);

CREATE TABLE inventory_balance (
    company_id UUID NOT NULL,
    inventory_balance_id UUID NOT NULL,
    inventory_item_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    stock_location_id UUID NOT NULL,
    lot_code VARCHAR(80),
    serial_number VARCHAR(120),
    expiry_date DATE,
    condition_code VARCHAR(24) NOT NULL,
    base_unit_code VARCHAR(16) NOT NULL,
    physical_quantity NUMERIC(30, 6) NOT NULL,
    reserved_quantity NUMERIC(30, 6) NOT NULL,
    entity_version BIGINT NOT NULL,
    PRIMARY KEY (company_id, inventory_balance_id),
    CONSTRAINT fk_inventory_balance_item
        FOREIGN KEY (company_id, inventory_item_id)
        REFERENCES inventory_item (company_id, inventory_item_id),
    CONSTRAINT fk_inventory_balance_location
        FOREIGN KEY (company_id, warehouse_id, stock_location_id)
        REFERENCES stock_location (company_id, warehouse_id, stock_location_id),
    CONSTRAINT uq_inventory_balance_key
        UNIQUE NULLS NOT DISTINCT (
            company_id, inventory_item_id, warehouse_id, stock_location_id,
            lot_code, serial_number, expiry_date, condition_code),
    CONSTRAINT ck_inventory_balance_tracking_key
        CHECK (lot_code IS NULL OR serial_number IS NULL),
    CONSTRAINT ck_inventory_balance_condition
        CHECK (condition_code IN ('AVAILABLE', 'QUARANTINED', 'DAMAGED')),
    CONSTRAINT ck_inventory_balance_quantities
        CHECK (physical_quantity >= 0 AND reserved_quantity >= 0
            AND reserved_quantity <= physical_quantity),
    CONSTRAINT ck_inventory_balance_version CHECK (entity_version >= 0)
);

CREATE UNIQUE INDEX uq_inventory_balance_positive_serial
    ON inventory_balance (company_id, inventory_item_id, serial_number)
    WHERE serial_number IS NOT NULL AND physical_quantity > 0;

CREATE TABLE stock_movement (
    company_id UUID NOT NULL,
    stock_movement_id UUID NOT NULL,
    movement_type VARCHAR(24) NOT NULL,
    reason_code VARCHAR(64) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(160) NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    posted_at TIMESTAMPTZ NOT NULL,
    reversal_of_movement_id UUID,
    PRIMARY KEY (company_id, stock_movement_id),
    CONSTRAINT fk_stock_movement_reversal
        FOREIGN KEY (company_id, reversal_of_movement_id)
        REFERENCES stock_movement (company_id, stock_movement_id),
    CONSTRAINT uq_stock_movement_idempotency
        UNIQUE (company_id, source_type, idempotency_key),
    CONSTRAINT ck_stock_movement_type
        CHECK (movement_type IN ('RECEIPT', 'ISSUE', 'TRANSFER', 'ADJUSTMENT', 'REVERSAL')),
    CONSTRAINT ck_stock_movement_reversal_shape
        CHECK ((movement_type = 'REVERSAL' AND reversal_of_movement_id IS NOT NULL)
            OR (movement_type <> 'REVERSAL' AND reversal_of_movement_id IS NULL))
);

CREATE UNIQUE INDEX uq_stock_movement_single_reversal
    ON stock_movement (company_id, reversal_of_movement_id)
    WHERE reversal_of_movement_id IS NOT NULL;

CREATE TABLE stock_movement_line (
    company_id UUID NOT NULL,
    stock_movement_id UUID NOT NULL,
    line_number INTEGER NOT NULL,
    inventory_item_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    stock_location_id UUID NOT NULL,
    lot_code VARCHAR(80),
    serial_number VARCHAR(120),
    expiry_date DATE,
    condition_code VARCHAR(24) NOT NULL,
    movement_direction VARCHAR(16) NOT NULL,
    catalog_item_id_snapshot UUID NOT NULL,
    catalog_code_snapshot VARCHAR(64) NOT NULL,
    catalog_name_snapshot VARCHAR(240) NOT NULL,
    presented_unit_code VARCHAR(16) NOT NULL,
    presented_quantity NUMERIC(30, 6) NOT NULL,
    base_unit_code VARCHAR(16) NOT NULL,
    conversion_factor NUMERIC(30, 12) NOT NULL,
    base_quantity NUMERIC(30, 6) NOT NULL,
    catalog_item_version BIGINT NOT NULL,
    PRIMARY KEY (company_id, stock_movement_id, line_number),
    CONSTRAINT fk_stock_movement_line_owner
        FOREIGN KEY (company_id, stock_movement_id)
        REFERENCES stock_movement (company_id, stock_movement_id),
    CONSTRAINT fk_stock_movement_line_item
        FOREIGN KEY (company_id, inventory_item_id)
        REFERENCES inventory_item (company_id, inventory_item_id),
    CONSTRAINT fk_stock_movement_line_location
        FOREIGN KEY (company_id, warehouse_id, stock_location_id)
        REFERENCES stock_location (company_id, warehouse_id, stock_location_id),
    CONSTRAINT ck_stock_movement_line_number CHECK (line_number > 0),
    CONSTRAINT ck_stock_movement_line_tracking_key CHECK (lot_code IS NULL OR serial_number IS NULL),
    CONSTRAINT ck_stock_movement_line_condition
        CHECK (condition_code IN ('AVAILABLE', 'QUARANTINED', 'DAMAGED')),
    CONSTRAINT ck_stock_movement_line_direction
        CHECK (movement_direction IN ('INCREASE', 'DECREASE')),
    CONSTRAINT ck_stock_movement_line_quantities
        CHECK (presented_quantity > 0 AND conversion_factor > 0 AND base_quantity > 0),
    CONSTRAINT ck_stock_movement_line_catalog_version CHECK (catalog_item_version >= 0)
);

CREATE TABLE stock_reservation (
    company_id UUID NOT NULL,
    stock_reservation_id UUID NOT NULL,
    inventory_item_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    stock_location_id UUID NOT NULL,
    lot_code VARCHAR(80),
    serial_number VARCHAR(120),
    expiry_date DATE,
    condition_code VARCHAR(24) NOT NULL,
    original_quantity NUMERIC(30, 6) NOT NULL,
    consumed_quantity NUMERIC(30, 6) NOT NULL,
    released_quantity NUMERIC(30, 6) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(160) NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    reservation_state VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    entity_version BIGINT NOT NULL,
    PRIMARY KEY (company_id, stock_reservation_id),
    CONSTRAINT fk_stock_reservation_item
        FOREIGN KEY (company_id, inventory_item_id)
        REFERENCES inventory_item (company_id, inventory_item_id),
    CONSTRAINT fk_stock_reservation_location
        FOREIGN KEY (company_id, warehouse_id, stock_location_id)
        REFERENCES stock_location (company_id, warehouse_id, stock_location_id),
    CONSTRAINT uq_stock_reservation_idempotency
        UNIQUE (company_id, source_type, idempotency_key),
    CONSTRAINT ck_stock_reservation_tracking_key CHECK (lot_code IS NULL OR serial_number IS NULL),
    CONSTRAINT ck_stock_reservation_condition
        CHECK (condition_code IN ('AVAILABLE', 'QUARANTINED', 'DAMAGED')),
    CONSTRAINT ck_stock_reservation_quantities
        CHECK (original_quantity > 0 AND consumed_quantity >= 0 AND released_quantity >= 0
            AND consumed_quantity + released_quantity <= original_quantity),
    CONSTRAINT ck_stock_reservation_state
        CHECK (reservation_state IN ('ACTIVE', 'PARTIALLY_CONSUMED', 'CONSUMED', 'RELEASED', 'EXPIRED')),
    CONSTRAINT ck_stock_reservation_expiry CHECK (expires_at > created_at),
    CONSTRAINT ck_stock_reservation_version CHECK (entity_version >= 0)
);

CREATE TABLE stock_count (
    company_id UUID NOT NULL,
    stock_count_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    stock_location_id UUID,
    count_state VARCHAR(24) NOT NULL,
    entity_version BIGINT NOT NULL,
    PRIMARY KEY (company_id, stock_count_id),
    CONSTRAINT fk_stock_count_warehouse
        FOREIGN KEY (company_id, warehouse_id)
        REFERENCES warehouse (company_id, warehouse_id),
    CONSTRAINT fk_stock_count_location
        FOREIGN KEY (company_id, warehouse_id, stock_location_id)
        REFERENCES stock_location (company_id, warehouse_id, stock_location_id),
    CONSTRAINT ck_stock_count_state
        CHECK (count_state IN ('DRAFT', 'COUNTING', 'REVIEW', 'POSTED', 'CANCELLED')),
    CONSTRAINT ck_stock_count_version CHECK (entity_version >= 0)
);

CREATE TABLE stock_count_line (
    company_id UUID NOT NULL,
    stock_count_id UUID NOT NULL,
    line_number INTEGER NOT NULL,
    inventory_item_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    stock_location_id UUID NOT NULL,
    lot_code VARCHAR(80),
    serial_number VARCHAR(120),
    expiry_date DATE,
    condition_code VARCHAR(24) NOT NULL,
    theoretical_quantity NUMERIC(30, 6) NOT NULL,
    counted_quantity NUMERIC(30, 6),
    PRIMARY KEY (company_id, stock_count_id, line_number),
    CONSTRAINT fk_stock_count_line_owner
        FOREIGN KEY (company_id, stock_count_id)
        REFERENCES stock_count (company_id, stock_count_id),
    CONSTRAINT fk_stock_count_line_item
        FOREIGN KEY (company_id, inventory_item_id)
        REFERENCES inventory_item (company_id, inventory_item_id),
    CONSTRAINT fk_stock_count_line_location
        FOREIGN KEY (company_id, warehouse_id, stock_location_id)
        REFERENCES stock_location (company_id, warehouse_id, stock_location_id),
    CONSTRAINT uq_stock_count_line_key
        UNIQUE NULLS NOT DISTINCT (
            company_id, stock_count_id, inventory_item_id, warehouse_id,
            stock_location_id, lot_code, serial_number, expiry_date, condition_code),
    CONSTRAINT ck_stock_count_line_number CHECK (line_number > 0),
    CONSTRAINT ck_stock_count_line_tracking_key CHECK (lot_code IS NULL OR serial_number IS NULL),
    CONSTRAINT ck_stock_count_line_condition
        CHECK (condition_code IN ('AVAILABLE', 'QUARANTINED', 'DAMAGED')),
    CONSTRAINT ck_stock_count_line_quantities
        CHECK (theoretical_quantity >= 0 AND (counted_quantity IS NULL OR counted_quantity >= 0))
);

CREATE OR REPLACE FUNCTION enforce_stock_count_scope_lock()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.count_state IN ('COUNTING', 'REVIEW') THEN
        PERFORM pg_advisory_xact_lock(
            hashtextextended(NEW.company_id::text || ':' || NEW.warehouse_id::text, 0));

        IF EXISTS (
            SELECT 1
              FROM plg_inventory.stock_count existing
             WHERE existing.company_id = NEW.company_id
               AND existing.warehouse_id = NEW.warehouse_id
               AND existing.stock_count_id <> NEW.stock_count_id
               AND existing.count_state IN ('COUNTING', 'REVIEW')
               AND (existing.stock_location_id IS NULL
                    OR NEW.stock_location_id IS NULL
                    OR existing.stock_location_id = NEW.stock_location_id)
        ) THEN
            RAISE EXCEPTION 'An active stock count already locks this inventory scope'
                USING ERRCODE = '23P01';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_stock_count_scope_lock
BEFORE INSERT OR UPDATE OF warehouse_id, stock_location_id, count_state
ON stock_count
FOR EACH ROW
EXECUTE FUNCTION enforce_stock_count_scope_lock();
