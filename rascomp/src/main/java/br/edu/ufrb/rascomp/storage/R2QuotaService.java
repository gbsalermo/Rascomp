package br.edu.ufrb.rascomp.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * MySQL-backed quota ledger. Reservations are committed before the remote upload
 * so concurrent application instances cannot overrun a category quota.
 * An interrupted upload may leave a reservation, which fails closed until reconciled.
 */
@Service
@ConditionalOnProperty(prefix = "app.storage.r2", name = "enabled", havingValue = "true")
public class R2QuotaService {
    private final JdbcTemplate jdbc;

    public R2QuotaService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserve(String key, long bytes) {
        if (bytes <= 0 || bytes > 10L * 1024 * 1024) {
            throw new IllegalArgumentException("Tamanho de arquivo fora do limite permitido.");
        }
        String category = category(key);
        int updated = jdbc.update("""
            UPDATE r2_storage_quota
            SET used_bytes = used_bytes + ?
            WHERE category = ? AND used_bytes <= limit_bytes - ?
            """, bytes, category, bytes);
        if (updated != 1) {
            throw new IllegalStateException("Limite de armazenamento da categoria atingido. Exclua arquivos antes de enviar novos.");
        }
        jdbc.update("""
            INSERT INTO r2_stored_object (object_key, category, size_bytes, state)
            VALUES (?, ?, ?, 'RESERVED')
            """, key, category, bytes);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void confirm(String key) {
        jdbc.update("UPDATE r2_stored_object SET state = 'ACTIVE' WHERE object_key = ? AND state = 'RESERVED'", key);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String key) {
        var records = jdbc.query("""
            SELECT category, size_bytes, state FROM r2_stored_object
            WHERE object_key = ? FOR UPDATE
            """, (rs, i) -> new Entry(rs.getString(1), rs.getLong(2), rs.getString(3)), key);
        if (records.isEmpty()) return;
        Entry entry = records.get(0);
        if ("DELETED".equals(entry.state())) return;
        jdbc.update("UPDATE r2_stored_object SET state = 'DELETED' WHERE object_key = ?", key);
        jdbc.update("UPDATE r2_storage_quota SET used_bytes = used_bytes - ? WHERE category = ?",
                entry.size(), entry.category());
    }

    private String category(String key) {
        if (key.startsWith("competition/")) return "competition";
        if (key.startsWith("gallery/") || key.startsWith("landing/")) return "gallery";
        throw new IllegalArgumentException("Categoria de objeto R2 desconhecida.");
    }

    private record Entry(String category, long size, String state) {}
}
