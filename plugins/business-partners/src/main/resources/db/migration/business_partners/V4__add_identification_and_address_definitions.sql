ALTER TABLE business_partner_definition
    DROP CONSTRAINT ck_business_partner_definition_kind;

ALTER TABLE business_partner_definition
    ADD CONSTRAINT ck_business_partner_definition_kind
        CHECK (definition_kind IN (
            'CHANNEL_KIND',
            'IDENTIFICATION_TYPE',
            'ADDRESS_TYPE',
            'ADDRESS_PURPOSE'));

ALTER TABLE business_partner_definition_revision
    DROP CONSTRAINT ck_business_partner_definition_revision_kind;

ALTER TABLE business_partner_definition_revision
    ADD CONSTRAINT ck_business_partner_definition_revision_kind
        CHECK (definition_kind IN (
            'CHANNEL_KIND',
            'IDENTIFICATION_TYPE',
            'ADDRESS_TYPE',
            'ADDRESS_PURPOSE'));

INSERT INTO business_partner_definition
    (company_id, definition_kind, code, display_name, state)
SELECT DISTINCT
    identification.company_id,
    'IDENTIFICATION_TYPE',
    identification.type_code,
    initcap(replace(identification.type_code, '_', ' ')),
    'ACTIVE'
FROM business_partner_identification identification
ON CONFLICT (company_id, definition_kind, code) DO NOTHING;

INSERT INTO business_partner_definition
    (company_id, definition_kind, code, display_name, state)
SELECT DISTINCT
    address.company_id,
    'ADDRESS_TYPE',
    address.type_code,
    initcap(replace(address.type_code, '_', ' ')),
    'ACTIVE'
FROM business_partner_address address
ON CONFLICT (company_id, definition_kind, code) DO NOTHING;

INSERT INTO business_partner_definition
    (company_id, definition_kind, code, display_name, state)
SELECT DISTINCT
    address.company_id,
    'ADDRESS_PURPOSE',
    address.purpose_code,
    initcap(replace(address.purpose_code, '_', ' ')),
    'ACTIVE'
FROM business_partner_address address
ON CONFLICT (company_id, definition_kind, code) DO NOTHING;

INSERT INTO business_partner_definition
    (company_id, definition_kind, code, display_name, state)
SELECT DISTINCT partner.company_id, seed.definition_kind, seed.code, seed.display_name, 'ACTIVE'
FROM business_partner partner
CROSS JOIN (VALUES
    ('IDENTIFICATION_TYPE', 'national_id', 'Documento nacional'),
    ('IDENTIFICATION_TYPE', 'tax_id', 'Identificación tributaria'),
    ('IDENTIFICATION_TYPE', 'passport', 'Pasaporte'),
    ('ADDRESS_TYPE', 'physical', 'Física'),
    ('ADDRESS_TYPE', 'postal', 'Postal'),
    ('ADDRESS_PURPOSE', 'general', 'General'),
    ('ADDRESS_PURPOSE', 'billing', 'Facturación'),
    ('ADDRESS_PURPOSE', 'shipping', 'Entrega')
) AS seed(definition_kind, code, display_name)
ON CONFLICT (company_id, definition_kind, code) DO NOTHING;

INSERT INTO business_partner_definition_revision
    (company_id, definition_kind, code, version, display_name, state, changed_at)
SELECT company_id, definition_kind, code, version, display_name, state, updated_at
FROM business_partner_definition
WHERE definition_kind IN ('IDENTIFICATION_TYPE', 'ADDRESS_TYPE', 'ADDRESS_PURPOSE')
ON CONFLICT (company_id, definition_kind, code, version) DO NOTHING;
