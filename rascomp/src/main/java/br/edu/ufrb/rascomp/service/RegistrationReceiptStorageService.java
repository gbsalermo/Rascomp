package br.edu.ufrb.rascomp.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RegistrationReceiptStorageService {

    private static final long MAX_BYTES = 10L * 1024L * 1024L;

    private final Path root;

    public RegistrationReceiptStorageService(
            @Value("${app.storage.registration-receipts-dir:./uploads/registration-receipts}") String directory) {
        root = Paths.get(directory).toAbsolutePath().normalize();
    }

    public StoredReceipt armazenar(
            String tipo,
            Long competitionId,
            Long ownerId,
            MultipartFile arquivo) {

        String contentType = detectarContentType(arquivo);
        String extensao = switch (contentType) {
            case "application/pdf" -> ".pdf";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };

        String namespace = tipo == null
                ? "geral"
                : tipo.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        String storageKey = namespace + "/" + competitionId + "/" + ownerId + "/"
                + UUID.randomUUID() + extensao;
        Path destino = resolverSeguro(storageKey);

        try {
            Files.createDirectories(destino.getParent());
            Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            return new StoredReceipt(
                    storageKey,
                    nomeSeguro(arquivo.getOriginalFilename()),
                    contentType);
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível armazenar o comprovante da inscrição.", ex);
        }
    }

    public String detectarContentType(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("O comprovante de pagamento é obrigatório.");
        }
        if (arquivo.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("O comprovante deve possuir no máximo 10 MB.");
        }

        try (InputStream input = arquivo.getInputStream()) {
            byte[] header = input.readNBytes(12);
            if (ehPdf(header)) return "application/pdf";
            if (ehPng(header)) return "image/png";
            if (ehJpeg(header)) return "image/jpeg";
            if (ehWebp(header)) return "image/webp";
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível validar o comprovante.", ex);
        }

        throw new IllegalArgumentException(
                "Formato de comprovante permitido: PDF, JPEG, PNG ou WEBP.");
    }

    public ReceiptFile carregar(String storageKey, String contentType, String filename) {
        Path arquivo = resolverSeguro(storageKey);
        try {
            Resource resource = new UrlResource(arquivo.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalStateException("Comprovante não encontrado no armazenamento.");
            }
            return new ReceiptFile(resource, contentType, filename);
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível ler o comprovante.", ex);
        }
    }

    public void remover(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) return;
        try {
            Files.deleteIfExists(resolverSeguro(storageKey));
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível remover o comprovante.", ex);
        }
    }

    private boolean ehPdf(byte[] header) {
        return header.length >= 5
                && header[0] == '%'
                && header[1] == 'P'
                && header[2] == 'D'
                && header[3] == 'F'
                && header[4] == '-';
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

    private String nomeSeguro(String original) {
        if (original == null || original.isBlank()) return "comprovante";
        String nome = original.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1);
        return nome.length() <= 255 ? nome : nome.substring(nome.length() - 255);
    }

    private Path resolverSeguro(String storageKey) {
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Caminho de comprovante inválido.");
        }
        return resolved;
    }

    public record StoredReceipt(String storageKey, String originalFilename, String contentType) {}
    public record ReceiptFile(Resource resource, String contentType, String filename) {}
}
