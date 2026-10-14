package br.edu.ufrb.rascomp.media;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

/**
 * H2 em modo MySQL, aplicando V33 no ambiente de testes (Flyway desabilitado
 * nos flowtests existentes). Nunca usa armazenamento R2 real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("flowtest")
@Sql(scripts="classpath:db/migration/V33__media_cms_draft_publication.sql",
     executionPhase=Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class MediaCmsAuthorizationTest {
    @Autowired MockMvc mvc;

    @Test
    void publicacaoNaoDeveVazarRascunhoEOperacoesDevemSerRestritas() throws Exception {
        mvc.perform(get("/api/v1/media/workspace"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/media/workspace")
                .with(SecurityMockMvcRequestPostProcessors.user("gestao").roles("GESTAO")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/media/workspace")
                .with(SecurityMockMvcRequestPostProcessors.user("midia").roles("MIDIA")))
            .andExpect(status().isOk());

        mvc.perform(put("/api/v1/media/draft")
                .with(SecurityMockMvcRequestPostProcessors.user("midia").roles("MIDIA"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"heroes\":[{\"id\":\"preview-only\",\"title\":\"NAO_PUBLICADO\"}]}"))
            .andExpect(status().isOk());

        // GET público lê somente o snapshot publicado, não o rascunho.
        mvc.perform(get("/api/v1/public/media/editorial"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.heroes").doesNotExist());

        mvc.perform(post("/api/v1/media/publish")
                .with(SecurityMockMvcRequestPostProcessors.user("dev").roles("DEV"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":0}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(1));

        mvc.perform(get("/api/v1/public/media/editorial"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.heroes[0].title").value("NAO_PUBLICADO"));
        mvc.perform(get("/api/v1/public/media/gallery"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.photos.length()").value(0));

        mvc.perform(post("/api/v1/media/publish")
                .with(SecurityMockMvcRequestPostProcessors.user("dev").roles("DEV"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":0}"))
            .andExpect(status().isConflict());

        // Salvar um segundo rascunho da Landing sem publicá-lo.
        mvc.perform(put("/api/v1/media/draft")
                .with(SecurityMockMvcRequestPostProcessors.user("midia").roles("MIDIA"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"heroes\":[{\"id\":\"future\",\"title\":\"AINDA_RASCUNHO\"}]}"))
            .andExpect(status().isOk());

        // A publicação da Galeria NÃO pode divulgar alterações pendentes na Landing.
        mvc.perform(post("/api/v1/media/publish/gallery")
                .with(SecurityMockMvcRequestPostProcessors.user("gestao").roles("GESTAO"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":1}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/media/publish/gallery")
                .with(SecurityMockMvcRequestPostProcessors.user("midia").roles("MIDIA"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(2));
        mvc.perform(get("/api/v1/public/media/editorial"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.heroes[0].title").value("NAO_PUBLICADO"));
        mvc.perform(get("/api/v1/media/workspace")
                .with(SecurityMockMvcRequestPostProcessors.user("midia").roles("MIDIA")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.editorialDraft.heroes[0].title").value("AINDA_RASCUNHO"));
    }
}
