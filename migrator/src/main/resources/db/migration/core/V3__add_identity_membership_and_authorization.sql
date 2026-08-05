CREATE TABLE core.app_user (
    app_user_id UUID NOT NULL,
    issuer VARCHAR(2048) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    display_name VARCHAR(160),
    status VARCHAR(16) NOT NULL DEFAULT 'INACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT app_user_pk PRIMARY KEY (app_user_id),
    CONSTRAINT app_user_external_identity_uk UNIQUE (issuer, subject),
    CONSTRAINT app_user_issuer_ck CHECK (
        char_length(issuer) BETWEEN 1 AND 2048
        AND issuer = btrim(issuer)
        AND issuer ~ '^https?://'
        AND issuer !~ '[[:space:][:cntrl:]]'
        AND position('?' IN issuer) = 0
        AND position('#' IN issuer) = 0
    ),
    CONSTRAINT app_user_subject_ck CHECK (
        char_length(subject) BETWEEN 1 AND 255
        AND subject = btrim(subject)
        AND subject !~ '[[:cntrl:]]'
    ),
    CONSTRAINT app_user_display_name_ck CHECK (
        display_name IS NULL OR (
            char_length(display_name) BETWEEN 1 AND 160
            AND display_name = btrim(display_name)
            AND display_name !~ '[[:cntrl:]]'
        )
    ),
    CONSTRAINT app_user_status_ck CHECK (status IN ('INACTIVE', 'ACTIVE')),
    CONSTRAINT app_user_version_ck CHECK (version >= 0),
    CONSTRAINT app_user_timestamps_ck CHECK (updated_at >= created_at)
);

CREATE TABLE core.company_membership (
    app_user_id UUID NOT NULL,
    company_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'INACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT company_membership_pk PRIMARY KEY (app_user_id, company_id),
    CONSTRAINT company_membership_user_fk FOREIGN KEY (app_user_id)
        REFERENCES core.app_user (app_user_id) ON DELETE RESTRICT,
    CONSTRAINT company_membership_company_fk FOREIGN KEY (company_id)
        REFERENCES core.company (company_id) ON DELETE RESTRICT,
    CONSTRAINT company_membership_status_ck CHECK (status IN ('INACTIVE', 'ACTIVE')),
    CONSTRAINT company_membership_version_ck CHECK (version >= 0),
    CONSTRAINT company_membership_timestamps_ck CHECK (updated_at >= created_at)
);

CREATE TABLE core.security_role (
    role_id UUID NOT NULL,
    company_id UUID NOT NULL,
    role_code VARCHAR(128) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'INACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT security_role_pk PRIMARY KEY (role_id),
    CONSTRAINT security_role_company_role_id_uk UNIQUE (company_id, role_id),
    CONSTRAINT security_role_company_code_uk UNIQUE (company_id, role_code),
    CONSTRAINT security_role_company_fk FOREIGN KEY (company_id)
        REFERENCES core.company (company_id) ON DELETE RESTRICT,
    CONSTRAINT security_role_code_ck CHECK (
        char_length(role_code) BETWEEN 1 AND 128
        AND role_code ~ '^[a-z][a-z0-9_]*([.:][a-z][a-z0-9_]*)*$'
    ),
    CONSTRAINT security_role_display_name_ck CHECK (
        char_length(display_name) BETWEEN 1 AND 160
        AND display_name = btrim(display_name)
        AND display_name !~ '[[:cntrl:]]'
    ),
    CONSTRAINT security_role_status_ck CHECK (status IN ('INACTIVE', 'ACTIVE')),
    CONSTRAINT security_role_version_ck CHECK (version >= 0),
    CONSTRAINT security_role_timestamps_ck CHECK (updated_at >= created_at)
);

CREATE TABLE core.role_permission (
    company_id UUID NOT NULL,
    role_id UUID NOT NULL,
    permission_id VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT role_permission_pk PRIMARY KEY (company_id, role_id, permission_id),
    CONSTRAINT role_permission_role_fk FOREIGN KEY (company_id, role_id)
        REFERENCES core.security_role (company_id, role_id) ON DELETE RESTRICT,
    CONSTRAINT role_permission_id_ck CHECK (
        char_length(permission_id) BETWEEN 1 AND 128
        AND permission_id ~ '^[a-z][a-z0-9_]*([.:][a-z][a-z0-9_]*)*$'
    )
);

CREATE TABLE core.membership_role (
    app_user_id UUID NOT NULL,
    company_id UUID NOT NULL,
    role_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT membership_role_pk PRIMARY KEY (app_user_id, company_id, role_id),
    CONSTRAINT membership_role_membership_fk FOREIGN KEY (app_user_id, company_id)
        REFERENCES core.company_membership (app_user_id, company_id) ON DELETE RESTRICT,
    CONSTRAINT membership_role_role_fk FOREIGN KEY (company_id, role_id)
        REFERENCES core.security_role (company_id, role_id) ON DELETE RESTRICT
);

CREATE INDEX company_membership_active_user_idx
    ON core.company_membership (app_user_id, company_id)
    WHERE status = 'ACTIVE';

CREATE INDEX company_membership_company_idx
    ON core.company_membership (company_id, app_user_id);

CREATE INDEX security_role_active_company_idx
    ON core.security_role (company_id, role_code, role_id)
    WHERE status = 'ACTIVE';

CREATE INDEX role_permission_permission_idx
    ON core.role_permission (permission_id, company_id, role_id);

CREATE INDEX membership_role_role_idx
    ON core.membership_role (company_id, role_id, app_user_id);
