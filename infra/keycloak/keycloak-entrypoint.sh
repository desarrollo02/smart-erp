#!/bin/bash

set -euo pipefail

read_secret() {
    local secret_file="$1"
    local variable_name="$2"

    if [[ -z "$secret_file" || ! -f "$secret_file" ]]; then
        echo "$variable_name must reference a regular file" >&2
        return 78
    fi

    local secret_size
    secret_size=$(wc -c < "$secret_file")
    if (( secret_size < 1 || secret_size > 4096 )); then
        echo "$variable_name has an invalid size" >&2
        return 78
    fi

    local secret_value
    secret_value=$(tr -d '\r' < "$secret_file")
    if [[ -z "$secret_value" || "$secret_value" == *$'\n'* ]]; then
        echo "$variable_name must contain exactly one non-empty line" >&2
        return 78
    fi

    printf '%s' "$secret_value"
}

KC_BOOTSTRAP_ADMIN_PASSWORD=$(read_secret \
    "${KC_BOOTSTRAP_ADMIN_PASSWORD_FILE:-}" \
    "KC_BOOTSTRAP_ADMIN_PASSWORD_FILE")
LOGIXONE_OIDC_CLIENT_SECRET=$(read_secret \
    "${LOGIXONE_OIDC_CLIENT_SECRET_FILE:-}" \
    "LOGIXONE_OIDC_CLIENT_SECRET_FILE")
LOGIXONE_DEMO_USER_PASSWORD=$(read_secret \
    "${LOGIXONE_DEMO_USER_PASSWORD_FILE:-}" \
    "LOGIXONE_DEMO_USER_PASSWORD_FILE")

export KC_BOOTSTRAP_ADMIN_PASSWORD
export LOGIXONE_OIDC_CLIENT_SECRET
export LOGIXONE_DEMO_USER_PASSWORD

exec /opt/keycloak/bin/kc.sh "$@"
