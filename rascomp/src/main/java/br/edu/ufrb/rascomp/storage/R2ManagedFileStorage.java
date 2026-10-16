package br.edu.ufrb.rascomp.storage;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

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
    private final R2QuotaService quota;

    public R2ManagedFileStorage(R2ObjectStorageService objects, S3Client client,
            R2QuotaService quota, @Value("${app.storage.r2.bucket}") String bucket) {
        this.quota = quota;
        this.objects = objects;
        this.client = client;
        this.bucket = bucket;
    }

    public void upload(String key, byte[] bytes, String contentType) {
        quota.reserve(key, bytes.length);
        try {
            objects.upload(key, bytes, contentType);
            quota.confirm(key);
        } catch (RuntimeException ex) {
            // A failed request can still have written its object remotely; only
            // release capacity if remote cleanup succeeds.
            try {
                objects.delete(key);
                quota.release(key);
            } catch (RuntimeException cleanup) {
                ex.addSuppressed(cleanup);
            }
            throw ex;
        }
    }

    public Resource read(String key) {
        try {
            // Spring ResourceHttpMessageConverter envia InputStreamResource em fluxo.
            // Evita carregar o objeto inteiro na heap de uma instância pequena.
            return new InputStreamResource(client.getObject(
                    GetObjectRequest.builder().bucket(bucket).key(key).build()));
        } catch (NoSuchKeyException ex) {
            throw new IllegalStateException("Arquivo não encontrado no R2.", ex);
        }
    }

    public void delete(String key) {
        if (key != null && !key.isBlank()) {
            objects.delete(key);
            quota.release(key);
        }
    }
}
