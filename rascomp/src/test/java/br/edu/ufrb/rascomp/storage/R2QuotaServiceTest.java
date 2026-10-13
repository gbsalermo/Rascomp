package br.edu.ufrb.rascomp.storage;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.mock;

class R2QuotaServiceTest {
    private JdbcTemplate jdbc;
    private R2QuotaService quota;

    @BeforeEach
    void setup() {
        jdbc = mock(JdbcTemplate.class);
        quota = new R2QuotaService(jdbc);
    }

    @Test
    void rejectsUnknownObjectPrefix() {
        assertThrows(IllegalArgumentException.class, () -> quota.reserve("receipts/a.pdf", 120));
    }

    @Test
    void rejectsInvalidSizes() {
        assertThrows(IllegalArgumentException.class, () -> quota.reserve("competition/a.pdf", 0));
        assertThrows(IllegalArgumentException.class,
                () -> quota.reserve("competition/a.pdf", 10L * 1024 * 1024 + 1));
    }

    @Test
    void rejectsWhenAtomicReservationDoesNotFit() {
        when(jdbc.update(any(String.class), eq(100L), eq("competition"), eq(100L)))
                .thenReturn(0);
        assertThrows(IllegalStateException.class,
                () -> quota.reserve("competition/a.pdf", 100));
    }

    @Test
    void reservesCompetitionCapacityBeforeUpload() {
        when(jdbc.update(any(String.class), eq(200L), eq("competition"), eq(200L)))
                .thenReturn(1);
        assertDoesNotThrow(() -> quota.reserve("competition/robots/x.webp", 200));
        verify(jdbc).update(any(String.class), eq("competition/robots/x.webp"),
                eq("competition"), eq(200L));
    }

    @Test
    void reservesLandingUnderGalleryQuota() {
        when(jdbc.update(any(String.class), eq(300L), eq("gallery"), eq(300L)))
                .thenReturn(1);
        assertDoesNotThrow(() -> quota.reserve("landing/banner.webp", 300));
        verify(jdbc).update(any(String.class), eq("landing/banner.webp"), eq("gallery"), eq(300L));
    }
}
