CREATE TABLE unit_definition_revision (
    company_id UUID NOT NULL,
    unit_code VARCHAR(16) NOT NULL,
    definition_version BIGINT NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    decimal_scale INTEGER NOT NULL,
    state VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_unit_definition_revision
        PRIMARY KEY (company_id, unit_code, definition_version),
    CONSTRAINT fk_unit_definition_revision_owner
        FOREIGN KEY (company_id, unit_code)
        REFERENCES unit_definition (company_id, unit_code),
    CONSTRAINT ck_unit_definition_revision_version CHECK (definition_version >= 0),
    CONSTRAINT ck_unit_definition_revision_scale CHECK (decimal_scale BETWEEN 0 AND 12),
    CONSTRAINT ck_unit_definition_revision_state CHECK (state IN ('ACTIVE', 'INACTIVE'))
);

INSERT INTO unit_definition_revision (
    company_id, unit_code, definition_version, display_name, decimal_scale, state, created_at)
SELECT company_id, unit_code, version, display_name, decimal_scale, state, updated_at
FROM unit_definition;

CREATE TABLE category_definition_revision (
    company_id UUID NOT NULL,
    category_id UUID NOT NULL,
    definition_version BIGINT NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    parent_category_id UUID,
    state VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_category_definition_revision
        PRIMARY KEY (company_id, category_id, definition_version),
    CONSTRAINT fk_category_definition_revision_owner
        FOREIGN KEY (company_id, category_id)
        REFERENCES category_definition (company_id, category_id),
    CONSTRAINT fk_category_definition_revision_parent
        FOREIGN KEY (company_id, parent_category_id)
        REFERENCES category_definition (company_id, category_id),
    CONSTRAINT ck_category_definition_revision_version CHECK (definition_version >= 0),
    CONSTRAINT ck_category_definition_revision_self
        CHECK (parent_category_id IS NULL OR parent_category_id <> category_id),
    CONSTRAINT ck_category_definition_revision_state CHECK (state IN ('ACTIVE', 'INACTIVE'))
);

INSERT INTO category_definition_revision (
    company_id, category_id, definition_version, display_name,
    parent_category_id, state, created_at)
SELECT company_id, category_id, version, display_name,
       parent_category_id, state, updated_at
FROM category_definition;

CREATE TABLE brand_definition_revision (
    company_id UUID NOT NULL,
    brand_id UUID NOT NULL,
    definition_version BIGINT NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    state VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_brand_definition_revision
        PRIMARY KEY (company_id, brand_id, definition_version),
    CONSTRAINT fk_brand_definition_revision_owner
        FOREIGN KEY (company_id, brand_id)
        REFERENCES brand_definition (company_id, brand_id),
    CONSTRAINT ck_brand_definition_revision_version CHECK (definition_version >= 0),
    CONSTRAINT ck_brand_definition_revision_state CHECK (state IN ('ACTIVE', 'INACTIVE'))
);

INSERT INTO brand_definition_revision (
    company_id, brand_id, definition_version, display_name, state, created_at)
SELECT company_id, brand_id, version, display_name, state, updated_at
FROM brand_definition;

CREATE TABLE tag_definition_revision (
    company_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    definition_version BIGINT NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    state VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_tag_definition_revision
        PRIMARY KEY (company_id, tag_id, definition_version),
    CONSTRAINT fk_tag_definition_revision_owner
        FOREIGN KEY (company_id, tag_id)
        REFERENCES tag_definition (company_id, tag_id),
    CONSTRAINT ck_tag_definition_revision_version CHECK (definition_version >= 0),
    CONSTRAINT ck_tag_definition_revision_state CHECK (state IN ('ACTIVE', 'INACTIVE'))
);

INSERT INTO tag_definition_revision (
    company_id, tag_id, definition_version, display_name, state, created_at)
SELECT company_id, tag_id, version, display_name, state, updated_at
FROM tag_definition;
