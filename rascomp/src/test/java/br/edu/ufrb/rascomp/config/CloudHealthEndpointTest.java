package br.edu.ufrb.rascomp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "management.endpoint.health.probes.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("flowtest")
class CloudHealthEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void endpointsDeHealthPublicosNaoPrecisamDeLogin() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void perfilCloudNaoDeveExigirMysqlNoHealthCheck() throws Exception {
        Properties properties = new Properties();
        try (InputStream resource = getClass().getResourceAsStream("/application-cloud.properties")) {
            assertNotNull(resource);
            properties.load(resource);
        }

        assertEquals("true", properties.getProperty("management.endpoint.health.probes.enabled"));
        assertEquals("false", properties.getProperty("management.health.db.enabled"));
    }
}
