CREATE TABLE sales_term (
    company_id UUID NOT NULL,
    sales_term_id UUID NOT NULL,
    term_code VARCHAR(32) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    due_days INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    entity_version BIGINT NOT NULL,
    PRIMARY KEY (company_id, sales_term_id),
    CONSTRAINT uq_sales_term_code UNIQUE (company_id, term_code),
    CONSTRAINT ck_sales_term_due_days CHECK (due_days BETWEEN 0 AND 3650),
    CONSTRAINT ck_sales_term_version CHECK (entity_version >= 0)
);

CREATE TABLE sales_quote (
    company_id UUID NOT NULL,
    sales_quote_id UUID NOT NULL,
    quote_number VARCHAR(64) NOT NULL,
    customer_id UUID NOT NULL,
    customer_code_snapshot VARCHAR(64) NOT NULL,
    customer_name_snapshot VARCHAR(200) NOT NULL,
    customer_tax_id_snapshot VARCHAR(64) NOT NULL,
    customer_source_version BIGINT NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    currency_minor_unit INTEGER NOT NULL,
    currency_name_snapshot VARCHAR(160) NOT NULL,
    currency_release_id VARCHAR(64) NOT NULL,
    term_id_snapshot UUID NOT NULL,
    term_code_snapshot VARCHAR(32) NOT NULL,
    term_name_snapshot VARCHAR(120) NOT NULL,
    term_due_days_snapshot INTEGER NOT NULL,
    term_source_version BIGINT NOT NULL,
    valid_until DATE NOT NULL,
    quote_state VARCHAR(24) NOT NULL,
    issued_at TIMESTAMPTZ,
    transition_actor_id UUID,
    entity_version BIGINT NOT NULL,
    PRIMARY KEY (company_id, sales_quote_id),
    CONSTRAINT uq_sales_quote_number UNIQUE (company_id, quote_number),
    CONSTRAINT ck_sales_quote_state CHECK (quote_state IN ('DRAFT','ISSUED','ACCEPTED','REJECTED','EXPIRED','CANCELLED')),
    CONSTRAINT ck_sales_quote_state_shape CHECK ((quote_state = 'DRAFT' AND issued_at IS NULL) OR (quote_state <> 'DRAFT' AND issued_at IS NOT NULL)),
    CONSTRAINT ck_sales_quote_versions CHECK (customer_source_version >= 0 AND term_source_version >= 0 AND entity_version >= 0),
    CONSTRAINT ck_sales_quote_currency CHECK (currency_minor_unit BETWEEN 0 AND 9),
    CONSTRAINT ck_sales_quote_term CHECK (term_due_days_snapshot BETWEEN 0 AND 3650)
);

CREATE TABLE sales_quote_line (
    company_id UUID NOT NULL,
    sales_quote_id UUID NOT NULL,
    sales_quote_line_id UUID NOT NULL,
    line_position INTEGER NOT NULL,
    catalog_item_id UUID NOT NULL,
    catalog_code_snapshot VARCHAR(64) NOT NULL,
    item_description_snapshot VARCHAR(240) NOT NULL,
    unit_code_snapshot VARCHAR(16) NOT NULL,
    stock_managed BOOLEAN NOT NULL,
    quoted_quantity NUMERIC(30,6) NOT NULL,
    unit_price NUMERIC(30,6) NOT NULL,
    tax_code_snapshot VARCHAR(32) NOT NULL,
    price_list_id_snapshot VARCHAR(128),
    manual_price BOOLEAN NOT NULL,
    price_exception_reason VARCHAR(240),
    catalog_source_version BIGINT NOT NULL,
    PRIMARY KEY (company_id, sales_quote_id, sales_quote_line_id),
    CONSTRAINT fk_sales_quote_line_owner FOREIGN KEY (company_id, sales_quote_id) REFERENCES sales_quote(company_id, sales_quote_id),
    CONSTRAINT uq_sales_quote_line_identity UNIQUE (company_id, sales_quote_line_id),
    CONSTRAINT uq_sales_quote_line_position UNIQUE (company_id, sales_quote_id, line_position),
    CONSTRAINT ck_sales_quote_line_position CHECK (line_position > 0),
    CONSTRAINT ck_sales_quote_line_quantity CHECK (quoted_quantity > 0),
    CONSTRAINT ck_sales_quote_line_price CHECK (unit_price >= 0),
    CONSTRAINT ck_sales_quote_line_exception CHECK (manual_price = (price_exception_reason IS NOT NULL)),
    CONSTRAINT ck_sales_quote_line_version CHECK (catalog_source_version >= 0)
);

CREATE TABLE sales_order (
    company_id UUID NOT NULL,
    sales_order_id UUID NOT NULL,
    order_number VARCHAR(64) NOT NULL,
    source_quote_id UUID,
    customer_id UUID NOT NULL,
    customer_code_snapshot VARCHAR(64) NOT NULL,
    customer_name_snapshot VARCHAR(200) NOT NULL,
    customer_tax_id_snapshot VARCHAR(64) NOT NULL,
    customer_source_version BIGINT NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    currency_minor_unit INTEGER NOT NULL,
    currency_name_snapshot VARCHAR(160) NOT NULL,
    currency_release_id VARCHAR(64) NOT NULL,
    term_id_snapshot UUID NOT NULL,
    term_code_snapshot VARCHAR(32) NOT NULL,
    term_name_snapshot VARCHAR(120) NOT NULL,
    term_due_days_snapshot INTEGER NOT NULL,
    term_source_version BIGINT NOT NULL,
    order_state VARCHAR(24) NOT NULL,
    entity_version BIGINT NOT NULL,
    PRIMARY KEY (company_id, sales_order_id),
    CONSTRAINT uq_sales_order_number UNIQUE (company_id, order_number),
    CONSTRAINT uq_sales_order_source_quote UNIQUE (company_id, source_quote_id),
    CONSTRAINT fk_sales_order_source_quote FOREIGN KEY (company_id, source_quote_id) REFERENCES sales_quote(company_id, sales_quote_id),
    CONSTRAINT ck_sales_order_state CHECK (order_state IN ('DRAFT','CONFIRMED','CLOSED','CANCELLED')),
    CONSTRAINT ck_sales_order_versions CHECK (customer_source_version >= 0 AND term_source_version >= 0 AND entity_version >= 0),
    CONSTRAINT ck_sales_order_currency CHECK (currency_minor_unit BETWEEN 0 AND 9),
    CONSTRAINT ck_sales_order_term CHECK (term_due_days_snapshot BETWEEN 0 AND 3650)
);

CREATE TABLE sales_order_line (
    company_id UUID NOT NULL,
    sales_order_id UUID NOT NULL,
    sales_order_line_id UUID NOT NULL,
    line_position INTEGER NOT NULL,
    catalog_item_id UUID NOT NULL,
    catalog_code_snapshot VARCHAR(64) NOT NULL,
    item_description_snapshot VARCHAR(240) NOT NULL,
    unit_code_snapshot VARCHAR(16) NOT NULL,
    stock_managed BOOLEAN NOT NULL,
    ordered_quantity NUMERIC(30,6) NOT NULL,
    unit_price NUMERIC(30,6) NOT NULL,
    tax_code_snapshot VARCHAR(32) NOT NULL,
    price_list_id_snapshot VARCHAR(128),
    manual_price BOOLEAN NOT NULL,
    price_exception_reason VARCHAR(240),
    catalog_source_version BIGINT NOT NULL,
    PRIMARY KEY (company_id, sales_order_id, sales_order_line_id),
    CONSTRAINT fk_sales_order_line_owner FOREIGN KEY (company_id, sales_order_id) REFERENCES sales_order(company_id, sales_order_id),
    CONSTRAINT uq_sales_order_line_identity UNIQUE (company_id, sales_order_line_id),
    CONSTRAINT uq_sales_order_line_position UNIQUE (company_id, sales_order_id, line_position),
    CONSTRAINT ck_sales_order_line_position CHECK (line_position > 0),
    CONSTRAINT ck_sales_order_line_quantity CHECK (ordered_quantity > 0),
    CONSTRAINT ck_sales_order_line_price CHECK (unit_price >= 0),
    CONSTRAINT ck_sales_order_line_exception CHECK (manual_price = (price_exception_reason IS NOT NULL)),
    CONSTRAINT ck_sales_order_line_version CHECK (catalog_source_version >= 0)
);

CREATE TABLE sales_order_reservation (
    company_id UUID NOT NULL,
    sales_order_id UUID NOT NULL,
    sales_order_line_id UUID NOT NULL,
    reservation_id UUID NOT NULL,
    PRIMARY KEY (company_id, sales_order_id, sales_order_line_id),
    CONSTRAINT fk_sales_reservation_line FOREIGN KEY (company_id, sales_order_id, sales_order_line_id) REFERENCES sales_order_line(company_id, sales_order_id, sales_order_line_id),
    CONSTRAINT uq_sales_reservation_id UNIQUE (company_id, reservation_id)
);

CREATE TABLE sales_operation (
    company_id UUID NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    operation_type VARCHAR(64) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(32) NOT NULL,
    aggregate_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (company_id, idempotency_key),
    CONSTRAINT ck_sales_operation_fingerprint CHECK (request_fingerprint ~ '^[0-9a-f]{64}$')
);

CREATE TABLE sales_transition_history (
    company_id UUID NOT NULL,
    transition_id UUID NOT NULL,
    aggregate_type VARCHAR(32) NOT NULL,
    aggregate_id UUID NOT NULL,
    from_state VARCHAR(24) NOT NULL,
    to_state VARCHAR(24) NOT NULL,
    actor_id UUID NOT NULL,
    transition_reason VARCHAR(240),
    occurred_at TIMESTAMPTZ NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    PRIMARY KEY (company_id, transition_id),
    CONSTRAINT uq_sales_transition_operation UNIQUE (company_id, idempotency_key)
);

CREATE OR REPLACE FUNCTION reject_sales_history_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'sales history is immutable' USING ERRCODE = '55000';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sales_operation_immutable BEFORE UPDATE OR DELETE ON sales_operation
FOR EACH ROW EXECUTE FUNCTION reject_sales_history_mutation();
CREATE TRIGGER trg_sales_transition_immutable BEFORE UPDATE OR DELETE ON sales_transition_history
FOR EACH ROW EXECUTE FUNCTION reject_sales_history_mutation();
