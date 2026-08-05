#!/usr/bin/env sh

set -eu

read_secret() {
    secret_file="$1"
    variable_name="$2"
    if [ -z "$secret_file" ] || [ ! -f "$secret_file" ]; then
        echo "$variable_name must reference a regular file" >&2
        return 78
    fi

    secret_size=$(wc -c < "$secret_file")
    if [ "$secret_size" -lt 1 ] || [ "$secret_size" -gt 4096 ]; then
        echo "$variable_name has an invalid size" >&2
        return 78
    fi

    secret_value=$(tr -d '\r' < "$secret_file")
    case "$secret_value" in
        ""|*"
"*)
            echo "$variable_name must contain exactly one non-empty line" >&2
            return 78
            ;;
    esac
    printf '%s' "$secret_value"
}

require_value() {
    variable_name="$1"
    variable_value=$(printenv "$variable_name" || true)
    if [ -z "$variable_value" ]; then
        echo "$variable_name must be configured" >&2
        return 78
    fi
}

require_value LOGIXONE_OIDC_PROVIDER_URL
require_value LOGIXONE_OIDC_CLIENT_ID
require_value LOGIXONE_OIDC_POST_LOGOUT_REDIRECT_URI

case "$LOGIXONE_OIDC_PROVIDER_URL" in
    http://*|https://*) ;;
    *)
        echo "LOGIXONE_OIDC_PROVIDER_URL must be an absolute HTTP(S) URL" >&2
        exit 78
        ;;
esac

case "$LOGIXONE_OIDC_POST_LOGOUT_REDIRECT_URI" in
    http://*|https://*) ;;
    *)
        echo "LOGIXONE_OIDC_POST_LOGOUT_REDIRECT_URI must be an absolute HTTP(S) URL" >&2
        exit 78
        ;;
esac

LOGIXONE_DB_PASSWORD=$(read_secret \
    "${LOGIXONE_DB_PASSWORD_FILE:-}" \
    "LOGIXONE_DB_PASSWORD_FILE")
LOGIXONE_OIDC_CLIENT_SECRET=$(read_secret \
    "${LOGIXONE_OIDC_CLIENT_SECRET_FILE:-}" \
    "LOGIXONE_OIDC_CLIENT_SECRET_FILE")

export LOGIXONE_DB_PASSWORD
export LOGIXONE_OIDC_CLIENT_SECRET
exec /__cacert_entrypoint.sh "$@"
