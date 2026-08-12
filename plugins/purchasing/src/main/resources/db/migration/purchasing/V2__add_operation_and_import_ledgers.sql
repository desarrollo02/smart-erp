CREATE TABLE purchasing_operation (
    company_id UUID NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    operation_type VARCHAR(64) NOT NULL,
    request_fingerprint CHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id UUID NOT NULL,
    resulting_version BIGINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (company_id, idempotency_key),
    CONSTRAINT ck_purchasing_operation_fingerprint
        CHECK (request_fingerprint ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_purchasing_operation_version CHECK (resulting_version >= 0)
);

CREATE TABLE purchasing_import (
    company_id UUID NOT NULL,
    source_system VARCHAR(64) NOT NULL,
    source_record_key VARCHAR(160) NOT NULL,
    batch_checksum CHAR(64),
    request_fingerprint CHAR(64) NOT NULL,
    document_type VARCHAR(32) NOT NULL,
    document_id UUID NOT NULL,
    imported_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (company_id, source_system, source_record_key),
    CONSTRAINT uq_purchasing_import_document
        UNIQUE (company_id, document_type, document_id),
    CONSTRAINT ck_purchasing_import_checksum
        CHECK (batch_checksum IS NULL OR batch_checksum ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_purchasing_import_fingerprint
        CHECK (request_fingerprint ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_purchasing_import_document_type
        CHECK (document_type IN ('PURCHASE_REQUEST', 'PURCHASE_ORDER'))
);
