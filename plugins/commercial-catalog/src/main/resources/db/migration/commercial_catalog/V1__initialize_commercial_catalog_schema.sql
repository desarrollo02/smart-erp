CREATE TABLE unit_definition (
    company_id UUID NOT NULL,
    unit_code VARCHAR(16) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    decimal_scale INTEGER NOT NULL,
    state VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_unit_definition PRIMARY KEY (company_id, unit_code),
    CONSTRAINT ck_unit_definition_code CHECK (unit_code ~ '^[A-Z0-9][A-Z0-9_.-]{0,15}$'),
    CONSTRAINT ck_unit_definition_scale CHECK (decimal_scale BETWEEN 0 AND 12),
    CONSTRAINT ck_unit_definition_state CHECK (state IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_unit_definition_version CHECK (version >= 0)
);

CREATE TABLE category_definition (
    company_id UUID NOT NULL,
    category_id UUID NOT NULL,
    parent_category_id UUID,
    code VARCHAR(64) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    state VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_category_definition PRIMARY KEY (company_id, category_id),
    CONSTRAINT uq_category_definition_code UNIQUE (company_id, code),
    CONSTRAINT fk_category_definition_parent FOREIGN KEY (company_id, parent_category_id)
        REFERENCES category_definition (company_id, category_id),
    CONSTRAINT ck_category_definition_self CHECK (parent_category_id IS NULL OR parent_category_id <> category_id),
    CONSTRAINT ck_category_definition_state CHECK (state IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_category_definition_version CHECK (version >= 0)
);

CREATE TABLE brand_definition (
    company_id UUID NOT NULL,
    brand_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    state VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_brand_definition PRIMARY KEY (company_id, brand_id),
    CONSTRAINT uq_brand_definition_code UNIQUE (company_id, code),
    CONSTRAINT ck_brand_definition_state CHECK (state IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_brand_definition_version CHECK (version >= 0)
);

CREATE TABLE tag_definition (
    company_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    state VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_tag_definition PRIMARY KEY (company_id, tag_id),
    CONSTRAINT uq_tag_definition_code UNIQUE (company_id, code),
    CONSTRAINT ck_tag_definition_state CHECK (state IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_tag_definition_version CHECK (version >= 0)
);

CREATE TABLE tax_profile (
    company_id UUID NOT NULL,
    tax_profile_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    state VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_tax_profile PRIMARY KEY (company_id, tax_profile_id),
    CONSTRAINT uq_tax_profile_code UNIQUE (company_id, code),
    CONSTRAINT ck_tax_profile_state CHECK (state IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_tax_profile_version CHECK (version >= 0)
);

CREATE TABLE tax_profile_revision (
    company_id UUID NOT NULL,
    tax_profile_id UUID NOT NULL,
    profile_version BIGINT NOT NULL,
    internal_kind_code VARCHAR(48) NOT NULL,
    description VARCHAR(250) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_tax_profile_revision PRIMARY KEY (company_id, tax_profile_id, profile_version),
    CONSTRAINT fk_tax_profile_revision_owner FOREIGN KEY (company_id, tax_profile_id)
        REFERENCES tax_profile (company_id, tax_profile_id),
    CONSTRAINT ck_tax_profile_revision_version CHECK (profile_version >= 0),
    CONSTRAINT ck_tax_profile_revision_validity CHECK (valid_until IS NULL OR valid_until > valid_from)
);

CREATE TABLE variant_family (
    company_id UUID NOT NULL,
    variant_family_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    state VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_variant_family PRIMARY KEY (company_id, variant_family_id),
    CONSTRAINT uq_variant_family_code UNIQUE (company_id, code),
    CONSTRAINT ck_variant_family_state CHECK (state IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_variant_family_version CHECK (version >= 0)
);

CREATE TABLE variant_attribute_definition (
    company_id UUID NOT NULL,
    variant_family_id UUID NOT NULL,
    attribute_code VARCHAR(32) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    value_type VARCHAR(16) NOT NULL,
    required BOOLEAN NOT NULL,
    position INTEGER NOT NULL,
    CONSTRAINT pk_variant_attribute_definition
        PRIMARY KEY (company_id, variant_family_id, attribute_code),
    CONSTRAINT uq_variant_attribute_definition_type
        UNIQUE (company_id, variant_family_id, attribute_code, value_type),
    CONSTRAINT fk_variant_attribute_definition_owner
        FOREIGN KEY (company_id, variant_family_id)
        REFERENCES variant_family (company_id, variant_family_id),
    CONSTRAINT ck_variant_attribute_definition_type CHECK (value_type IN ('TEXT', 'NUMBER', 'BOOLEAN')),
    CONSTRAINT ck_variant_attribute_definition_position CHECK (position >= 0)
);

CREATE TABLE catalog_item (
    company_id UUID NOT NULL,
    catalog_item_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    item_type VARCHAR(16) NOT NULL,
    state VARCHAR(16) NOT NULL,
    base_unit_code VARCHAR(16) NOT NULL,
    tax_profile_id UUID NOT NULL,
    tax_profile_version BIGINT NOT NULL,
    brand_id UUID,
    replacement_item_id UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_catalog_item PRIMARY KEY (company_id, catalog_item_id),
    CONSTRAINT uq_catalog_item_code UNIQUE (company_id, code),
    CONSTRAINT fk_catalog_item_base_unit FOREIGN KEY (company_id, base_unit_code)
        REFERENCES unit_definition (company_id, unit_code),
    CONSTRAINT fk_catalog_item_tax_profile FOREIGN KEY (company_id, tax_profile_id, tax_profile_version)
        REFERENCES tax_profile_revision (company_id, tax_profile_id, profile_version),
    CONSTRAINT fk_catalog_item_brand FOREIGN KEY (company_id, brand_id)
        REFERENCES brand_definition (company_id, brand_id),
    CONSTRAINT fk_catalog_item_replacement FOREIGN KEY (company_id, replacement_item_id)
        REFERENCES catalog_item (company_id, catalog_item_id),
    CONSTRAINT ck_catalog_item_type CHECK (item_type IN ('PRODUCT', 'SERVICE')),
    CONSTRAINT ck_catalog_item_state CHECK (state IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_catalog_item_replacement CHECK (replacement_item_id IS NULL OR replacement_item_id <> catalog_item_id),
    CONSTRAINT ck_catalog_item_active_replacement CHECK (state = 'INACTIVE' OR replacement_item_id IS NULL),
    CONSTRAINT ck_catalog_item_version CHECK (version >= 0)
);

CREATE INDEX ix_catalog_item_state ON catalog_item (company_id, state, catalog_item_id);
CREATE INDEX ix_catalog_item_name ON catalog_item (company_id, lower(display_name), catalog_item_id);
CREATE INDEX ix_catalog_item_replacement ON catalog_item (company_id, replacement_item_id)
    WHERE replacement_item_id IS NOT NULL;

CREATE TABLE catalog_item_scope (
    company_id UUID NOT NULL,
    catalog_item_id UUID NOT NULL,
    scope_code VARCHAR(16) NOT NULL,
    CONSTRAINT pk_catalog_item_scope PRIMARY KEY (company_id, catalog_item_id, scope_code),
    CONSTRAINT fk_catalog_item_scope_owner FOREIGN KEY (company_id, catalog_item_id)
        REFERENCES catalog_item (company_id, catalog_item_id),
    CONSTRAINT ck_catalog_item_scope CHECK (scope_code IN ('PURCHASE', 'SALE'))
);

CREATE TABLE catalog_item_identifier (
    company_id UUID NOT NULL,
    catalog_item_id UUID NOT NULL,
    identifier_id UUID NOT NULL,
    type_code VARCHAR(32) NOT NULL,
    presented_value VARCHAR(128) NOT NULL,
    normalized_value VARCHAR(128) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_catalog_item_identifier PRIMARY KEY (company_id, catalog_item_id, identifier_id),
    CONSTRAINT fk_catalog_item_identifier_owner FOREIGN KEY (company_id, catalog_item_id)
        REFERENCES catalog_item (company_id, catalog_item_id)
);

CREATE UNIQUE INDEX uq_catalog_item_identifier_active
    ON catalog_item_identifier (company_id, type_code, normalized_value) WHERE active;
CREATE INDEX ix_catalog_item_identifier_item
    ON catalog_item_identifier (company_id, catalog_item_id, active);

CREATE TABLE catalog_item_unit_conversion (
    company_id UUID NOT NULL,
    catalog_item_id UUID NOT NULL,
    unit_code VARCHAR(16) NOT NULL,
    to_base_factor NUMERIC(38,18) NOT NULL,
    active BOOLEAN NOT NULL,
    CONSTRAINT pk_catalog_item_unit_conversion PRIMARY KEY (company_id, catalog_item_id, unit_code),
    CONSTRAINT fk_catalog_item_unit_conversion_owner FOREIGN KEY (company_id, catalog_item_id)
        REFERENCES catalog_item (company_id, catalog_item_id),
    CONSTRAINT fk_catalog_item_unit_conversion_unit FOREIGN KEY (company_id, unit_code)
        REFERENCES unit_definition (company_id, unit_code),
    CONSTRAINT ck_catalog_item_unit_conversion_factor CHECK (to_base_factor > 0)
);

CREATE TABLE catalog_item_unit_purpose (
    company_id UUID NOT NULL,
    catalog_item_id UUID NOT NULL,
    unit_code VARCHAR(16) NOT NULL,
    purpose_code VARCHAR(16) NOT NULL,
    is_default BOOLEAN NOT NULL,
    CONSTRAINT pk_catalog_item_unit_purpose
        PRIMARY KEY (company_id, catalog_item_id, unit_code, purpose_code),
    CONSTRAINT fk_catalog_item_unit_purpose_owner
        FOREIGN KEY (company_id, catalog_item_id, unit_code)
        REFERENCES catalog_item_unit_conversion (company_id, catalog_item_id, unit_code),
    CONSTRAINT ck_catalog_item_unit_purpose CHECK (purpose_code IN ('PURCHASE', 'SALE', 'CONSUMPTION'))
);

CREATE UNIQUE INDEX uq_catalog_item_unit_default
    ON catalog_item_unit_purpose (company_id, catalog_item_id, purpose_code) WHERE is_default;

CREATE TABLE catalog_item_category (
    company_id UUID NOT NULL,
    catalog_item_id UUID NOT NULL,
    category_id UUID NOT NULL,
    is_primary BOOLEAN NOT NULL,
    CONSTRAINT pk_catalog_item_category PRIMARY KEY (company_id, catalog_item_id, category_id),
    CONSTRAINT fk_catalog_item_category_owner FOREIGN KEY (company_id, catalog_item_id)
        REFERENCES catalog_item (company_id, catalog_item_id),
    CONSTRAINT fk_catalog_item_category_definition FOREIGN KEY (company_id, category_id)
        REFERENCES category_definition (company_id, category_id)
);

CREATE UNIQUE INDEX uq_catalog_item_primary_category
    ON catalog_item_category (company_id, catalog_item_id) WHERE is_primary;

CREATE TABLE catalog_item_tag (
    company_id UUID NOT NULL,
    catalog_item_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    CONSTRAINT pk_catalog_item_tag PRIMARY KEY (company_id, catalog_item_id, tag_id),
    CONSTRAINT fk_catalog_item_tag_owner FOREIGN KEY (company_id, catalog_item_id)
        REFERENCES catalog_item (company_id, catalog_item_id),
    CONSTRAINT fk_catalog_item_tag_definition FOREIGN KEY (company_id, tag_id)
        REFERENCES tag_definition (company_id, tag_id)
);

CREATE TABLE catalog_item_variant (
    company_id UUID NOT NULL,
    catalog_item_id UUID NOT NULL,
    variant_family_id UUID NOT NULL,
    CONSTRAINT pk_catalog_item_variant PRIMARY KEY (company_id, catalog_item_id),
    CONSTRAINT uq_catalog_item_variant_family
        UNIQUE (company_id, catalog_item_id, variant_family_id),
    CONSTRAINT fk_catalog_item_variant_owner FOREIGN KEY (company_id, catalog_item_id)
        REFERENCES catalog_item (company_id, catalog_item_id),
    CONSTRAINT fk_catalog_item_variant_family FOREIGN KEY (company_id, variant_family_id)
        REFERENCES variant_family (company_id, variant_family_id)
);

CREATE TABLE catalog_item_variant_attribute (
    company_id UUID NOT NULL,
    catalog_item_id UUID NOT NULL,
    variant_family_id UUID NOT NULL,
    attribute_code VARCHAR(32) NOT NULL,
    value_type VARCHAR(16) NOT NULL,
    attribute_value VARCHAR(100) NOT NULL,
    CONSTRAINT pk_catalog_item_variant_attribute
        PRIMARY KEY (company_id, catalog_item_id, attribute_code),
    CONSTRAINT fk_catalog_item_variant_attribute_owner
        FOREIGN KEY (company_id, catalog_item_id, variant_family_id)
        REFERENCES catalog_item_variant (company_id, catalog_item_id, variant_family_id),
    CONSTRAINT fk_catalog_item_variant_attribute_definition
        FOREIGN KEY (company_id, variant_family_id, attribute_code, value_type)
        REFERENCES variant_attribute_definition (company_id, variant_family_id, attribute_code, value_type),
    CONSTRAINT ck_catalog_item_variant_attribute_type CHECK (value_type IN ('TEXT', 'NUMBER', 'BOOLEAN'))
);

CREATE TABLE price_list (
    company_id UUID NOT NULL,
    price_list_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    currency_code CHAR(3) NOT NULL,
    tax_mode VARCHAR(16) NOT NULL,
    amount_scale INTEGER NOT NULL,
    rounding_mode VARCHAR(16) NOT NULL,
    state VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_price_list PRIMARY KEY (company_id, price_list_id),
    CONSTRAINT uq_price_list_code UNIQUE (company_id, code),
    CONSTRAINT ck_price_list_currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_price_list_tax_mode CHECK (tax_mode IN ('NET', 'TAX_INCLUDED')),
    CONSTRAINT ck_price_list_scale CHECK (amount_scale BETWEEN 0 AND 6),
    CONSTRAINT ck_price_list_rounding CHECK (rounding_mode IN ('UP', 'DOWN', 'CEILING', 'FLOOR', 'HALF_UP', 'HALF_DOWN', 'HALF_EVEN')),
    CONSTRAINT ck_price_list_state CHECK (state IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_price_list_version CHECK (version >= 0)
);

CREATE TABLE price_entry (
    company_id UUID NOT NULL,
    price_list_id UUID NOT NULL,
    price_entry_id UUID NOT NULL,
    catalog_item_id UUID NOT NULL,
    unit_code VARCHAR(16) NOT NULL,
    minimum_quantity NUMERIC(38,18) NOT NULL,
    amount NUMERIC(38,6) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_price_entry PRIMARY KEY (company_id, price_list_id, price_entry_id),
    CONSTRAINT fk_price_entry_list FOREIGN KEY (company_id, price_list_id)
        REFERENCES price_list (company_id, price_list_id),
    CONSTRAINT fk_price_entry_item FOREIGN KEY (company_id, catalog_item_id)
        REFERENCES catalog_item (company_id, catalog_item_id),
    CONSTRAINT fk_price_entry_unit FOREIGN KEY (company_id, unit_code)
        REFERENCES unit_definition (company_id, unit_code),
    CONSTRAINT ck_price_entry_quantity CHECK (minimum_quantity > 0),
    CONSTRAINT ck_price_entry_amount CHECK (amount >= 0),
    CONSTRAINT ck_price_entry_validity CHECK (valid_until IS NULL OR valid_until > valid_from)
);

CREATE INDEX ix_price_entry_quote
    ON price_entry (company_id, price_list_id, catalog_item_id, unit_code, active, minimum_quantity, valid_from);

CREATE TABLE catalog_code_sequence (
    company_id UUID NOT NULL,
    sequence_scope VARCHAR(64) NOT NULL,
    next_value BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_catalog_code_sequence PRIMARY KEY (company_id, sequence_scope),
    CONSTRAINT ck_catalog_code_sequence_next CHECK (next_value > 0)
);

CREATE FUNCTION prevent_tax_profile_revision_overlap()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.active THEN
        PERFORM pg_advisory_xact_lock(hashtextextended(
            NEW.company_id::text || ':' || NEW.tax_profile_id::text, 0));
        IF EXISTS (
            SELECT 1 FROM plg_commercial_catalog.tax_profile_revision existing
            WHERE existing.company_id = NEW.company_id
              AND existing.tax_profile_id = NEW.tax_profile_id
              AND existing.profile_version <> NEW.profile_version
              AND existing.active
              AND tstzrange(existing.valid_from, COALESCE(existing.valid_until, 'infinity'::timestamptz), '[)')
                  && tstzrange(NEW.valid_from, COALESCE(NEW.valid_until, 'infinity'::timestamptz), '[)')
        ) THEN
            RAISE EXCEPTION 'Active tax profile revision overlaps the same profile'
                USING ERRCODE = '23505', CONSTRAINT = 'uq_tax_profile_revision_validity';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_tax_profile_revision_overlap
BEFORE INSERT OR UPDATE ON tax_profile_revision
FOR EACH ROW EXECUTE FUNCTION prevent_tax_profile_revision_overlap();

CREATE FUNCTION prevent_price_entry_overlap()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.active THEN
        PERFORM pg_advisory_xact_lock(hashtextextended(
            NEW.company_id::text || ':' || NEW.price_list_id::text || ':' ||
            NEW.catalog_item_id::text || ':' || NEW.unit_code || ':' || NEW.minimum_quantity::text, 0));
        IF EXISTS (
            SELECT 1 FROM plg_commercial_catalog.price_entry existing
            WHERE existing.company_id = NEW.company_id
              AND existing.price_list_id = NEW.price_list_id
              AND existing.catalog_item_id = NEW.catalog_item_id
              AND existing.unit_code = NEW.unit_code
              AND existing.minimum_quantity = NEW.minimum_quantity
              AND existing.price_entry_id <> NEW.price_entry_id
              AND existing.active
              AND tstzrange(existing.valid_from, COALESCE(existing.valid_until, 'infinity'::timestamptz), '[)')
                  && tstzrange(NEW.valid_from, COALESCE(NEW.valid_until, 'infinity'::timestamptz), '[)')
        ) THEN
            RAISE EXCEPTION 'Active price entry overlaps the same scope'
                USING ERRCODE = '23505', CONSTRAINT = 'uq_price_entry_validity';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_price_entry_overlap
BEFORE INSERT OR UPDATE ON price_entry
FOR EACH ROW EXECUTE FUNCTION prevent_price_entry_overlap();
