package br.edu.ufrb.rascomp.exception;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ApiErrorResponse", description = "Formato padrão de erro retornado pela API Rascomp.")
public record ApiErrorResponse(
        @Schema(example = "2026-08-24T21:30:00", description = "Momento em que o erro foi produzido.")
        LocalDateTime timestamp,
        @Schema(example = "400", description = "Status HTTP numérico.")
        int status,
        @Schema(example = "Regra de negócio inválida", description = "Categoria resumida do erro.")
        String error,
        @Schema(example = "O robô não pertence à equipe informada.", description = "Mensagem segura para o cliente.")
        String message,
        @Schema(example = "/api/v1/participante/equipes/4/inscricoes", description = "Caminho solicitado.")
        String path,
        @Schema(description = "Erros de validação por campo quando aplicável.", example = "{\"email\":\"deve ser um endereço de e-mail bem formado\"}")
        Map<String, String> validationErrors
) {

    public ApiErrorResponse(
            int status,
            String error,
            String message,
            String path) {

        this(
                LocalDateTime.now(),
                status,
                error,
                message,
                path,
                null
        );
    }

    public ApiErrorResponse(
            int status,
            String error,
            String message,
            String path,
            Map<String, String> validationErrors) {

        this(
                LocalDateTime.now(),
                status,
                error,
                message,
                path,
                validationErrors
        );
    }
}
