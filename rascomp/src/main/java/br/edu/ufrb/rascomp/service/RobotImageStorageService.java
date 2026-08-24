package br.edu.ufrb.rascomp.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RobotImageStorageService {

    private static final long MAX_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");

    private final Path root;

    public RobotImageStorageService(
            @Value("${app.storage.robot-images-dir:./uploads/robots}") String directory) {
        this.root = Paths.get(directory).toAbsolutePath().normalize();
    }

    public String armazenar(Long robotId, MultipartFile arquivo) {
        validar(arquivo);

        String extensao = switch (arquivo.getContentType()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };

        String storageKey = robotId + "/" + UUID.randomUUID() + extensao;
        Path destino = resolverSeguro(storageKey);

        try {
            Files.createDirectories(destino.getParent());
            Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            return storageKey;
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível armazenar a imagem do robô.", ex);
        }
    }

    public Resource carregar(String storageKey) {
        Path arquivo = resolverSeguro(storageKey);
        try {
            Resource resource = new UrlResource(arquivo.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalStateException("Arquivo de imagem não encontrado no armazenamento.");
            }
            return resource;
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível ler a imagem do robô.", ex);
        }
    }

    public void remover(String storageKey) {
        try {
            Files.deleteIfExists(resolverSeguro(storageKey));
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível remover a imagem do armazenamento.", ex);
        }
    }

    private void validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("A imagem é obrigatória.");
        }
        if (arquivo.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("A imagem deve possuir no máximo 5 MB.");
        }
        if (arquivo.getContentType() == null || !CONTENT_TYPES.contains(arquivo.getContentType())) {
            throw new IllegalArgumentException("Formato de imagem permitido: JPEG, PNG ou WEBP.");
        }
    }

    private Path resolverSeguro(String storageKey) {
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Caminho de arquivo inválido.");
        }
        return resolved;
    }
}
