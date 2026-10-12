package br.edu.ufrb.rascomp.storage;

import java.io.IOException;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Backend-only gateway for persistent competition files. Keeps the bucket private:
 * callers still apply their existing authorization rules before invoking read().
 *
 * Never accept object keys or storage namespaces directly from HTTP requests.
 */
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "app.storage.r2", name = "enabled", havingValue = "true")
public class R2ManagedFileStorage {
    private final R2ObjectStorageService objects;
    private final S3Client client;
    private final String bucket;

    public R2ManagedFileStorage(R2ObjectStorageService objects, S3Client client,
            @Value("${app.storage.r2.bucket}") String bucket) {
        this.objects = objects;
        this.client = client;
        this.bucket = bucket;
    }

    public void upload(String key, byte[] bytes, String contentType) {
        objects.upload(key, bytes, contentType);
    }

    public Resource read(String key) {
        try {
            ResponseBytes<?> bytes = client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(key).build());
            return new ByteArrayResource(bytes.asByteArray());
        } catch (NoSuchKeyException ex) {
            throw new IllegalStateException("Arquivo não encontrado no R2.", ex);
        }
    }

    public void delete(String key) {
        if (key != null && !key.isBlank()) {
            objects.delete(key);
        }
    }
}
