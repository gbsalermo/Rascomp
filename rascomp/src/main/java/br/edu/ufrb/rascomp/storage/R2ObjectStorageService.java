package br.edu.ufrb.rascomp.storage;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import br.edu.ufrb.rascomp.config.R2StorageProperties;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage.r2", name = "enabled", havingValue = "true")
public class R2ObjectStorageService implements ObjectStorageService {

    private static final Pattern SAFE_OBJECT_KEY = Pattern.compile("[A-Za-z0-9._/-]+");
    private static final Duration MAX_PRESIGNED_VALIDITY = Duration.ofHours(1);

    private final S3Client r2S3Client;
    private final S3Presigner r2S3Presigner;
    private final R2StorageProperties properties;

    @Override
    public void upload(String objectKey, byte[] content, String contentType) {
        String key = validateObjectKey(objectKey);
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("O conteúdo do arquivo é obrigatório.");
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .contentType(requireContentType(contentType))
                .contentLength((long) content.length)
                .build();

        try {
            r2S3Client.putObject(request, RequestBody.fromBytes(content));
        } catch (S3Exception ex) {
            throw storageFailure("Não foi possível enviar o arquivo para o Cloudflare R2.", ex);
        }
    }

    @Override
    public void delete(String objectKey) {
        String key = validateObjectKey(objectKey);
        try {
            r2S3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(key)
                    .build());
        } catch (S3Exception ex) {
            throw storageFailure("Não foi possível remover o arquivo do Cloudflare R2.", ex);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        String key = validateObjectKey(objectKey);
        try {
            r2S3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(key)
                    .build());
            return true;
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                return false;
            }
            throw storageFailure("Não foi possível consultar o arquivo no Cloudflare R2.", ex);
        }
    }

    @Override
    public Optional<URI> publicUrl(String objectKey) {
        String key = validateObjectKey(objectKey);
        String baseUrl = properties.publicBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return Optional.empty();
        }

        String normalizedBase = baseUrl.trim().replaceAll("/+$", "");
        return Optional.of(URI.create(normalizedBase + "/" + key));
    }

    @Override
    public URI presignedUploadUrl(String objectKey, String contentType, Duration validity) {
        String key = validateObjectKey(objectKey);
        Duration effectiveValidity = validateValidity(validity);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .contentType(requireContentType(contentType))
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(effectiveValidity)
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presigned = r2S3Presigner.presignPutObject(presignRequest);
        return presigned.url().toURI();
    }

    private String validateObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("A chave do arquivo é obrigatória.");
        }

        String key = objectKey.trim().replace('\\', '/');
        if (key.startsWith("/") || key.endsWith("/") || key.contains("..")
                || !SAFE_OBJECT_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Chave de arquivo inválida para o armazenamento.");
        }
        return key;
    }

    private String requireContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("O content type do arquivo é obrigatório.");
        }
        return contentType.trim();
    }

    private Duration validateValidity(Duration validity) {
        if (validity == null || validity.isZero() || validity.isNegative()) {
            throw new IllegalArgumentException("A validade da URL pré-assinada deve ser positiva.");
        }
        if (validity.compareTo(MAX_PRESIGNED_VALIDITY) > 0) {
            throw new IllegalArgumentException("A URL pré-assinada pode valer por no máximo 1 hora.");
        }
        return validity;
    }

    private IllegalStateException storageFailure(String message, S3Exception cause) {
        String detail = cause.awsErrorDetails() != null
                ? cause.awsErrorDetails().errorMessage()
                : cause.getMessage();
        return new IllegalStateException(message + " Detalhe: " + detail, cause);
    }
}
