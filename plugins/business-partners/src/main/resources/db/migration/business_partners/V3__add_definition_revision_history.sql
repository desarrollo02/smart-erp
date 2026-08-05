CREATE TABLE business_partner_definition_revision (
    company_id UUID NOT NULL,
    definition_kind VARCHAR(32) NOT NULL,
    code VARCHAR(48) NOT NULL,
    version BIGINT NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    state VARCHAR(16) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_business_partner_definition_revision
        PRIMARY KEY (company_id, definition_kind, code, version),
    CONSTRAINT fk_business_partner_definition_revision_definition
        FOREIGN KEY (company_id, definition_kind, code)
        REFERENCES business_partner_definition (company_id, definition_kind, code),
    CONSTRAINT ck_business_partner_definition_revision_kind
        CHECK (definition_kind IN ('CHANNEL_KIND')),
    CONSTRAINT ck_business_partner_definition_revision_code
        CHECK (code ~ '^[a-z][a-z0-9_]{0,47}$'),
    CONSTRAINT ck_business_partner_definition_revision_state
        CHECK (state IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_business_partner_definition_revision_version
        CHECK (version >= 0)
);

CREATE INDEX ix_business_partner_definition_revision_history
    ON business_partner_definition_revision
        (company_id, definition_kind, code, version DESC);

INSERT INTO business_partner_definition_revision
    (company_id, definition_kind, code, version, display_name, state, changed_at)
SELECT company_id, definition_kind, code, version, display_name, state, updated_at
FROM business_partner_definition;
