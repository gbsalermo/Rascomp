package br.edu.ufrb.rascomp.config;

import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI rascompOpenAPI() {
        Components components = new Components()
                .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .name(BEARER_AUTH)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT retornado por POST /api/v1/auth/login ou POST /api/v1/auth/register."))
                .addSchemas("ApiErrorResponse", apiErrorSchema())
                .addSchemas("SecurityErrorResponse", securityErrorSchema());

        components
                .addResponses("BadRequest", respostaErro("Requisição inválida ou regra de negócio não atendida."))
                .addResponses("Unauthorized", respostaSeguranca("Token ausente, inválido ou credenciais incorretas."))
                .addResponses("Forbidden", respostaSeguranca("Usuário autenticado sem permissão para o recurso."))
                .addResponses("NotFound", respostaErro("Recurso não encontrado."))
                .addResponses("MethodNotAllowed", respostaErro("Método HTTP não permitido para o endpoint."))
                .addResponses("Conflict", respostaErro("Conflito com restrição de integridade."))
                .addResponses("PayloadTooLarge", respostaErro("Arquivo excede o tamanho máximo permitido."))
                .addResponses("UnsupportedMediaType", respostaErro("Content-Type não suportado."))
                .addResponses("InternalServerError", respostaErro("Erro interno inesperado."));

        return new OpenAPI()
                .components(components)
                .info(new Info()
                        .title("Rascomp API")
                        .version("v1")
                        .description("""
                                API REST para gestão de competições de robótica da IEEE RAS UFRB.

                                Perfis de acesso:
                                - PARTICIPANTE: gerencia somente equipes sob sua responsabilidade, competidores, robôs, fotos e inscrições.
                                - ORGANIZACAO: administra a competição e os endpoints internos de operação.
                                - PUBLICO: consulta somente leitura em /api/v1/public/**, sem dados sensíveis.

                                FOLLOW_LINE não utiliza chaveamento. SUMO utiliza inspeção, chaveamento, partidas e rounds.
                                Resultados de partida são somente leitura e são consolidados automaticamente pelos rounds do Sumô.

                                Respostas 401/403 emitidas diretamente pelo Spring Security possuem o formato compacto
                                {status, error}; os demais erros tratados pela aplicação utilizam ApiErrorResponse.
                                """))
                .tags(tags());
    }

    private List<Tag> tags() {
        return List.of(
                new Tag().name("Autenticação").description("Cadastro de participante, login JWT e usuário autenticado."),
                new Tag().name("Portal do Participante").description("Operações do PARTICIPANTE com validação de ownership."),
                new Tag().name("API Pública").description("Consultas sanitizadas para landing page e acompanhamento público."),
                new Tag().name("Usuários").description("Administração de contas pela ORGANIZACAO."),
                new Tag().name("Instituições").description("Cadastro e manutenção de instituições."),
                new Tag().name("Equipes").description("Cadastro e manutenção de equipes."),
                new Tag().name("Competidores").description("Cadastro e manutenção de competidores."),
                new Tag().name("Robôs").description("Cadastro e manutenção de robôs."),
                new Tag().name("Fotos de Robôs").description("Upload e gestão das fotos dos robôs. JPEG, PNG ou WEBP, até 5 MB."),
                new Tag().name("Competições").description("Cadastro e ciclo de vida das competições."),
                new Tag().name("Categorias").description("Categorias vinculadas às competições e suas modalidades."),
                new Tag().name("Inscrições").description("Gestão administrativa das inscrições, incluindo aprovação e rejeição."),
                new Tag().name("Configuração Follow").description("Parâmetros de execução da modalidade FOLLOW_LINE."),
                new Tag().name("Tentativas Follow").description("Registro das tentativas do Seguidor de Linha."),
                new Tag().name("Ranking Follow").description("Classificação calculada a partir da melhor tentativa válida e concluída."),
                new Tag().name("Configuração Sumô").description("Parâmetros de execução da modalidade SUMO."),
                new Tag().name("Inspeção Sumô").description("Inspeção técnica e aptidão das inscrições de SUMO."),
                new Tag().name("Chaveamentos Sumô").description("Geração e consulta de brackets somente para SUMO."),
                new Tag().name("Partidas Sumô").description("Partidas pertencentes aos chaveamentos de SUMO."),
                new Tag().name("Rounds Sumô").description("Rounds que determinam automaticamente o resultado das partidas."),
                new Tag().name("Resultados de Partida").description("Somente leitura. MatchResult é gerado automaticamente a partir dos rounds do SUMO."));
    }

    private Schema<?> apiErrorSchema() {
        ObjectSchema validationErrors = new ObjectSchema();
        validationErrors.setDescription("Erros de validação por campo quando aplicável. Cada propriedade representa o nome do campo e sua mensagem de validação.");

        return new ObjectSchema()
                .description("Formato padrão de erro retornado pelos handlers da aplicação.")
                .addProperty("timestamp", new StringSchema()
                        .format("date-time")
                        .example("2026-08-24T21:30:00"))
                .addProperty("status", new IntegerSchema().example(400))
                .addProperty("error", new StringSchema().example("Regra de negócio inválida"))
                .addProperty("message", new StringSchema().example("O robô não pertence à equipe informada."))
                .addProperty("path", new StringSchema().example("/api/v1/participante/equipes/4/inscricoes"))
                .addProperty("validationErrors", validationErrors);
    }

    private Schema<?> securityErrorSchema() {
        return new ObjectSchema()
                .description("Formato compacto dos erros emitidos diretamente pelo Spring Security.")
                .addProperty("status", new IntegerSchema().example(401))
                .addProperty("error", new StringSchema().example("Não autenticado"));
    }

    @Bean
    OpenApiCustomizer rascompOpenApiCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) return;

            openApi.getPaths().forEach((path, pathItem) ->
                    pathItem.readOperationsMap().forEach((method, operation) -> {
                        operation.setTags(List.of(tagPara(path)));
                        if (operation.getSummary() == null || operation.getSummary().isBlank()) {
                            operation.setSummary(humanizar(operation.getOperationId()));
                        }
                        normalizarRespostaSucesso(path, method, operation);
                        aplicarDescricaoEspecial(path, method, operation);
                        aplicarSeguranca(path, operation);
                        aplicarRespostasPadrao(path, method, operation);
                    }));
        };
    }

    @Bean
    GroupedOpenApi apiCompleta(OpenApiCustomizer customizer) {
        return grupo("completa", "API completa", customizer, new String[]{"/api/v1/**"}, new String[]{});
    }

    @Bean
    GroupedOpenApi apiPublica(OpenApiCustomizer customizer) {
        return grupo("publica", "Público / Landing", customizer, new String[]{"/api/v1/public/**"}, new String[]{});
    }

    @Bean
    GroupedOpenApi apiParticipante(OpenApiCustomizer customizer) {
        return grupo("participante", "Participante", customizer,
                new String[]{"/api/v1/participante/**", "/api/v1/auth/**"}, new String[]{});
    }

    @Bean
    GroupedOpenApi apiOrganizacao(OpenApiCustomizer customizer) {
        return grupo("organizacao", "Organização", customizer,
                new String[]{"/api/v1/**"},
                new String[]{"/api/v1/public/**", "/api/v1/participante/**", "/api/v1/auth/register"});
    }

    private GroupedOpenApi grupo(
            String nome,
            String displayName,
            OpenApiCustomizer customizer,
            String[] incluir,
            String[] excluir) {
        GroupedOpenApi.Builder builder = GroupedOpenApi.builder()
                .group(nome)
                .displayName(displayName)
                .pathsToMatch(incluir)
                .addOpenApiCustomizer(customizer);
        if (excluir.length > 0) builder.pathsToExclude(excluir);
        return builder.build();
    }

    private ApiResponse respostaErro(String descricao) {
        return respostaComSchema(descricao, "ApiErrorResponse");
    }

    private ApiResponse respostaSeguranca(String descricao) {
        return respostaComSchema(descricao, "SecurityErrorResponse");
    }

    private ApiResponse respostaComSchema(String descricao, String schema) {
        return new ApiResponse()
                .description(descricao)
                .content(new Content().addMediaType(
                        "application/json",
                        new io.swagger.v3.oas.models.media.MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/" + schema))));
    }

    private void aplicarSeguranca(String path, Operation operation) {
        boolean publico = path.startsWith("/api/v1/public/")
                || path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/login");
        if (!publico) operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    private void normalizarRespostaSucesso(String path, HttpMethod method, Operation operation) {
        if (operation.getResponses() == null) return;

        if (method == HttpMethod.POST && !path.equals("/api/v1/auth/login")
                && !operation.getResponses().containsKey("201")) {
            ApiResponse resposta = operation.getResponses().remove("200");
            operation.getResponses().addApiResponse("201",
                    resposta != null
                            ? resposta.description("Criado com sucesso.")
                            : new ApiResponse().description("Criado com sucesso."));
        } else if (method == HttpMethod.DELETE && !operation.getResponses().containsKey("204")) {
            operation.getResponses().remove("200");
            operation.getResponses().addApiResponse("204",
                    new ApiResponse().description("Operação concluída sem conteúdo."));
        }
    }

    private void aplicarRespostasPadrao(String path, HttpMethod method, Operation operation) {
        boolean publico = path.startsWith("/api/v1/public/")
                || path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/login");

        operation.getResponses().addApiResponse("400", refResposta("BadRequest"));
        operation.getResponses().addApiResponse("404", refResposta("NotFound"));
        operation.getResponses().addApiResponse("405", refResposta("MethodNotAllowed"));
        operation.getResponses().addApiResponse("500", refResposta("InternalServerError"));

        if (!publico) {
            operation.getResponses().addApiResponse("401", refResposta("Unauthorized"));
            operation.getResponses().addApiResponse("403", refResposta("Forbidden"));
        }
        if (method == HttpMethod.POST || method == HttpMethod.PUT
                || method == HttpMethod.PATCH || method == HttpMethod.DELETE) {
            operation.getResponses().addApiResponse("409", refResposta("Conflict"));
        }
        if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
            operation.getResponses().addApiResponse("415", refResposta("UnsupportedMediaType"));
        }
        if (path.contains("/fotos")) {
            operation.getResponses().addApiResponse("413", refResposta("PayloadTooLarge"));
        }
    }

    private ApiResponse refResposta(String nome) {
        return new ApiResponse().$ref("#/components/responses/" + nome);
    }

    private void aplicarDescricaoEspecial(String path, HttpMethod method, Operation operation) {
        if (path.equals("/api/v1/auth/register") && method == HttpMethod.POST) {
            operation.setDescription("Cria uma conta PARTICIPANTE. A senha é persistida somente como hash BCrypt e a resposta já contém um JWT.");
        } else if (path.equals("/api/v1/auth/login") && method == HttpMethod.POST) {
            operation.setDescription("Autentica por e-mail e senha e retorna um JWT Bearer.");
        } else if (path.equals("/api/v1/auth/me") && method == HttpMethod.GET) {
            operation.setDescription("Retorna os dados do usuário identificado pelo JWT atual. Nunca expõe passwordHash.");
        } else if (path.contains("/participante/") && path.contains("/fotos") && method == HttpMethod.POST) {
            operation.setDescription("Envia uma foto do robô pertencente à equipe do participante. Formatos aceitos: JPEG, PNG e WEBP; máximo de 5 MB.");
        } else if (path.startsWith("/api/v1/participante/") && path.endsWith("/inscricoes") && method == HttpMethod.POST) {
            operation.setDescription("Cria inscrição PENDENTE para uma equipe sob responsabilidade do participante. Robô e competidores precisam pertencer à mesma equipe.");
        } else if (path.matches("/api/v1/inscricoes/\\{id}") && method == HttpMethod.PUT) {
            operation.setDescription("Atualização administrativa da inscrição. Mudanças para APROVADA ou REJEITADA registram automaticamente o usuário revisor e a data da revisão.");
        } else if (path.startsWith("/api/v1/resultados-partida")) {
            operation.setDescription("Consulta somente leitura. Resultados são criados automaticamente pela consolidação dos rounds de SUMO; não existe API externa de escrita de MatchResult.");
        } else if (path.contains("/ranking/seguidor-linha")) {
            operation.setDescription("Ranking FOLLOW_LINE calculado usando a melhor tentativa válida e concluída de cada inscrição, ordenada pelo menor tempo final.");
        } else if (path.contains("/chaveamentos")) {
            operation.setDescription("Operação de chaveamento exclusiva da modalidade SUMO. FOLLOW_LINE não utiliza bracket.");
        }
    }

    private String tagPara(String path) {
        if (path.startsWith("/api/v1/auth")) return "Autenticação";
        if (path.startsWith("/api/v1/public")) return "API Pública";
        if (path.startsWith("/api/v1/participante")) return "Portal do Participante";
        if (path.contains("/config-follow")) return "Configuração Follow";
        if (path.contains("/config-sumo")) return "Configuração Sumô";
        if (path.contains("/tentativas-seguidor-linha")) return "Tentativas Follow";
        if (path.contains("/ranking/seguidor-linha")) return "Ranking Follow";
        if (path.contains("/inspecoes-sumo")) return "Inspeção Sumô";
        if (path.contains("/resultados-partida")) return "Resultados de Partida";
        if (path.contains("/rounds-sumo")) return "Rounds Sumô";
        if (path.contains("/chaveamentos")) return "Chaveamentos Sumô";
        if (path.contains("/partidas")) return "Partidas Sumô";
        if (path.contains("/usuarios")) return "Usuários";
        if (path.contains("/instituicoes")) return "Instituições";
        if (path.contains("/competidores")) return "Competidores";
        if (path.contains("/robos") && path.contains("/fotos")) return "Fotos de Robôs";
        if (path.contains("/robos")) return "Robôs";
        if (path.contains("/equipes")) return "Equipes";
        if (path.contains("/inscricoes")) return "Inscrições";
        if (path.contains("/categorias")) return "Categorias";
        if (path.contains("/competicoes")) return "Competições";
        return "Gestão";
    }

    private String humanizar(String operationId) {
        if (operationId == null || operationId.isBlank()) return "Operação";
        String texto = operationId
                .replaceAll("_\\d+$", "")
                .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replace('_', ' ')
                .trim();
        if (texto.isBlank()) return "Operação";
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
