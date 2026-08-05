CREATE TABLE core.system_role (
    system_role_id UUID NOT NULL,
    role_code VARCHAR(128) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'INACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT system_role_pk PRIMARY KEY (system_role_id),
    CONSTRAINT system_role_code_uk UNIQUE (role_code),
    CONSTRAINT system_role_code_ck CHECK (
        char_length(role_code) BETWEEN 1 AND 128
        AND role_code ~ '^[a-z][a-z0-9_]*([.:][a-z][a-z0-9_]*)*$'
    ),
    CONSTRAINT system_role_display_name_ck CHECK (
        char_length(display_name) BETWEEN 1 AND 160
        AND display_name = btrim(display_name)
        AND display_name !~ '[[:cntrl:]]'
    ),
    CONSTRAINT system_role_status_ck CHECK (status IN ('INACTIVE', 'ACTIVE')),
    CONSTRAINT system_role_version_ck CHECK (version >= 0),
    CONSTRAINT system_role_timestamps_ck CHECK (updated_at >= created_at)
);

CREATE TABLE core.system_role_permission (
    system_role_id UUID NOT NULL,
    permission_id VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT system_role_permission_pk PRIMARY KEY (system_role_id, permission_id),
    CONSTRAINT system_role_permission_role_fk FOREIGN KEY (system_role_id)
        REFERENCES core.system_role (system_role_id) ON DELETE RESTRICT,
    CONSTRAINT system_role_permission_id_ck CHECK (
        char_length(permission_id) BETWEEN 1 AND 128
        AND permission_id ~ '^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*){2,}$'
    )
);

CREATE TABLE core.app_user_system_role (
    app_user_id UUID NOT NULL,
    system_role_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT app_user_system_role_pk PRIMARY KEY (app_user_id, system_role_id),
    CONSTRAINT app_user_system_role_user_fk FOREIGN KEY (app_user_id)
        REFERENCES core.app_user (app_user_id) ON DELETE RESTRICT,
    CONSTRAINT app_user_system_role_role_fk FOREIGN KEY (system_role_id)
        REFERENCES core.system_role (system_role_id) ON DELETE RESTRICT
);

CREATE INDEX system_role_active_code_idx
    ON core.system_role (role_code, system_role_id)
    WHERE status = 'ACTIVE';

CREATE INDEX system_role_permission_permission_idx
    ON core.system_role_permission (permission_id, system_role_id);

CREATE INDEX app_user_system_role_role_idx
    ON core.app_user_system_role (system_role_id, app_user_id);
