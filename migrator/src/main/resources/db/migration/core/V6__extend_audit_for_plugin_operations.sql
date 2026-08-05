ALTER TABLE core.audit_event
    ADD COLUMN resource_type VARCHAR(96),
    ADD COLUMN resource_id VARCHAR(160);

ALTER TABLE core.audit_event
    DROP CONSTRAINT audit_event_category_ck,
    ADD CONSTRAINT audit_event_category_ck CHECK (category IN (
        'COMPANY_OPERATION',
        'SECURITY_OPERATION',
        'TRUSTED_ACCESS',
        'SYSTEM_AUTHORITY_OPERATION',
        'SYSTEM_AUTHORITY_ACCESS',
        'PLUGIN_OPERATION'
    )),
    ADD CONSTRAINT audit_event_resource_ck CHECK (
        (resource_type IS NULL AND resource_id IS NULL)
        OR (
            resource_type IS NOT NULL
            AND char_length(resource_type) BETWEEN 1 AND 96
            AND resource_type = btrim(resource_type)
            AND resource_type !~ '[[:cntrl:]]'
            AND (resource_id IS NULL OR (
                char_length(resource_id) BETWEEN 1 AND 160
                AND resource_id = btrim(resource_id)
                AND resource_id !~ '[[:cntrl:]]'
            ))
        )
    );

CREATE INDEX audit_event_resource_idx
    ON core.audit_event (resource_type, resource_id, occurred_at DESC)
    WHERE resource_type IS NOT NULL;
