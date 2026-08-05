CREATE TABLE stock_reservation_operation (
    company_id UUID NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    stock_reservation_id UUID NOT NULL,
    operation_type VARCHAR(24) NOT NULL,
    operation_quantity NUMERIC(30, 6) NOT NULL,
    resulting_consumed_quantity NUMERIC(30, 6) NOT NULL,
    resulting_released_quantity NUMERIC(30, 6) NOT NULL,
    resulting_remaining_quantity NUMERIC(30, 6) NOT NULL,
    resulting_state VARCHAR(32) NOT NULL,
    resulting_version BIGINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (company_id, idempotency_key),
    CONSTRAINT fk_stock_reservation_operation_owner
        FOREIGN KEY (company_id, stock_reservation_id)
        REFERENCES stock_reservation (company_id, stock_reservation_id),
    CONSTRAINT ck_stock_reservation_operation_type
        CHECK (operation_type IN ('CONSUME', 'RELEASE', 'EXPIRE')),
    CONSTRAINT ck_stock_reservation_operation_quantities
        CHECK (operation_quantity > 0
            AND resulting_consumed_quantity >= 0
            AND resulting_released_quantity >= 0
            AND resulting_remaining_quantity >= 0),
    CONSTRAINT ck_stock_reservation_operation_state
        CHECK (resulting_state IN ('ACTIVE', 'PARTIALLY_CONSUMED', 'CONSUMED', 'RELEASED', 'EXPIRED')),
    CONSTRAINT ck_stock_reservation_operation_version CHECK (resulting_version >= 0)
);

CREATE INDEX ix_stock_reservation_operation_reservation
    ON stock_reservation_operation (company_id, stock_reservation_id, occurred_at);
