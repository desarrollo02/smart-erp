CREATE TABLE business_partner_definition (
    company_id UUID NOT NULL,
    definition_kind VARCHAR(32) NOT NULL,
    code VARCHAR(48) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    state VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_business_partner_definition
        PRIMARY KEY (company_id, definition_kind, code),
    CONSTRAINT ck_business_partner_definition_kind
        CHECK (definition_kind IN ('CHANNEL_KIND')),
    CONSTRAINT ck_business_partner_definition_code
        CHECK (code ~ '^[a-z][a-z0-9_]{0,47}$'),
    CONSTRAINT ck_business_partner_definition_state
        CHECK (state IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_business_partner_definition_version
        CHECK (version >= 0)
);

CREATE INDEX ix_business_partner_definition_directory
    ON business_partner_definition (company_id, definition_kind, state, display_name, code);

INSERT INTO business_partner_definition
    (company_id, definition_kind, code, display_name, state)
SELECT DISTINCT partner.company_id, 'CHANNEL_KIND', seed.code, seed.display_name, 'ACTIVE'
FROM business_partner partner
CROSS JOIN (VALUES
    ('email', 'Correo electrónico'),
    ('phone', 'Teléfono'),
    ('whatsapp', 'WhatsApp'),
    ('website', 'Sitio web')
) AS seed(code, display_name)
ON CONFLICT (company_id, definition_kind, code) DO NOTHING;
