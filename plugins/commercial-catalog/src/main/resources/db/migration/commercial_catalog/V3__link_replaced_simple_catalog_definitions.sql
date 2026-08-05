ALTER TABLE unit_definition
    ADD COLUMN replacement_unit_code VARCHAR(16),
    ADD CONSTRAINT fk_unit_definition_replacement
        FOREIGN KEY (company_id, replacement_unit_code)
        REFERENCES unit_definition (company_id, unit_code),
    ADD CONSTRAINT ck_unit_definition_replacement_self
        CHECK (replacement_unit_code IS NULL OR replacement_unit_code <> unit_code),
    ADD CONSTRAINT ck_unit_definition_replacement_inactive
        CHECK (replacement_unit_code IS NULL OR state = 'INACTIVE');

CREATE INDEX ix_unit_definition_replacement
    ON unit_definition (company_id, replacement_unit_code)
    WHERE replacement_unit_code IS NOT NULL;

ALTER TABLE category_definition
    ADD COLUMN replacement_category_id UUID,
    ADD CONSTRAINT fk_category_definition_replacement
        FOREIGN KEY (company_id, replacement_category_id)
        REFERENCES category_definition (company_id, category_id),
    ADD CONSTRAINT ck_category_definition_replacement_self
        CHECK (replacement_category_id IS NULL OR replacement_category_id <> category_id),
    ADD CONSTRAINT ck_category_definition_replacement_inactive
        CHECK (replacement_category_id IS NULL OR state = 'INACTIVE');

CREATE INDEX ix_category_definition_replacement
    ON category_definition (company_id, replacement_category_id)
    WHERE replacement_category_id IS NOT NULL;

ALTER TABLE brand_definition
    ADD COLUMN replacement_brand_id UUID,
    ADD CONSTRAINT fk_brand_definition_replacement
        FOREIGN KEY (company_id, replacement_brand_id)
        REFERENCES brand_definition (company_id, brand_id),
    ADD CONSTRAINT ck_brand_definition_replacement_self
        CHECK (replacement_brand_id IS NULL OR replacement_brand_id <> brand_id),
    ADD CONSTRAINT ck_brand_definition_replacement_inactive
        CHECK (replacement_brand_id IS NULL OR state = 'INACTIVE');

CREATE INDEX ix_brand_definition_replacement
    ON brand_definition (company_id, replacement_brand_id)
    WHERE replacement_brand_id IS NOT NULL;

ALTER TABLE tag_definition
    ADD COLUMN replacement_tag_id UUID,
    ADD CONSTRAINT fk_tag_definition_replacement
        FOREIGN KEY (company_id, replacement_tag_id)
        REFERENCES tag_definition (company_id, tag_id),
    ADD CONSTRAINT ck_tag_definition_replacement_self
        CHECK (replacement_tag_id IS NULL OR replacement_tag_id <> tag_id),
    ADD CONSTRAINT ck_tag_definition_replacement_inactive
        CHECK (replacement_tag_id IS NULL OR state = 'INACTIVE');

CREATE INDEX ix_tag_definition_replacement
    ON tag_definition (company_id, replacement_tag_id)
    WHERE replacement_tag_id IS NOT NULL;
