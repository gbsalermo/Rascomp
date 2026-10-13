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

  # A private truststore alone replaces Java's standard public CAs, which
  # breaks TLS for Cloudflare R2 and other HTTPS services. Merge the Aiven CA
  # entries into a private copy of the JVM's default cacerts instead.
  default_cacerts="$(dirname "$(readlink -f "$(command -v java)")")/../lib/security/cacerts"
  merged_store="$store_dir/merged-cacerts.p12"
  cp "$default_cacerts" "$merged_store"
  chmod 600 "$merged_store"

  if ! keytool -importkeystore -noprompt \
    -srckeystore "$store_path" -srcstoretype PKCS12 -srcstorepass "$TRUSTSTORE_PASSWORD" \
    -destkeystore "$merged_store" -deststorepass changeit 1>/dev/null; then
    echo "Failed to merge the Aiven certificate into Java's default truststore." >&2
    exit 1
  fi

  # JAVA_OPTS is intentionally shell-split into JVM arguments, as in the original Dockerfile.
  # Do not store secrets in JAVA_OPTS or commit them to the repository.
  # shellcheck disable=SC2086
  exec java ${JAVA_OPTS:-} \
    "-Djavax.net.ssl.trustStore=$merged_store" \
    "-Djavax.net.ssl.trustStorePassword=changeit" \
    -jar /app/app.jar
fi

# Existing local Docker flow remains supported (including mounted truststores).
# shellcheck disable=SC2086
exec java ${JAVA_OPTS:-} -jar /app/app.jar
