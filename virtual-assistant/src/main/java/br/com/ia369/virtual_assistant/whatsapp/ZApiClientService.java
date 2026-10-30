package br.com.ia369.virtual_assistant.whatsapp;

import br.com.ia369.virtual_assistant.config.ZApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Service
public class ZApiClientService {

    private static final Logger logger = LoggerFactory.getLogger(ZApiClientService.class);

    private final RestClient restClient;
    private final String clientToken;
    private final String sendMessageEndpoint;

    public ZApiClientService(ZApiProperties zApiProperties, RestClient.Builder restClientBuilder) {
        String token = (zApiProperties != null && zApiProperties.clientToken() != null && !zApiProperties.clientToken().isBlank())
                ? zApiProperties.clientToken()
                : System.getenv().getOrDefault("ZAPI_CLIENT_TOKEN", "");
        this.clientToken = token;

        String endpoint = (zApiProperties != null && zApiProperties.sendMessageEndpoint() != null && !zApiProperties.sendMessageEndpoint().isBlank())
                ? zApiProperties.sendMessageEndpoint()
                : "/send-text";
        this.sendMessageEndpoint = endpoint;

        String baseUrl = (zApiProperties != null && zApiProperties.apiUrl() != null && !zApiProperties.apiUrl().isBlank())
                ? zApiProperties.apiUrl()
                : "https://api.z-api.io/instances/" + System.getenv("ZAPI_INSTANCE_ID") + "/token/" + System.getenv("ZAPI_TOKEN");

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();

        logger.info("ZApiClientService inicializado com sucesso (baseUrl={}, endpoint={}, clientTokenConfigured={})",
                baseUrl, this.sendMessageEndpoint, !this.clientToken.isBlank());
    }

    public void sendMessage(String phone, String message) {
        if (phone == null || phone.isBlank()) {
            logger.warn("⚠️ Telefone não informado para envio de mensagem.");
            return;
        }
        if (message == null || message.isBlank()) {
            logger.warn("⚠️ Mensagem vazia não enviada para phone={}", phone);
            return;
        }

        logger.info("📤 [phone={}] Enviando resposta para Z-API ({} caracteres)...", phone, message.length());

        try {
            Map<String, String> requestBody = Map.of(
                    "phone", phone,
                    "message", message
            );

            restClient.post()
                    .uri(sendMessageEndpoint)
                    .header("Client-Token", clientToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            logger.info("✅ [phone={}] Mensagem enviada com sucesso para Z-API.", phone);
        } catch (RestClientResponseException e) {
            logger.error("❌ [phone={}] Erro HTTP da Z-API ao enviar mensagem: Status {} {} - Resposta: {}",
                    phone, e.getStatusCode(), e.getStatusText(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            logger.error("❌ [phone={}] Falha inesperada ao enviar mensagem para Z-API: {}",
                    phone, e.getMessage(), e);
            throw e;
        }
    }
}
