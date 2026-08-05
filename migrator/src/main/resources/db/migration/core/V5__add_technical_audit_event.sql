CREATE TABLE core.audit_event (
    audit_event_id UUID NOT NULL,
    category VARCHAR(40) NOT NULL,
    operation VARCHAR(96) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    actor_kind VARCHAR(32) NOT NULL,
    actor_user_id UUID,
    subject_user_id UUID,
    company_id UUID,
    role_id UUID,
    system_role_id UUID,
    plugin_id VARCHAR(128),
    permission_id VARCHAR(160),
    screen_id VARCHAR(260),
    result_code VARCHAR(128),
    previous_version BIGINT,
    resulting_version BIGINT,
    correlation_id VARCHAR(128),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT audit_event_pk PRIMARY KEY (audit_event_id),
    CONSTRAINT audit_event_category_ck CHECK (category IN (
        'COMPANY_OPERATION',
        'SECURITY_OPERATION',
        'TRUSTED_ACCESS',
        'SYSTEM_AUTHORITY_OPERATION',
        'SYSTEM_AUTHORITY_ACCESS'
    )),
    CONSTRAINT audit_event_outcome_ck CHECK (outcome IN (
        'CHANGED', 'UNCHANGED', 'REJECTED', 'ALLOWED', 'DENIED',
        'SELECTION_REQUIRED'
    )),
    CONSTRAINT audit_event_actor_kind_ck CHECK (actor_kind IN (
        'SYSTEM', 'AUTHENTICATED_USER', 'TEST', 'UNRESOLVED'
    )),
    CONSTRAINT audit_event_actor_ck CHECK (
        actor_kind <> 'AUTHENTICATED_USER' OR actor_user_id IS NOT NULL
    ),
    CONSTRAINT audit_event_operation_ck CHECK (
        char_length(operation) BETWEEN 1 AND 96
        AND operation = btrim(operation)
        AND operation !~ '[[:cntrl:]]'
    ),
    CONSTRAINT audit_event_optional_ids_ck CHECK (
        (plugin_id IS NULL OR (
            char_length(plugin_id) BETWEEN 1 AND 128
            AND plugin_id = btrim(plugin_id)
            AND plugin_id !~ '[[:cntrl:]]'
        ))
        AND (permission_id IS NULL OR (
            char_length(permission_id) BETWEEN 1 AND 160
            AND permission_id = btrim(permission_id)
            AND permission_id !~ '[[:cntrl:]]'
        ))
        AND (screen_id IS NULL OR (
            char_length(screen_id) BETWEEN 1 AND 260
            AND screen_id = btrim(screen_id)
            AND screen_id !~ '[[:cntrl:]]'
        ))
        AND (result_code IS NULL OR (
            char_length(result_code) BETWEEN 1 AND 128
            AND result_code = btrim(result_code)
            AND result_code !~ '[[:cntrl:]]'
        ))
    ),
    CONSTRAINT audit_event_versions_ck CHECK (
        (previous_version IS NULL OR previous_version >= 0)
        AND (resulting_version IS NULL OR resulting_version >= 0)
    ),
    CONSTRAINT audit_event_correlation_ck CHECK (
        correlation_id IS NULL
        OR correlation_id ~ '^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$'
    )
);

CREATE INDEX audit_event_occurred_idx
    ON core.audit_event (occurred_at DESC, audit_event_id DESC);

CREATE INDEX audit_event_category_outcome_idx
    ON core.audit_event (category, outcome, occurred_at DESC);

CREATE INDEX audit_event_company_idx
    ON core.audit_event (company_id, occurred_at DESC)
    WHERE company_id IS NOT NULL;

CREATE INDEX audit_event_actor_idx
    ON core.audit_event (actor_user_id, occurred_at DESC)
    WHERE actor_user_id IS NOT NULL;

CREATE INDEX audit_event_correlation_idx
    ON core.audit_event (correlation_id, occurred_at DESC)
    WHERE correlation_id IS NOT NULL;

CREATE FUNCTION core.reject_audit_event_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'core.audit_event is append-only';
END;
$$;

CREATE TRIGGER audit_event_no_update_or_delete
BEFORE UPDATE OR DELETE ON core.audit_event
FOR EACH ROW EXECUTE FUNCTION core.reject_audit_event_mutation();
