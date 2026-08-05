CREATE TABLE core.system_metadata (
    property_key VARCHAR(100) PRIMARY KEY,
    property_value VARCHAR(500) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO core.system_metadata (property_key, property_value)
VALUES ('schema_owner', 'core');
