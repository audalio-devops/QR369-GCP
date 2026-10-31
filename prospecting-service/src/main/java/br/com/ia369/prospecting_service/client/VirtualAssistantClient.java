package br.com.ia369.prospecting_service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class VirtualAssistantClient {

    private static final Logger log = LoggerFactory.getLogger(VirtualAssistantClient.class);

    private final RestClient restClient;

    public VirtualAssistantClient(
            @Value("${virtual-assistant.base-url:http://localhost:8080}") String baseUrl,
            RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    public VirtualAssistantClient(String baseUrl) {
        this(baseUrl, RestClient.builder());
    }

    public void recordInitialContact(String phone, String message) {
        log.info("Registrando mensagem de contato inicial no virtual-assistant para o telefone: {}", phone);
        try {
            Map<String, String> body = Map.of(
                    "phone", phone,
                    "message", message
            );
            restClient.post()
                    .uri("/chat/initial-contact")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Mensagem de contato inicial registrada com sucesso no virtual-assistant para {}", phone);
        } catch (Exception ex) {
            log.warn("Falha ao registrar contato inicial no virtual-assistant para {}: {}", phone, ex.getMessage());
        }
    }
}
