CREATE TABLE purchase_request (
    company_id UUID NOT NULL,
    purchase_request_id UUID NOT NULL,
    request_number VARCHAR(64) NOT NULL,
    requester_id UUID NOT NULL,
    requested_on DATE NOT NULL,
    request_state VARCHAR(24) NOT NULL,
    submitted_at TIMESTAMPTZ,
    decision_actor_id UUID,
    decision_at TIMESTAMPTZ,
    decision_reason VARCHAR(240),
    entity_version BIGINT NOT NULL,
    PRIMARY KEY (company_id, purchase_request_id),
    CONSTRAINT uq_purchase_request_number UNIQUE (company_id, request_number),
    CONSTRAINT ck_purchase_request_state
        CHECK (request_state IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT ck_purchase_request_decision_pair
        CHECK ((decision_actor_id IS NULL) = (decision_at IS NULL)),
    CONSTRAINT ck_purchase_request_state_shape CHECK (
        (request_state = 'DRAFT'
            AND submitted_at IS NULL AND decision_at IS NULL AND decision_reason IS NULL)
        OR (request_state = 'SUBMITTED'
            AND submitted_at IS NOT NULL AND decision_at IS NULL AND decision_reason IS NULL)
        OR (request_state = 'APPROVED'
            AND submitted_at IS NOT NULL AND decision_at IS NOT NULL
            AND decision_reason IS NULL AND requester_id <> decision_actor_id)
        OR (request_state = 'REJECTED'
            AND submitted_at IS NOT NULL AND decision_at IS NOT NULL
            AND decision_reason IS NOT NULL AND requester_id <> decision_actor_id)
        OR (request_state = 'CANCELLED'
            AND decision_at IS NOT NULL AND decision_reason IS NOT NULL)),
    CONSTRAINT ck_purchase_request_version CHECK (entity_version >= 0)
);

CREATE TABLE purchase_request_line (
    company_id UUID NOT NULL,
    purchase_request_id UUID NOT NULL,
    purchase_request_line_id UUID NOT NULL,
    line_position INTEGER NOT NULL,
    catalog_item_id UUID,
    catalog_code_snapshot VARCHAR(64),
    item_description_snapshot VARCHAR(240) NOT NULL,
    presented_unit_code_snapshot VARCHAR(16) NOT NULL,
    base_unit_code_snapshot VARCHAR(16) NOT NULL,
    conversion_factor NUMERIC(30, 12) NOT NULL,
    line_kind VARCHAR(24) NOT NULL,
    catalog_source_version BIGINT NOT NULL,
    requested_quantity NUMERIC(30, 6) NOT NULL,
    expected_unit_price NUMERIC(30, 6),
    expected_currency_code VARCHAR(3),
    expected_currency_minor_unit INTEGER,
    expected_currency_name VARCHAR(160),
    expected_currency_release_id VARCHAR(64),
    PRIMARY KEY (company_id, purchase_request_id, purchase_request_line_id),
    CONSTRAINT fk_purchase_request_line_owner
        FOREIGN KEY (company_id, purchase_request_id)
        REFERENCES purchase_request (company_id, purchase_request_id),
    CONSTRAINT uq_purchase_request_line_identity
        UNIQUE (company_id, purchase_request_line_id),
    CONSTRAINT uq_purchase_request_line_position
        UNIQUE (company_id, purchase_request_id, line_position),
    CONSTRAINT ck_purchase_request_line_position CHECK (line_position > 0),
    CONSTRAINT ck_purchase_request_line_kind
        CHECK (line_kind IN ('STOCK', 'NON_STOCK', 'SERVICE')),
    CONSTRAINT ck_purchase_request_line_catalog_pair
        CHECK ((catalog_item_id IS NULL) = (catalog_code_snapshot IS NULL)),
    CONSTRAINT ck_purchase_request_line_stock_catalog
        CHECK (line_kind <> 'STOCK' OR catalog_item_id IS NOT NULL),
    CONSTRAINT ck_purchase_request_line_source_version
        CHECK (catalog_source_version >= 0
            AND (catalog_item_id IS NOT NULL OR catalog_source_version = 0)),
    CONSTRAINT ck_purchase_request_line_conversion_factor CHECK (conversion_factor > 0),
    CONSTRAINT ck_purchase_request_line_free_conversion CHECK (
        catalog_item_id IS NOT NULL OR (
            presented_unit_code_snapshot = base_unit_code_snapshot
            AND conversion_factor = 1)),
    CONSTRAINT ck_purchase_request_line_quantity CHECK (requested_quantity > 0),
    CONSTRAINT ck_purchase_request_line_expected_price CHECK (
        (expected_unit_price IS NULL
            AND expected_currency_code IS NULL
            AND expected_currency_minor_unit IS NULL
            AND expected_currency_name IS NULL
            AND expected_currency_release_id IS NULL)
        OR (expected_unit_price >= 0
            AND expected_currency_code IS NOT NULL
            AND expected_currency_minor_unit BETWEEN 0 AND 9
            AND expected_currency_name IS NOT NULL
            AND expected_currency_release_id IS NOT NULL))
);

CREATE TABLE purchase_order (
    company_id UUID NOT NULL,
    purchase_order_id UUID NOT NULL,
    order_number VARCHAR(64) NOT NULL,
    supplier_id UUID NOT NULL,
    supplier_code_snapshot VARCHAR(64) NOT NULL,
    supplier_name_snapshot VARCHAR(200) NOT NULL,
    supplier_source_version BIGINT NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    currency_minor_unit INTEGER NOT NULL,
    currency_name_snapshot VARCHAR(160) NOT NULL,
    currency_release_id VARCHAR(64) NOT NULL,
    direct_order_justification VARCHAR(240),
    order_state VARCHAR(24) NOT NULL,
    issued_by UUID,
    issued_at TIMESTAMPTZ,
    terminal_reason VARCHAR(240),
    entity_version BIGINT NOT NULL,
    PRIMARY KEY (company_id, purchase_order_id),
    CONSTRAINT uq_purchase_order_number UNIQUE (company_id, order_number),
    CONSTRAINT uq_purchase_order_owner
        UNIQUE (company_id, purchase_order_id, supplier_id),
    CONSTRAINT ck_purchase_order_supplier_version CHECK (supplier_source_version >= 0),
    CONSTRAINT ck_purchase_order_currency_minor_unit CHECK (currency_minor_unit BETWEEN 0 AND 9),
    CONSTRAINT ck_purchase_order_state CHECK (order_state IN ('DRAFT', 'ISSUED', 'CLOSED', 'CANCELLED')),
    CONSTRAINT ck_purchase_order_issue_pair CHECK ((issued_by IS NULL) = (issued_at IS NULL)),
    CONSTRAINT ck_purchase_order_state_shape CHECK (
        (order_state = 'DRAFT' AND issued_at IS NULL AND terminal_reason IS NULL)
        OR (order_state = 'ISSUED' AND issued_at IS NOT NULL AND terminal_reason IS NULL)
        OR (order_state = 'CLOSED' AND issued_at IS NOT NULL)
        OR (order_state = 'CANCELLED' AND terminal_reason IS NOT NULL)),
    CONSTRAINT ck_purchase_order_version CHECK (entity_version >= 0)
);

CREATE TABLE purchase_order_line (
    company_id UUID NOT NULL,
    purchase_order_id UUID NOT NULL,
    purchase_order_line_id UUID NOT NULL,
    line_position INTEGER NOT NULL,
    catalog_item_id UUID,
    catalog_code_snapshot VARCHAR(64),
    item_description_snapshot VARCHAR(240) NOT NULL,
    presented_unit_code_snapshot VARCHAR(16) NOT NULL,
    base_unit_code_snapshot VARCHAR(16) NOT NULL,
    conversion_factor NUMERIC(30, 12) NOT NULL,
    line_kind VARCHAR(24) NOT NULL,
    catalog_source_version BIGINT NOT NULL,
    ordered_quantity NUMERIC(30, 6) NOT NULL,
    unit_price NUMERIC(30, 6) NOT NULL,
    received_quantity NUMERIC(30, 6) NOT NULL,
    returned_quantity NUMERIC(30, 6) NOT NULL,
    short_closed_quantity NUMERIC(30, 6) NOT NULL,
    PRIMARY KEY (company_id, purchase_order_id, purchase_order_line_id),
    CONSTRAINT fk_purchase_order_line_owner
        FOREIGN KEY (company_id, purchase_order_id)
        REFERENCES purchase_order (company_id, purchase_order_id),
    CONSTRAINT uq_purchase_order_line_identity
        UNIQUE (company_id, purchase_order_line_id),
    CONSTRAINT uq_purchase_order_line_position
        UNIQUE (company_id, purchase_order_id, line_position),
    CONSTRAINT ck_purchase_order_line_position CHECK (line_position > 0),
    CONSTRAINT ck_purchase_order_line_kind
        CHECK (line_kind IN ('STOCK', 'NON_STOCK', 'SERVICE')),
    CONSTRAINT ck_purchase_order_line_catalog_pair
        CHECK ((catalog_item_id IS NULL) = (catalog_code_snapshot IS NULL)),
    CONSTRAINT ck_purchase_order_line_stock_catalog
        CHECK (line_kind <> 'STOCK' OR catalog_item_id IS NOT NULL),
    CONSTRAINT ck_purchase_order_line_source_version
        CHECK (catalog_source_version >= 0
            AND (catalog_item_id IS NOT NULL OR catalog_source_version = 0)),
    CONSTRAINT ck_purchase_order_line_conversion_factor CHECK (conversion_factor > 0),
    CONSTRAINT ck_purchase_order_line_free_conversion CHECK (
        catalog_item_id IS NOT NULL OR (
            presented_unit_code_snapshot = base_unit_code_snapshot
            AND conversion_factor = 1)),
    CONSTRAINT ck_purchase_order_line_quantities CHECK (
        ordered_quantity > 0 AND unit_price >= 0
        AND received_quantity >= 0 AND returned_quantity >= 0
        AND short_closed_quantity >= 0
        AND returned_quantity <= received_quantity
        AND received_quantity - returned_quantity + short_closed_quantity <= ordered_quantity)
);

CREATE TABLE purchase_order_allocation (
    company_id UUID NOT NULL,
    purchase_order_id UUID NOT NULL,
    purchase_order_line_id UUID NOT NULL,
    purchase_request_id UUID NOT NULL,
    purchase_request_line_id UUID NOT NULL,
    allocation_position INTEGER NOT NULL,
    allocated_quantity NUMERIC(30, 6) NOT NULL,
    PRIMARY KEY (
        company_id, purchase_order_id, purchase_order_line_id,
        purchase_request_id, purchase_request_line_id),
    CONSTRAINT fk_purchase_order_allocation_order_line
        FOREIGN KEY (company_id, purchase_order_id, purchase_order_line_id)
        REFERENCES purchase_order_line (company_id, purchase_order_id, purchase_order_line_id),
    CONSTRAINT fk_purchase_order_allocation_request_line
        FOREIGN KEY (company_id, purchase_request_id, purchase_request_line_id)
        REFERENCES purchase_request_line (company_id, purchase_request_id, purchase_request_line_id),
    CONSTRAINT uq_purchase_order_allocation_position
        UNIQUE (company_id, purchase_order_id, purchase_order_line_id, allocation_position),
    CONSTRAINT ck_purchase_order_allocation_position CHECK (allocation_position > 0),
    CONSTRAINT ck_purchase_order_allocation_quantity CHECK (allocated_quantity > 0)
);

CREATE OR REPLACE FUNCTION enforce_purchase_order_allocation_limits()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    requested_limit NUMERIC(30, 6);
    ordered_limit NUMERIC(30, 6);
    request_total NUMERIC(30, 6);
    order_line_total NUMERIC(30, 6);
BEGIN
    SELECT requested_quantity INTO requested_limit
      FROM plg_purchasing.purchase_request_line
     WHERE company_id = NEW.company_id
       AND purchase_request_id = NEW.purchase_request_id
       AND purchase_request_line_id = NEW.purchase_request_line_id
     FOR UPDATE;
    IF requested_limit IS NULL THEN
        RAISE EXCEPTION 'Purchase request line does not exist in this company'
            USING ERRCODE = '23503';
    END IF;

    SELECT ordered_quantity INTO ordered_limit
      FROM plg_purchasing.purchase_order_line
     WHERE company_id = NEW.company_id
       AND purchase_order_id = NEW.purchase_order_id
       AND purchase_order_line_id = NEW.purchase_order_line_id
     FOR UPDATE;
    IF ordered_limit IS NULL THEN
        RAISE EXCEPTION 'Purchase order line does not exist in this company'
            USING ERRCODE = '23503';
    END IF;

    SELECT COALESCE(SUM(allocated_quantity), 0) INTO request_total
      FROM plg_purchasing.purchase_order_allocation
     WHERE company_id = NEW.company_id
       AND purchase_request_id = NEW.purchase_request_id
       AND purchase_request_line_id = NEW.purchase_request_line_id;
    SELECT COALESCE(SUM(allocated_quantity), 0) INTO order_line_total
      FROM plg_purchasing.purchase_order_allocation
     WHERE company_id = NEW.company_id
       AND purchase_order_id = NEW.purchase_order_id
       AND purchase_order_line_id = NEW.purchase_order_line_id;

    IF TG_OP = 'UPDATE' THEN
        request_total = request_total - OLD.allocated_quantity;
        order_line_total = order_line_total - OLD.allocated_quantity;
    END IF;
    IF request_total + NEW.allocated_quantity > requested_limit
        OR order_line_total + NEW.allocated_quantity > ordered_limit THEN
        RAISE EXCEPTION 'Allocated quantity exceeds request or order line limit'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_purchase_order_allocation_limits
BEFORE INSERT OR UPDATE OF allocated_quantity ON purchase_order_allocation
FOR EACH ROW EXECUTE FUNCTION enforce_purchase_order_allocation_limits();

CREATE TABLE goods_receipt (
    company_id UUID NOT NULL,
    goods_receipt_id UUID NOT NULL,
    receipt_number VARCHAR(64) NOT NULL,
    purchase_order_id UUID NOT NULL,
    receipt_state VARCHAR(24) NOT NULL,
    confirmed_by UUID,
    confirmed_at TIMESTAMPTZ,
    entity_version BIGINT NOT NULL,
    PRIMARY KEY (company_id, goods_receipt_id),
    CONSTRAINT fk_goods_receipt_order
        FOREIGN KEY (company_id, purchase_order_id)
        REFERENCES purchase_order (company_id, purchase_order_id),
    CONSTRAINT uq_goods_receipt_number UNIQUE (company_id, receipt_number),
    CONSTRAINT uq_goods_receipt_owner
        UNIQUE (company_id, goods_receipt_id, purchase_order_id),
    CONSTRAINT ck_goods_receipt_state CHECK (receipt_state IN ('DRAFT', 'CONFIRMED')),
    CONSTRAINT ck_goods_receipt_confirmation_pair CHECK ((confirmed_by IS NULL) = (confirmed_at IS NULL)),
    CONSTRAINT ck_goods_receipt_state_shape CHECK (
        (receipt_state = 'DRAFT' AND confirmed_at IS NULL)
        OR (receipt_state = 'CONFIRMED' AND confirmed_at IS NOT NULL)),
    CONSTRAINT ck_goods_receipt_version CHECK (entity_version >= 0)
);

CREATE TABLE goods_receipt_line (
    company_id UUID NOT NULL,
    goods_receipt_id UUID NOT NULL,
    goods_receipt_line_id UUID NOT NULL,
    line_position INTEGER NOT NULL,
    purchase_order_id UUID NOT NULL,
    purchase_order_line_id UUID NOT NULL,
    line_kind VARCHAR(24) NOT NULL,
    received_quantity NUMERIC(30, 6) NOT NULL,
    warehouse_id UUID,
    stock_location_id UUID,
    lot_code VARCHAR(80),
    serial_number VARCHAR(120),
    expiry_date DATE,
    stock_condition VARCHAR(24),
    stock_movement_id UUID,
    PRIMARY KEY (company_id, goods_receipt_id, goods_receipt_line_id),
    CONSTRAINT fk_goods_receipt_line_owner
        FOREIGN KEY (company_id, goods_receipt_id, purchase_order_id)
        REFERENCES goods_receipt (company_id, goods_receipt_id, purchase_order_id),
    CONSTRAINT fk_goods_receipt_line_order
        FOREIGN KEY (company_id, purchase_order_id, purchase_order_line_id)
        REFERENCES purchase_order_line (company_id, purchase_order_id, purchase_order_line_id),
    CONSTRAINT uq_goods_receipt_line_order_line
        UNIQUE (company_id, goods_receipt_id, purchase_order_line_id),
    CONSTRAINT uq_goods_receipt_line_position
        UNIQUE (company_id, goods_receipt_id, line_position),
    CONSTRAINT ck_goods_receipt_line_position CHECK (line_position > 0),
    CONSTRAINT uq_goods_receipt_line_trace
        UNIQUE (
            company_id, goods_receipt_id, goods_receipt_line_id,
            purchase_order_id, purchase_order_line_id),
    CONSTRAINT uq_goods_receipt_stock_movement
        UNIQUE (company_id, stock_movement_id),
    CONSTRAINT ck_goods_receipt_line_kind
        CHECK (line_kind IN ('STOCK', 'NON_STOCK', 'SERVICE')),
    CONSTRAINT ck_goods_receipt_line_quantity CHECK (received_quantity > 0),
    CONSTRAINT ck_goods_receipt_line_stock_shape CHECK (
        (line_kind = 'STOCK' AND warehouse_id IS NOT NULL
            AND stock_location_id IS NOT NULL AND stock_condition IS NOT NULL)
        OR (line_kind <> 'STOCK'
            AND warehouse_id IS NULL AND stock_location_id IS NULL
            AND lot_code IS NULL AND serial_number IS NULL AND expiry_date IS NULL
            AND stock_condition IS NULL AND stock_movement_id IS NULL)),
    CONSTRAINT ck_goods_receipt_line_condition
        CHECK (stock_condition IS NULL OR stock_condition IN ('AVAILABLE', 'QUARANTINED', 'DAMAGED'))
);

CREATE TABLE supplier_return (
    company_id UUID NOT NULL,
    supplier_return_id UUID NOT NULL,
    return_number VARCHAR(64) NOT NULL,
    purchase_order_id UUID NOT NULL,
    return_reason VARCHAR(240) NOT NULL,
    return_state VARCHAR(24) NOT NULL,
    confirmed_by UUID,
    confirmed_at TIMESTAMPTZ,
    entity_version BIGINT NOT NULL,
    PRIMARY KEY (company_id, supplier_return_id),
    CONSTRAINT fk_supplier_return_order
        FOREIGN KEY (company_id, purchase_order_id)
        REFERENCES purchase_order (company_id, purchase_order_id),
    CONSTRAINT uq_supplier_return_number UNIQUE (company_id, return_number),
    CONSTRAINT uq_supplier_return_owner
        UNIQUE (company_id, supplier_return_id, purchase_order_id),
    CONSTRAINT ck_supplier_return_state CHECK (return_state IN ('DRAFT', 'CONFIRMED')),
    CONSTRAINT ck_supplier_return_confirmation_pair CHECK ((confirmed_by IS NULL) = (confirmed_at IS NULL)),
    CONSTRAINT ck_supplier_return_state_shape CHECK (
        (return_state = 'DRAFT' AND confirmed_at IS NULL)
        OR (return_state = 'CONFIRMED' AND confirmed_at IS NOT NULL)),
    CONSTRAINT ck_supplier_return_version CHECK (entity_version >= 0)
);

CREATE TABLE supplier_return_line (
    company_id UUID NOT NULL,
    supplier_return_id UUID NOT NULL,
    supplier_return_line_id UUID NOT NULL,
    line_position INTEGER NOT NULL,
    purchase_order_id UUID NOT NULL,
    purchase_order_line_id UUID NOT NULL,
    goods_receipt_id UUID NOT NULL,
    goods_receipt_line_id UUID NOT NULL,
    line_kind VARCHAR(24) NOT NULL,
    returned_quantity NUMERIC(30, 6) NOT NULL,
    warehouse_id UUID,
    stock_location_id UUID,
    lot_code VARCHAR(80),
    serial_number VARCHAR(120),
    expiry_date DATE,
    stock_condition VARCHAR(24),
    stock_movement_id UUID,
    PRIMARY KEY (company_id, supplier_return_id, supplier_return_line_id),
    CONSTRAINT fk_supplier_return_line_owner
        FOREIGN KEY (company_id, supplier_return_id, purchase_order_id)
        REFERENCES supplier_return (company_id, supplier_return_id, purchase_order_id),
    CONSTRAINT fk_supplier_return_line_receipt
        FOREIGN KEY (
            company_id, goods_receipt_id, goods_receipt_line_id,
            purchase_order_id, purchase_order_line_id)
        REFERENCES goods_receipt_line (
            company_id, goods_receipt_id, goods_receipt_line_id,
            purchase_order_id, purchase_order_line_id),
    CONSTRAINT uq_supplier_return_receipt_line
        UNIQUE (company_id, supplier_return_id, goods_receipt_line_id),
    CONSTRAINT uq_supplier_return_line_position
        UNIQUE (company_id, supplier_return_id, line_position),
    CONSTRAINT ck_supplier_return_line_position CHECK (line_position > 0),
    CONSTRAINT uq_supplier_return_stock_movement
        UNIQUE (company_id, stock_movement_id),
    CONSTRAINT ck_supplier_return_line_kind
        CHECK (line_kind IN ('STOCK', 'NON_STOCK', 'SERVICE')),
    CONSTRAINT ck_supplier_return_line_quantity CHECK (returned_quantity > 0),
    CONSTRAINT ck_supplier_return_line_stock_shape CHECK (
        (line_kind = 'STOCK' AND warehouse_id IS NOT NULL
            AND stock_location_id IS NOT NULL AND stock_condition IS NOT NULL)
        OR (line_kind <> 'STOCK'
            AND warehouse_id IS NULL AND stock_location_id IS NULL
            AND lot_code IS NULL AND serial_number IS NULL AND expiry_date IS NULL
            AND stock_condition IS NULL AND stock_movement_id IS NULL)),
    CONSTRAINT ck_supplier_return_line_condition
        CHECK (stock_condition IS NULL OR stock_condition IN ('AVAILABLE', 'QUARANTINED', 'DAMAGED'))
);

CREATE OR REPLACE FUNCTION reject_final_purchasing_line_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    parent_state VARCHAR(24);
BEGIN
    IF TG_TABLE_NAME = 'purchase_request_line' THEN
        SELECT request_state INTO parent_state
          FROM plg_purchasing.purchase_request
         WHERE company_id = COALESCE(NEW.company_id, OLD.company_id)
           AND purchase_request_id = COALESCE(NEW.purchase_request_id, OLD.purchase_request_id);
        IF parent_state <> 'DRAFT' THEN
            RAISE EXCEPTION 'Final purchase request lines are immutable' USING ERRCODE = 'P2001';
        END IF;
    ELSIF TG_TABLE_NAME = 'goods_receipt_line' THEN
        SELECT receipt_state INTO parent_state
          FROM plg_purchasing.goods_receipt
         WHERE company_id = COALESCE(NEW.company_id, OLD.company_id)
           AND goods_receipt_id = COALESCE(NEW.goods_receipt_id, OLD.goods_receipt_id);
        IF parent_state = 'CONFIRMED' THEN
            RAISE EXCEPTION 'Confirmed receipt lines are immutable' USING ERRCODE = 'P2001';
        END IF;
    ELSE
        SELECT return_state INTO parent_state
          FROM plg_purchasing.supplier_return
         WHERE company_id = COALESCE(NEW.company_id, OLD.company_id)
           AND supplier_return_id = COALESCE(NEW.supplier_return_id, OLD.supplier_return_id);
        IF parent_state = 'CONFIRMED' THEN
            RAISE EXCEPTION 'Confirmed supplier-return lines are immutable' USING ERRCODE = 'P2001';
        END IF;
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_purchase_request_line_immutable
BEFORE INSERT OR UPDATE OR DELETE ON purchase_request_line
FOR EACH ROW EXECUTE FUNCTION reject_final_purchasing_line_change();

CREATE TRIGGER trg_goods_receipt_line_immutable
BEFORE INSERT OR UPDATE OR DELETE ON goods_receipt_line
FOR EACH ROW EXECUTE FUNCTION reject_final_purchasing_line_change();

CREATE TRIGGER trg_supplier_return_line_immutable
BEFORE INSERT OR UPDATE OR DELETE ON supplier_return_line
FOR EACH ROW EXECUTE FUNCTION reject_final_purchasing_line_change();

CREATE OR REPLACE FUNCTION validate_goods_receipt_confirmation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    missing_lines INTEGER;
BEGIN
    IF NEW.receipt_state = 'CONFIRMED' THEN
        SELECT COUNT(*) INTO missing_lines
          FROM plg_purchasing.goods_receipt_line line
         WHERE line.company_id = NEW.company_id
           AND line.goods_receipt_id = NEW.goods_receipt_id
           AND line.line_kind = 'STOCK'
           AND line.stock_movement_id IS NULL;
        IF missing_lines > 0 THEN
            RAISE EXCEPTION 'Confirmed stock receipt lines require inventory movement' USING ERRCODE = '23514';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION validate_supplier_return_confirmation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    missing_lines INTEGER;
BEGIN
    IF NEW.return_state = 'CONFIRMED' THEN
        SELECT COUNT(*) INTO missing_lines
          FROM plg_purchasing.supplier_return_line line
         WHERE line.company_id = NEW.company_id
           AND line.supplier_return_id = NEW.supplier_return_id
           AND line.line_kind = 'STOCK'
           AND line.stock_movement_id IS NULL;
        IF missing_lines > 0 THEN
            RAISE EXCEPTION 'Confirmed stock return lines require inventory movement' USING ERRCODE = '23514';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_goods_receipt_confirmation
BEFORE INSERT OR UPDATE OF receipt_state ON goods_receipt
FOR EACH ROW EXECUTE FUNCTION validate_goods_receipt_confirmation();

CREATE TRIGGER trg_supplier_return_confirmation
BEFORE INSERT OR UPDATE OF return_state ON supplier_return
FOR EACH ROW EXECUTE FUNCTION validate_supplier_return_confirmation();

CREATE OR REPLACE FUNCTION reject_confirmed_goods_receipt_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.receipt_state = 'CONFIRMED' THEN
        RAISE EXCEPTION 'Confirmed purchasing documents are immutable' USING ERRCODE = 'P2001';
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION reject_confirmed_supplier_return_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.return_state = 'CONFIRMED' THEN
        RAISE EXCEPTION 'Confirmed purchasing documents are immutable' USING ERRCODE = 'P2001';
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_goods_receipt_immutable
BEFORE UPDATE OR DELETE ON goods_receipt
FOR EACH ROW EXECUTE FUNCTION reject_confirmed_goods_receipt_change();

CREATE TRIGGER trg_supplier_return_immutable
BEFORE UPDATE OR DELETE ON supplier_return
FOR EACH ROW EXECUTE FUNCTION reject_confirmed_supplier_return_change();
