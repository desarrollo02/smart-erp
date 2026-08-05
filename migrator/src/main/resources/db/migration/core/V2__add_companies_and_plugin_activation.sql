CREATE TABLE core.company (
    company_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    customization_plugin_id VARCHAR(59) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT company_pk PRIMARY KEY (company_id),
    CONSTRAINT company_customization_plugin_id_uk UNIQUE (customization_plugin_id),
    CONSTRAINT company_status_ck CHECK (status IN ('INACTIVE', 'ACTIVE')),
    CONSTRAINT company_customization_plugin_id_format_ck CHECK (
        customization_plugin_id ~ '^[a-z][a-z0-9]*(_[a-z0-9]+)*$'
        AND char_length(customization_plugin_id) <= 59
    ),
    CONSTRAINT company_version_ck CHECK (version >= 0),
    CONSTRAINT company_timestamps_ck CHECK (updated_at >= created_at)
);

CREATE TABLE core.company_plugin_activation (
    company_id UUID NOT NULL,
    plugin_id VARCHAR(59) NOT NULL,
    desired_state VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT company_plugin_activation_pk PRIMARY KEY (company_id, plugin_id),
    CONSTRAINT company_plugin_activation_company_fk FOREIGN KEY (company_id)
        REFERENCES core.company (company_id) ON DELETE RESTRICT,
    CONSTRAINT company_plugin_activation_plugin_id_format_ck CHECK (
        plugin_id ~ '^[a-z][a-z0-9]*(_[a-z0-9]+)*$'
        AND char_length(plugin_id) <= 59
    ),
    CONSTRAINT company_plugin_activation_state_ck CHECK (
        desired_state IN ('DISABLED', 'ENABLED')
    ),
    CONSTRAINT company_plugin_activation_version_ck CHECK (version >= 0),
    CONSTRAINT company_plugin_activation_timestamps_ck CHECK (updated_at >= created_at)
);

CREATE INDEX company_plugin_activation_enabled_idx
    ON core.company_plugin_activation (company_id, plugin_id)
    WHERE desired_state = 'ENABLED';
