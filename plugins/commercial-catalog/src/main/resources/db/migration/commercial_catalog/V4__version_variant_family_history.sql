SET search_path TO plg_commercial_catalog, public;

CREATE TABLE variant_family_revision (
    company_id UUID NOT NULL,
    variant_family_id UUID NOT NULL,
    family_version BIGINT NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    state VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_variant_family_revision
        PRIMARY KEY (company_id, variant_family_id, family_version),
    CONSTRAINT fk_variant_family_revision_owner
        FOREIGN KEY (company_id, variant_family_id)
        REFERENCES variant_family (company_id, variant_family_id),
    CONSTRAINT ck_variant_family_revision_version CHECK (family_version >= 0),
    CONSTRAINT ck_variant_family_revision_state CHECK (state IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE variant_attribute_revision (
    company_id UUID NOT NULL,
    variant_family_id UUID NOT NULL,
    family_version BIGINT NOT NULL,
    attribute_code VARCHAR(32) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    value_type VARCHAR(16) NOT NULL,
    required BOOLEAN NOT NULL,
    position INTEGER NOT NULL,
    CONSTRAINT pk_variant_attribute_revision
        PRIMARY KEY (company_id, variant_family_id, family_version, attribute_code),
    CONSTRAINT uq_variant_attribute_revision_type
        UNIQUE (company_id, variant_family_id, family_version, attribute_code, value_type),
    CONSTRAINT uq_variant_attribute_revision_position
        UNIQUE (company_id, variant_family_id, family_version, position),
    CONSTRAINT fk_variant_attribute_revision_owner
        FOREIGN KEY (company_id, variant_family_id, family_version)
        REFERENCES variant_family_revision (company_id, variant_family_id, family_version),
    CONSTRAINT ck_variant_attribute_revision_type
        CHECK (value_type IN ('TEXT', 'NUMBER', 'BOOLEAN')),
    CONSTRAINT ck_variant_attribute_revision_position CHECK (position >= 0)
);

INSERT INTO variant_family_revision (
    company_id, variant_family_id, family_version, display_name, state)
SELECT company_id, variant_family_id, version, display_name, state
FROM variant_family;

INSERT INTO variant_attribute_revision (
    company_id, variant_family_id, family_version, attribute_code,
    display_name, value_type, required, position)
SELECT attribute.company_id, attribute.variant_family_id, family.version,
       attribute.attribute_code, attribute.display_name, attribute.value_type,
       attribute.required, attribute.position
FROM variant_attribute_definition attribute
JOIN variant_family family
  ON family.company_id = attribute.company_id
 AND family.variant_family_id = attribute.variant_family_id;

ALTER TABLE catalog_item_variant
    ADD COLUMN variant_family_version BIGINT;

UPDATE catalog_item_variant assignment
SET variant_family_version = family.version
FROM variant_family family
WHERE family.company_id = assignment.company_id
  AND family.variant_family_id = assignment.variant_family_id;

ALTER TABLE catalog_item_variant
    ALTER COLUMN variant_family_version SET NOT NULL,
    ADD CONSTRAINT ck_catalog_item_variant_family_version
        CHECK (variant_family_version >= 0),
    ADD CONSTRAINT uq_catalog_item_variant_family_revision
        UNIQUE (company_id, catalog_item_id, variant_family_id, variant_family_version),
    ADD CONSTRAINT fk_catalog_item_variant_family_revision
        FOREIGN KEY (company_id, variant_family_id, variant_family_version)
        REFERENCES variant_family_revision (company_id, variant_family_id, family_version);

ALTER TABLE catalog_item_variant_attribute
    ADD COLUMN variant_family_version BIGINT;

UPDATE catalog_item_variant_attribute attribute
SET variant_family_version = assignment.variant_family_version
FROM catalog_item_variant assignment
WHERE assignment.company_id = attribute.company_id
  AND assignment.catalog_item_id = attribute.catalog_item_id
  AND assignment.variant_family_id = attribute.variant_family_id;

ALTER TABLE catalog_item_variant_attribute
    DROP CONSTRAINT fk_catalog_item_variant_attribute_owner,
    DROP CONSTRAINT fk_catalog_item_variant_attribute_definition,
    ALTER COLUMN variant_family_version SET NOT NULL,
    ADD CONSTRAINT ck_catalog_item_variant_attribute_family_version
        CHECK (variant_family_version >= 0),
    ADD CONSTRAINT fk_catalog_item_variant_attribute_owner
        FOREIGN KEY (
            company_id, catalog_item_id, variant_family_id, variant_family_version)
        REFERENCES catalog_item_variant (
            company_id, catalog_item_id, variant_family_id, variant_family_version),
    ADD CONSTRAINT fk_catalog_item_variant_attribute_revision
        FOREIGN KEY (
            company_id, variant_family_id, variant_family_version,
            attribute_code, value_type)
        REFERENCES variant_attribute_revision (
            company_id, variant_family_id, family_version,
            attribute_code, value_type);
