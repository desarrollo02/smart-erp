CREATE TABLE company_reference_policy_history (
    company_id UUID NOT NULL,
    catalog_kind VARCHAR(16) NOT NULL,
    reference_code VARCHAR(3) NOT NULL,
    version BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL,
    actor_user_id UUID NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_company_reference_policy_history
        PRIMARY KEY (company_id, catalog_kind, reference_code, version),
    CONSTRAINT ck_company_reference_policy_history_kind
        CHECK (catalog_kind IN ('COUNTRY', 'CURRENCY')),
    CONSTRAINT ck_company_reference_policy_history_code
        CHECK (
            (catalog_kind = 'COUNTRY' AND reference_code ~ '^[A-Z]{2}$')
            OR (catalog_kind = 'CURRENCY' AND reference_code ~ '^[A-Z]{3}$')
        ),
    CONSTRAINT ck_company_reference_policy_history_version CHECK (version > 0),
    CONSTRAINT ck_company_reference_policy_history_correlation
        CHECK (correlation_id ~ '^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$')
);

CREATE INDEX ix_company_reference_policy_history_lookup
    ON company_reference_policy_history
        (company_id, catalog_kind, reference_code, version DESC);
