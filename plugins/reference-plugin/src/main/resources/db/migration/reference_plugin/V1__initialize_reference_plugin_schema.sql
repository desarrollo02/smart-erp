CREATE TABLE migration_fixture (
    fixture_key VARCHAR(64) PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE migration_fixture IS
    'Technical fixture proving plugin-owned migrations; not an ERP domain table';

