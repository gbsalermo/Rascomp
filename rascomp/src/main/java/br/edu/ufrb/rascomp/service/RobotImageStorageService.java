package br.edu.ufrb.rascomp.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RobotImageStorageService {

    private static final long MAX_BYTES = 5L * 1024L * 1024L;

    private final Path root;

    public RobotImageStorageService(
            @Value("${app.storage.robot-images-dir:./uploads/robots}") String directory) {
        this.root = Paths.get(directory).toAbsolutePath().normalize();
    }

    public String armazenar(Long robotId, MultipartFile arquivo) {
        String contentType = validar(arquivo);

        String extensao = switch (contentType) {
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

    public String detectarContentType(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("A imagem é obrigatória.");
        }

        try (InputStream input = arquivo.getInputStream()) {
            byte[] header = input.readNBytes(12);

            if (ehPng(header)) return "image/png";
            if (ehJpeg(header)) return "image/jpeg";
            if (ehWebp(header)) return "image/webp";
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível validar o conteúdo da imagem.", ex);
        }

        throw new IllegalArgumentException("Formato de imagem permitido: JPEG, PNG ou WEBP.");
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

    private String validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("A imagem é obrigatória.");
        }
        if (arquivo.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("A imagem deve possuir no máximo 5 MB.");
        }
        return detectarContentType(arquivo);
    }

    private boolean ehPng(byte[] header) {
        return header.length >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A;
    }

    private boolean ehJpeg(byte[] header) {
        return header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
    }

    private boolean ehWebp(byte[] header) {
        return header.length >= 12
                && header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P';
    }

    private Path resolverSeguro(String storageKey) {
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Caminho de arquivo inválido.");
        }
        return resolved;
    }
}
