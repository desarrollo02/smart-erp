\set ON_ERROR_STOP on

BEGIN;

INSERT INTO plg_commercial_catalog.unit_definition
    (company_id, unit_code, display_name, decimal_scale, state, version)
VALUES
    (:'company_id'::uuid, 'EA', 'Unidad', 0, 'ACTIVE', 0),
    (:'company_id'::uuid, 'BOX', 'Caja', 0, 'ACTIVE', 0)
ON CONFLICT (company_id, unit_code) DO NOTHING;

INSERT INTO plg_commercial_catalog.category_definition
    (company_id, category_id, parent_category_id, code, display_name, state, version)
VALUES
    (:'company_id'::uuid, '00000000-0000-0000-0000-00000000c001'::uuid,
     NULL, 'DEMO', U&'Categor\00EDa de demostraci\00F3n', 'ACTIVE', 0)
ON CONFLICT (company_id, category_id) DO UPDATE
SET display_name = EXCLUDED.display_name
WHERE plg_commercial_catalog.category_definition.display_name
      IS DISTINCT FROM EXCLUDED.display_name;

INSERT INTO plg_commercial_catalog.brand_definition
    (company_id, brand_id, code, display_name, state, version)
VALUES
    (:'company_id'::uuid, '00000000-0000-0000-0000-00000000b001'::uuid,
     'DEMO', U&'Marca de demostraci\00F3n', 'ACTIVE', 0)
ON CONFLICT (company_id, brand_id) DO UPDATE
SET display_name = EXCLUDED.display_name
WHERE plg_commercial_catalog.brand_definition.display_name
      IS DISTINCT FROM EXCLUDED.display_name;

INSERT INTO plg_commercial_catalog.tax_profile
    (company_id, tax_profile_id, code, display_name, state, version)
VALUES
    (:'company_id'::uuid, '00000000-0000-0000-0000-00000000a001'::uuid,
     'IVA_GENERAL_DEMO', U&'IVA general de demostraci\00F3n', 'ACTIVE', 0),
    (:'company_id'::uuid, '00000000-0000-0000-0000-00000000a002'::uuid,
     'IVA_REDUCIDO_DEMO', U&'IVA reducido de demostraci\00F3n', 'ACTIVE', 0),
    (:'company_id'::uuid, '00000000-0000-0000-0000-00000000a003'::uuid,
     'EXENTO_DEMO', U&'Exento de demostraci\00F3n', 'ACTIVE', 0)
ON CONFLICT (company_id, tax_profile_id) DO UPDATE
SET code = EXCLUDED.code,
    display_name = EXCLUDED.display_name
WHERE plg_commercial_catalog.tax_profile.code IS DISTINCT FROM EXCLUDED.code
   OR plg_commercial_catalog.tax_profile.display_name
      IS DISTINCT FROM EXCLUDED.display_name;

INSERT INTO plg_commercial_catalog.tax_profile_revision
    (company_id, tax_profile_id, profile_version, internal_kind_code,
     description, valid_from, valid_until, active)
VALUES
    (:'company_id'::uuid, '00000000-0000-0000-0000-00000000a001'::uuid,
     0, 'TAXED_STANDARD',
     'Perfil general ficticio; no representa una tasa o regla SIFEN certificada',
     '2026-01-01T00:00:00Z'::timestamptz, NULL, TRUE),
    (:'company_id'::uuid, '00000000-0000-0000-0000-00000000a002'::uuid,
     0, 'TAXED_REDUCED',
     'Perfil reducido ficticio; no representa una tasa o regla SIFEN certificada',
     '2026-01-01T00:00:00Z'::timestamptz, NULL, TRUE),
    (:'company_id'::uuid, '00000000-0000-0000-0000-00000000a003'::uuid,
     0, 'EXEMPT',
     'Perfil exento ficticio; su uso real requiere validacion fiscal',
     '2026-01-01T00:00:00Z'::timestamptz, NULL, TRUE)
ON CONFLICT (company_id, tax_profile_id, profile_version) DO NOTHING;

COMMIT;

SELECT
    :'company_id' AS company_id,
    (SELECT count(*) FROM plg_commercial_catalog.unit_definition
       WHERE company_id = :'company_id'::uuid AND state = 'ACTIVE') AS active_units,
    (SELECT count(*) FROM plg_commercial_catalog.category_definition
       WHERE company_id = :'company_id'::uuid AND state = 'ACTIVE') AS active_categories,
    (SELECT count(*) FROM plg_commercial_catalog.brand_definition
       WHERE company_id = :'company_id'::uuid AND state = 'ACTIVE') AS active_brands,
    (SELECT count(*) FROM plg_commercial_catalog.tax_profile_revision
       WHERE company_id = :'company_id'::uuid AND active) AS active_tax_profiles;
