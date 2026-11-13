package br.edu.ufrb.rascomp.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufrb.rascomp.repository.UserAccountRepository;
import br.edu.ufrb.rascomp.security.JwtAuthenticationFilter;
import br.edu.ufrb.rascomp.security.JwtService;

@WebMvcTest(controllers = SecurityConfigAuthorizationTest.SecurityProbeController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, SecurityConfigAuthorizationTest.SecurityProbeController.class })
class SecurityConfigAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @RestController
    static class SecurityProbeController {

        @GetMapping("/api/v1/competicoes/security-probe")
        String competition() {
            return "ok";
        }

        @GetMapping("/api/v1/usuarios/security-probe")
        String users() {
            return "ok";
        }

        @GetMapping("/api/v1/participante/security-probe")
        String participant() {
            return "ok";
        }
    }

    @Test
    @WithMockUser(roles = "DEV")
    void devDeveAcessarOperacaoCompetitiva() throws Exception {
        mockMvc.perform(get("/api/v1/competicoes/security-probe"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GESTAO")
    void gestaoDeveAcessarOperacaoCompetitiva() throws Exception {
        mockMvc.perform(get("/api/v1/competicoes/security-probe"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MIDIA")
    void midiaNaoDeveAcessarOperacaoCompetitiva() throws Exception {
        mockMvc.perform(get("/api/v1/competicoes/security-probe"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PARTICIPANTE")
    void participanteNaoDeveAcessarOperacaoCompetitiva() throws Exception {
        mockMvc.perform(get("/api/v1/competicoes/security-probe"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DEV")
    void devDeveAdministrarUsuarios() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/security-probe"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GESTAO")
    void gestaoNaoDeveAdministrarUsuarios() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/security-probe"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MIDIA")
    void midiaNaoDeveAdministrarUsuarios() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/security-probe"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PARTICIPANTE")
    void participanteDeveAcessarNamespaceProprio() throws Exception {
        mockMvc.perform(get("/api/v1/participante/security-probe"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DEV")
    void devNaoDeveHerdarNamespaceDoParticipante() throws Exception {
        mockMvc.perform(get("/api/v1/participante/security-probe"))
                .andExpect(status().isForbidden());
    }
}
