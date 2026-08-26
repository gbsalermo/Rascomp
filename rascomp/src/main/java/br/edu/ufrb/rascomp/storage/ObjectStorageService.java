package br.edu.ufrb.rascomp.storage;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

public interface ObjectStorageService {

    void upload(String objectKey, byte[] content, String contentType);

    void delete(String objectKey);

    boolean exists(String objectKey);

    Optional<URI> publicUrl(String objectKey);

    URI presignedUploadUrl(String objectKey, String contentType, Duration validity);
}
