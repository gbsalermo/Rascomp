package br.edu.ufrb.rascomp.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("flowtest")
class SecurityAuthorizationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "dev@rascomp.local", roles = "DEV")
    void devDeveAcessarOperacaoCompetitivaEAdministracaoDeUsuarios() throws Exception {
        mockMvc.perform(get("/api/v1/competicoes"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/usuarios").param("role", "DEV"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "gestao@rascomp.local", roles = "GESTAO")
    void gestaoDeveOperarCompeticaoSemAdministrarUsuarios() throws Exception {
        mockMvc.perform(get("/api/v1/competicoes"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/usuarios").param("role", "DEV"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "midia@rascomp.local", roles = "MIDIA")
    void midiaNaoDeveHerdarOperacaoCompetitivaNemAdministracaoDeUsuarios() throws Exception {
        mockMvc.perform(get("/api/v1/competicoes"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/usuarios").param("role", "MIDIA"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "participante@rascomp.local", roles = "PARTICIPANTE")
    void participanteNaoDeveAcessarNamespacesAdministrativos() throws Exception {
        mockMvc.perform(get("/api/v1/competicoes"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/usuarios").param("role", "PARTICIPANTE"))
                .andExpect(status().isForbidden());
    }
}
