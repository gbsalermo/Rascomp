package br.edu.ufrb.rascomp.config;

import java.net.URI;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@ConditionalOnProperty(prefix = "app.storage.r2", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(R2StorageProperties.class)
public class R2StorageConfiguration {

    @Bean(destroyMethod = "close")
    S3Client r2S3Client(R2StorageProperties properties) {
        properties.validateEnabledConfiguration();

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.accessKeyId().trim(),
                properties.secretAccessKey().trim());

        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false)
                .build();

        return S3Client.builder()
                .endpointOverride(URI.create(properties.resolvedEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .serviceConfiguration(serviceConfiguration)
                .build();
    }

    @Bean(destroyMethod = "close")
    S3Presigner r2S3Presigner(R2StorageProperties properties) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.accessKeyId().trim(),
                properties.secretAccessKey().trim());

        return S3Presigner.builder()
                .endpointOverride(URI.create(properties.resolvedEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }
}
