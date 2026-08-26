package br.edu.ufrb.rascomp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.r2")
public record R2StorageProperties(
        boolean enabled,
        String accountId,
        String endpoint,
        String accessKeyId,
        String secretAccessKey,
        String bucket,
        String publicBaseUrl) {

    public String resolvedEndpoint() {
        if (endpoint != null && !endpoint.isBlank()) {
            return endpoint.trim();
        }
        if (accountId != null && !accountId.isBlank()) {
            return "https://" + accountId.trim() + ".r2.cloudflarestorage.com";
        }
        throw new IllegalStateException(
                "R2 habilitado, mas R2_ENDPOINT ou R2_ACCOUNT_ID não foi configurado.");
    }

    public void validateEnabledConfiguration() {
        resolvedEndpoint();
        requireValue(accessKeyId, "R2_ACCESS_KEY_ID");
        requireValue(secretAccessKey, "R2_SECRET_ACCESS_KEY");
        requireValue(bucket, "R2_BUCKET");
    }

    private static void requireValue(String value, String environmentVariable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "R2 habilitado, mas " + environmentVariable + " não foi configurado.");
        }
    }
}
