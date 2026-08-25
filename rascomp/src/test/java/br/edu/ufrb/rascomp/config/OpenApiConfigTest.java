package br.edu.ufrb.rascomp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void deveConfigurarMetadadosJwtEErrosReutilizaveis() {
        OpenAPI openAPI = config.rascompOpenAPI();

        assertEquals("Rascomp API", openAPI.getInfo().getTitle());
        assertEquals("v1", openAPI.getInfo().getVersion());
        assertNotNull(openAPI.getComponents().getSecuritySchemes().get(OpenApiConfig.BEARER_AUTH));
        assertNotNull(openAPI.getComponents().getSchemas().get("ApiErrorResponse"));
        assertNotNull(openAPI.getComponents().getSchemas().get("SecurityErrorResponse"));
        assertNotNull(openAPI.getComponents().getResponses().get("Unauthorized"));
        assertNotNull(openAPI.getComponents().getResponses().get("Forbidden"));
        assertNotNull(openAPI.getComponents().getResponses().get("MethodNotAllowed"));
    }

    @Test
    void deveSepararEndpointPublicoDeEndpointProtegido() {
        Operation publica = operacao("competicoes");
        Operation protegida = operacao("listar");

        OpenAPI openAPI = config.rascompOpenAPI();
        openAPI.setPaths(new Paths()
                .addPathItem("/api/v1/public/competicoes", new PathItem().get(publica))
                .addPathItem("/api/v1/competicoes", new PathItem().get(protegida)));

        config.rascompOpenApiCustomizer().customise(openAPI);

        assertNull(publica.getSecurity());
        assertNotNull(protegida.getSecurity());
        assertTrue(protegida.getSecurity().stream()
                .anyMatch(item -> item.containsKey(OpenApiConfig.BEARER_AUTH)));
        assertEquals("API Pública", publica.getTags().getFirst());
        assertEquals("Competições", protegida.getTags().getFirst());
        assertNotNull(protegida.getResponses().get("401"));
        assertNotNull(protegida.getResponses().get("403"));
    }

    @Test
    void deveDocumentarCreatedParaPostENoContentParaDelete() {
        Operation criar = operacao("criar");
        Operation deletar = operacao("deletar");

        OpenAPI openAPI = config.rascompOpenAPI();
        openAPI.setPaths(new Paths()
                .addPathItem("/api/v1/competicoes", new PathItem().post(criar))
                .addPathItem("/api/v1/competicoes/{id}", new PathItem().delete(deletar)));

        config.rascompOpenApiCustomizer().customise(openAPI);

        assertNotNull(criar.getResponses().get("201"));
        assertNull(criar.getResponses().get("200"));
        assertNotNull(deletar.getResponses().get("204"));
        assertNull(deletar.getResponses().get("200"));
    }

    private Operation operacao(String id) {
        return new Operation()
                .operationId(id)
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse().description("OK")));
    }
}
