package br.edu.ufrb.rascomp.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
    void cadastroPublicoDeveSempreCriarParticipanteMesmoSeRoleForEnviada() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Conta Pública",
                                  "email": "cadastro.publico@rascomp.local",
                                  "senha": "Rascomp@2026",
                                  "role": "DEV"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.role").value("PARTICIPANTE"));
    }

    @Test
    @WithMockUser(username = "dev@rascomp.local", roles = "DEV")
    void devDeveCriarContaInternaComRoleExplicita() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios/internos")
                        .param("role", "GESTAO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Gestão Interna",
                                  "email": "gestao.interna@rascomp.local",
                                  "senha": "Rascomp@2026"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("GESTAO"));
    }

    @Test
    @WithMockUser(username = "dev@rascomp.local", roles = "DEV")
    void rotaInternaNaoDeveCriarParticipante() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios/internos")
                        .param("role", "PARTICIPANTE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Participante Interno",
                                  "email": "participante.interno@rascomp.local",
                                  "senha": "Rascomp@2026"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

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

        mockMvc.perform(post("/api/v1/usuarios/internos")
                        .param("role", "GESTAO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Gestão Indevida",
                                  "email": "gestao.nao.pode@rascomp.local",
                                  "senha": "Rascomp@2026"
                                }
                                """))
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
