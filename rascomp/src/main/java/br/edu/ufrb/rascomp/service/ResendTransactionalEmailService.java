package br.edu.ufrb.rascomp.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "resend")
public class ResendTransactionalEmailService implements TransactionalEmailService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.email.resend.api-key:}")
    private String apiKey;

    @Value("${app.email.from:RasComp <no-reply@localhost>}")
    private String from;

    public ResendTransactionalEmailService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void send(String to, String subject, String html) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("RESEND_API_KEY não configurada para envio transacional.");
        }

        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "from", from,
                    "to", new String[] { to },
                    "subject", subject,
                    "html", html));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Falha no envio de e-mail transacional. HTTP " + response.statusCode());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Envio de e-mail interrompido.", ex);
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException illegalState) {
                throw illegalState;
            }
            throw new IllegalStateException("Falha ao enviar e-mail transacional.", ex);
        }
    }
}
