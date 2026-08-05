CREATE TABLE catalog_release (
    catalog_kind VARCHAR(16) NOT NULL,
    release_id VARCHAR(64) NOT NULL,
    standard_id VARCHAR(64) NOT NULL,
    authority VARCHAR(160) NOT NULL,
    source_uri VARCHAR(500) NOT NULL,
    source_sha256 CHAR(64) NOT NULL,
    observed_on DATE NOT NULL,
    completeness VARCHAR(24) NOT NULL,
    entry_count INTEGER NOT NULL,
    current_release BOOLEAN NOT NULL,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_catalog_release PRIMARY KEY (catalog_kind, release_id),
    CONSTRAINT ck_catalog_release_kind CHECK (catalog_kind IN ('COUNTRY', 'CURRENCY')),
    CONSTRAINT ck_catalog_release_sha256 CHECK (source_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_catalog_release_completeness
        CHECK (completeness IN ('BOOTSTRAP_SUBSET', 'FULL')),
    CONSTRAINT ck_catalog_release_entry_count CHECK (entry_count > 0)
);

CREATE UNIQUE INDEX uq_catalog_release_current
    ON catalog_release (catalog_kind) WHERE current_release;

CREATE TABLE country_entry (
    catalog_kind VARCHAR(16) NOT NULL DEFAULT 'COUNTRY',
    release_id VARCHAR(64) NOT NULL,
    alpha2_code CHAR(2) NOT NULL,
    alpha3_code CHAR(3) NOT NULL,
    numeric_code CHAR(3) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    CONSTRAINT pk_country_entry PRIMARY KEY (catalog_kind, release_id, alpha2_code),
    CONSTRAINT fk_country_entry_release FOREIGN KEY (catalog_kind, release_id)
        REFERENCES catalog_release (catalog_kind, release_id),
    CONSTRAINT ck_country_entry_kind CHECK (catalog_kind = 'COUNTRY'),
    CONSTRAINT ck_country_entry_alpha2 CHECK (alpha2_code ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_country_entry_alpha3 CHECK (alpha3_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_country_entry_numeric CHECK (numeric_code ~ '^[0-9]{3}$'),
    CONSTRAINT uq_country_entry_alpha3 UNIQUE (catalog_kind, release_id, alpha3_code),
    CONSTRAINT uq_country_entry_numeric UNIQUE (catalog_kind, release_id, numeric_code)
);

CREATE TABLE currency_entry (
    catalog_kind VARCHAR(16) NOT NULL DEFAULT 'CURRENCY',
    release_id VARCHAR(64) NOT NULL,
    alphabetic_code CHAR(3) NOT NULL,
    numeric_code CHAR(3) NOT NULL,
    minor_unit INTEGER NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    CONSTRAINT pk_currency_entry PRIMARY KEY (catalog_kind, release_id, alphabetic_code),
    CONSTRAINT fk_currency_entry_release FOREIGN KEY (catalog_kind, release_id)
        REFERENCES catalog_release (catalog_kind, release_id),
    CONSTRAINT ck_currency_entry_kind CHECK (catalog_kind = 'CURRENCY'),
    CONSTRAINT ck_currency_entry_alpha CHECK (alphabetic_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_currency_entry_numeric CHECK (numeric_code ~ '^[0-9]{3}$'),
    CONSTRAINT ck_currency_entry_minor CHECK (minor_unit BETWEEN 0 AND 9),
    CONSTRAINT uq_currency_entry_numeric UNIQUE (catalog_kind, release_id, numeric_code)
);

CREATE TABLE company_country_policy (
    company_id UUID NOT NULL,
    alpha2_code CHAR(2) NOT NULL,
    enabled BOOLEAN NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_company_country_policy PRIMARY KEY (company_id, alpha2_code),
    CONSTRAINT ck_company_country_policy_code CHECK (alpha2_code ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_company_country_policy_version CHECK (version >= 0)
);

CREATE TABLE company_currency_policy (
    company_id UUID NOT NULL,
    alphabetic_code CHAR(3) NOT NULL,
    enabled BOOLEAN NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_company_currency_policy PRIMARY KEY (company_id, alphabetic_code),
    CONSTRAINT ck_company_currency_policy_code CHECK (alphabetic_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_company_currency_policy_version CHECK (version >= 0)
);

INSERT INTO catalog_release (
    catalog_kind, release_id, standard_id, authority, source_uri, source_sha256,
    observed_on, completeness, entry_count, current_release)
VALUES
    ('COUNTRY', 'un-m49-2026-08-04-bootstrap', 'ISO 3166-1:2020 / UN M49',
     'United Nations Statistics Division',
     'https://unstats.un.org/unsd/methodology/m49/overview/',
     '748f6ff7380c8a50ea9448f068b79e3a1ee31be63207249e8cc89bf1eb969d11',
     DATE '2026-08-04', 'BOOTSTRAP_SUBSET', 1, TRUE),
    ('CURRENCY', 'six-list-one-2026-08-04-bootstrap', 'ISO 4217:2015',
     'SIX Financial Information AG',
     'https://www.six-group.com/dam/download/financial-information/data-center/iso-currrency/lists/list-one.xml',
     '838dfb991648cf36df939edd5fe3811737962b75a32252847d239cedd1e291c9',
     DATE '2026-08-04', 'BOOTSTRAP_SUBSET', 2, TRUE);

INSERT INTO country_entry (
    catalog_kind, release_id, alpha2_code, alpha3_code, numeric_code, display_name)
VALUES ('COUNTRY', 'un-m49-2026-08-04-bootstrap', 'PY', 'PRY', '600', 'Paraguay');

INSERT INTO currency_entry (
    catalog_kind, release_id, alphabetic_code, numeric_code, minor_unit, display_name)
VALUES
    ('CURRENCY', 'six-list-one-2026-08-04-bootstrap', 'PYG', '600', 0, 'Guarani'),
    ('CURRENCY', 'six-list-one-2026-08-04-bootstrap', 'USD', '840', 2, 'US Dollar');
