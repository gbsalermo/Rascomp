package br.edu.ufrb.rascomp.exception;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
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