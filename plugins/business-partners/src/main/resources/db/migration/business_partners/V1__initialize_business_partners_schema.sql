CREATE TABLE business_partner (
    company_id UUID NOT NULL,
    business_partner_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    kind VARCHAR(24) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    legal_name VARCHAR(200),
    trade_name VARCHAR(200),
    state VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_business_partner PRIMARY KEY (company_id, business_partner_id),
    CONSTRAINT uq_business_partner_code UNIQUE (company_id, code),
    CONSTRAINT ck_business_partner_kind CHECK (kind IN ('NATURAL_PERSON', 'ORGANIZATION')),
    CONSTRAINT ck_business_partner_state CHECK (state IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_business_partner_version CHECK (version >= 0)
);

CREATE INDEX ix_business_partner_state
    ON business_partner (company_id, state, business_partner_id);
CREATE INDEX ix_business_partner_display_name
    ON business_partner (company_id, lower(display_name), business_partner_id);

CREATE TABLE business_partner_role (
    company_id UUID NOT NULL,
    business_partner_id UUID NOT NULL,
    role_type VARCHAR(16) NOT NULL,
    state VARCHAR(16) NOT NULL,
    role_code VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_business_partner_role
        PRIMARY KEY (company_id, business_partner_id, role_type),
    CONSTRAINT fk_business_partner_role_owner
        FOREIGN KEY (company_id, business_partner_id)
        REFERENCES business_partner (company_id, business_partner_id),
    CONSTRAINT ck_business_partner_role_type CHECK (role_type IN ('CLIENT', 'SUPPLIER')),
    CONSTRAINT ck_business_partner_role_state CHECK (state IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX uq_business_partner_role_code
    ON business_partner_role (company_id, role_type, role_code)
    WHERE role_code IS NOT NULL;
CREATE INDEX ix_business_partner_role_state
    ON business_partner_role (company_id, role_type, state, business_partner_id);

CREATE TABLE business_partner_identification (
    company_id UUID NOT NULL,
    business_partner_id UUID NOT NULL,
    identification_id UUID NOT NULL,
    type_code VARCHAR(48) NOT NULL,
    country_code CHAR(2),
    presented_value VARCHAR(100) NOT NULL,
    normalized_value VARCHAR(100) NOT NULL,
    check_digit VARCHAR(16),
    valid_until DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_business_partner_identification
        PRIMARY KEY (company_id, business_partner_id, identification_id),
    CONSTRAINT fk_business_partner_identification_owner
        FOREIGN KEY (company_id, business_partner_id)
        REFERENCES business_partner (company_id, business_partner_id),
    CONSTRAINT ck_business_partner_identification_country
        CHECK (country_code IS NULL OR country_code ~ '^[A-Z]{2}$')
);

CREATE INDEX ix_business_partner_identification_candidate
    ON business_partner_identification
    (company_id, type_code, country_code, normalized_value, business_partner_id);

CREATE TABLE business_partner_address (
    company_id UUID NOT NULL,
    business_partner_id UUID NOT NULL,
    address_id UUID NOT NULL,
    type_code VARCHAR(48) NOT NULL,
    purpose_code VARCHAR(48) NOT NULL,
    address_line VARCHAR(250) NOT NULL,
    additional_line VARCHAR(250),
    house_number VARCHAR(32),
    postal_code VARCHAR(32),
    country_code CHAR(2),
    first_administrative_area VARCHAR(120),
    locality VARCHAR(120),
    active BOOLEAN NOT NULL,
    is_primary BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_business_partner_address
        PRIMARY KEY (company_id, business_partner_id, address_id),
    CONSTRAINT fk_business_partner_address_owner
        FOREIGN KEY (company_id, business_partner_id)
        REFERENCES business_partner (company_id, business_partner_id),
    CONSTRAINT ck_business_partner_address_country
        CHECK (country_code IS NULL OR country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_business_partner_address_primary
        CHECK (active OR NOT is_primary)
);

CREATE UNIQUE INDEX uq_business_partner_address_primary
    ON business_partner_address (company_id, business_partner_id, type_code, purpose_code)
    WHERE active AND is_primary;
CREATE INDEX ix_business_partner_address_owner
    ON business_partner_address (company_id, business_partner_id, active);

CREATE TABLE business_partner_channel (
    company_id UUID NOT NULL,
    business_partner_id UUID NOT NULL,
    channel_id UUID NOT NULL,
    kind_code VARCHAR(48) NOT NULL,
    purpose_code VARCHAR(48) NOT NULL,
    channel_value VARCHAR(254) NOT NULL,
    active BOOLEAN NOT NULL,
    is_primary BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_business_partner_channel
        PRIMARY KEY (company_id, business_partner_id, channel_id),
    CONSTRAINT fk_business_partner_channel_owner
        FOREIGN KEY (company_id, business_partner_id)
        REFERENCES business_partner (company_id, business_partner_id),
    CONSTRAINT ck_business_partner_channel_primary
        CHECK (active OR NOT is_primary)
);

CREATE UNIQUE INDEX uq_business_partner_channel_primary
    ON business_partner_channel (company_id, business_partner_id, kind_code, purpose_code)
    WHERE active AND is_primary;
CREATE INDEX ix_business_partner_channel_owner
    ON business_partner_channel (company_id, business_partner_id, active);

CREATE TABLE business_partner_contact (
    company_id UUID NOT NULL,
    business_partner_id UUID NOT NULL,
    contact_id UUID NOT NULL,
    contact_name VARCHAR(200) NOT NULL,
    position_name VARCHAR(200),
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_business_partner_contact
        PRIMARY KEY (company_id, business_partner_id, contact_id),
    CONSTRAINT fk_business_partner_contact_owner
        FOREIGN KEY (company_id, business_partner_id)
        REFERENCES business_partner (company_id, business_partner_id)
);

CREATE INDEX ix_business_partner_contact_owner
    ON business_partner_contact (company_id, business_partner_id, active);

CREATE TABLE business_partner_contact_channel (
    company_id UUID NOT NULL,
    business_partner_id UUID NOT NULL,
    contact_id UUID NOT NULL,
    channel_id UUID NOT NULL,
    kind_code VARCHAR(48) NOT NULL,
    purpose_code VARCHAR(48) NOT NULL,
    channel_value VARCHAR(254) NOT NULL,
    active BOOLEAN NOT NULL,
    is_primary BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_business_partner_contact_channel
        PRIMARY KEY (company_id, business_partner_id, contact_id, channel_id),
    CONSTRAINT fk_business_partner_contact_channel_owner
        FOREIGN KEY (company_id, business_partner_id, contact_id)
        REFERENCES business_partner_contact (company_id, business_partner_id, contact_id),
    CONSTRAINT ck_business_partner_contact_channel_primary
        CHECK (active OR NOT is_primary)
);

CREATE UNIQUE INDEX uq_business_partner_contact_channel_primary
    ON business_partner_contact_channel
    (company_id, business_partner_id, contact_id, kind_code, purpose_code)
    WHERE active AND is_primary;
CREATE INDEX ix_business_partner_contact_channel_owner
    ON business_partner_contact_channel
    (company_id, business_partner_id, contact_id, active);

CREATE TABLE business_partner_code_sequence (
    company_id UUID NOT NULL,
    sequence_scope VARCHAR(64) NOT NULL,
    next_value BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_business_partner_code_sequence PRIMARY KEY (company_id, sequence_scope),
    CONSTRAINT ck_business_partner_code_sequence_next CHECK (next_value > 0)
);

