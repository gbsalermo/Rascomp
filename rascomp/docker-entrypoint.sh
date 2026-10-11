#!/bin/sh
set -eu

# Render: a truststore PKCS12 is provided through protected environment variables.
# Base64 is a transport encoding, not encryption. Never print these values.
if [ -n "${TRUSTSTORE_BASE64:-}" ]; then
  if [ -z "${TRUSTSTORE_PASSWORD:-}" ]; then
    echo "TRUSTSTORE_PASSWORD is required when TRUSTSTORE_BASE64 is set." >&2
    exit 1
  fi

  store_dir="$(mktemp -d /tmp/rascomp-tls.XXXXXX)"
  chmod 700 "$store_dir"
  store_path="$store_dir/truststore.p12"
  if ! printf '%s' "$TRUSTSTORE_BASE64" | base64 -d > "$store_path"; then
    echo "Invalid TRUSTSTORE_BASE64 content." >&2
    exit 1
  fi
  chmod 600 "$store_path"
  unset TRUSTSTORE_BASE64

  # JAVA_OPTS is intentionally shell-split into JVM arguments, as in the original Dockerfile.
  # Do not store secrets in JAVA_OPTS or commit them to the repository.
  # shellcheck disable=SC2086
  exec java ${JAVA_OPTS:-} \
    "-Djavax.net.ssl.trustStore=$store_path" \
    "-Djavax.net.ssl.trustStorePassword=$TRUSTSTORE_PASSWORD" \
    "-Djavax.net.ssl.trustStoreType=PKCS12" \
    -jar /app/app.jar
fi

# Existing local Docker flow remains supported (including mounted truststores).
# shellcheck disable=SC2086
exec java ${JAVA_OPTS:-} -jar /app/app.jar
