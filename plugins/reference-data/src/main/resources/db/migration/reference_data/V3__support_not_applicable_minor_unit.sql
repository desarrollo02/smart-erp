ALTER TABLE currency_entry
    ALTER COLUMN minor_unit DROP NOT NULL;

ALTER TABLE currency_entry
    DROP CONSTRAINT ck_currency_entry_minor;

ALTER TABLE currency_entry
    ADD CONSTRAINT ck_currency_entry_minor
        CHECK (minor_unit IS NULL OR minor_unit BETWEEN 0 AND 9);
