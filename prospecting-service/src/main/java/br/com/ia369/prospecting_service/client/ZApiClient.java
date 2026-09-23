package br.com.ia369.prospecting_service.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Cliente HTTP para a Z-API (WhatsApp).
 *
 * Endpoints utilizados:
 * - GET /phone-exists/{number} → verifica se o número tem WhatsApp
 * - POST /send-text → envia mensagem de texto
 *
 * Header obrigatório em toda requisição: Client-Token
 * Formato do número: DDI + DDD + número (ex.: 5511999999999)
 */
@Component
public class ZApiClient {

    private static final Logger log = LoggerFactory.getLogger(ZApiClient.class);

    private final RestClient restClient;
    private final String clientToken;

    @org.springframework.beans.factory.annotation.Autowired
    public ZApiClient(
            @Value("${zapi.base-url}") String baseUrl,
            @Value("${zapi.instance}") String instance,
            @Value("${zapi.token}") String token,
            @Value("${zapi.client-token}") String clientToken,
            RestClient.Builder restClientBuilder) {

        this.clientToken = clientToken;
        this.restClient = restClientBuilder
                .baseUrl(baseUrl + "/instances/" + instance + "/token/" + token)
                .build();
    }

    public ZApiClient(
            String baseUrl,
            String instance,
            String token,
            String clientToken) {
        this(baseUrl, instance, token, clientToken, RestClient.builder());
    }

    /**
     * Verifica se a instância da Z-API está conectada ao WhatsApp.
     * Endpoint: GET /status
     * Header obrigatório: Client-Token
     *
     * @return true se "connected" for true, false caso contrário ou em caso de erro
     */
    public boolean isConnected() {
        log.info("Verificando status de conexão da Z-API...");
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri("/status")
                    .header("Client-Token", clientToken)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("connected")) {
                boolean connected = Boolean.TRUE.equals(response.get("connected"));
                log.info("Status de conexão Z-API: connected = {}", connected);
                return connected;
            }
            log.warn("Resposta inesperada da Z-API para verificação de status: {}", response);
            return false;
        } catch (Exception ex) {
            log.error("Erro ao verificar status de conexão da Z-API: {}", ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Verifica se o número tem WhatsApp via phone-exists.
     *
     * @param phoneNumber número no formato DDI+DDD+número (ex.: 5511999999999)
     * @return true se o número existir no WhatsApp, false em caso de não encontrado
     *         ou erro
     */
    public boolean phoneExists(String phoneNumber) {
        log.info("Validando telefone no WhatsApp: {}", phoneNumber);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri("/phone-exists/{phone}", phoneNumber)
                    .header("Client-Token", clientToken)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("exists")) {
                boolean exists = Boolean.TRUE.equals(response.get("exists"));
                log.info("Telefone {}: existe no WhatsApp = {}", phoneNumber, exists);
                return exists;
            }
            log.warn("Resposta inesperada da Z-API para phone-exists ({}): {}", phoneNumber, response);
            return false;
        } catch (Exception ex) {
            log.error("Erro ao validar telefone {} via Z-API: {}", phoneNumber, ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Envia uma mensagem de texto via Z-API.
     *
     * @param phoneNumber número destino no formato DDI+DDD+número
     * @param message     texto da mensagem
     * @throws RuntimeException se o envio falhar
     */
    public void sendTextMessage(String phoneNumber, String message) {
        log.info("Enviando mensagem WhatsApp para: {}", phoneNumber);
        try {
            Map<String, String> body = Map.of(
                    "phone", phoneNumber,
                    "message", message);
            restClient.post()
                    .uri("/send-text")
                    .header("Client-Token", clientToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Mensagem enviada com sucesso para: {}", phoneNumber);
        } catch (Exception ex) {
            log.error("Falha ao enviar mensagem para {}: {}", phoneNumber, ex.getMessage(), ex);
            throw new RuntimeException("Falha ao enviar mensagem via Z-API para " + phoneNumber, ex);
        }
    }
}
