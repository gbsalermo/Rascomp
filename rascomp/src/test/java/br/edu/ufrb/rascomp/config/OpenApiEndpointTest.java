package br.edu.ufrb.rascomp.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.edu.ufrb.rascomp.teste.DataInitializer;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:swagger;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",
        "camunda.bpm.enabled=false",
        "app.security.jwt.secret=rascomp-swagger-test-secret-with-at-least-32-bytes"
})
@AutoConfigureMockMvc
class OpenApiEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataInitializer dataInitializer;

    @Test
    void deveExporOpenApiCompletaSemAutenticacao() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Rascomp API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists());
    }

    @Test
    void deveExporGrupoPublicoSemAutenticacao() throws Exception {
        mockMvc.perform(get("/v3/api-docs/publica"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/public/competicoes']").exists());
    }

    @Test
    void deveExporGrupoParticipanteComAuthEPortal() throws Exception {
        mockMvc.perform(get("/v3/api-docs/participante"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/participante/equipes']").exists());
    }

    @Test
    void deveDocumentarMatchResultSomenteComoGet() throws Exception {
        mockMvc.perform(get("/v3/api-docs/completa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/resultados-partida'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/resultados-partida'].post").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/resultados-partida'].put").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/resultados-partida'].delete").doesNotExist());
    }
}
