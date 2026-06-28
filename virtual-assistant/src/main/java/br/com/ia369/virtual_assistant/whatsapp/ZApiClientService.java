package br.com.ia369.virtual_assistant.whatsapp;

import br.com.ia369.virtual_assistant.config.ZApiProperties;
import br.com.ia369.virtual_assistant.whatsapp.dto.SendMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ZApiClientService {

    private static final Logger logger = LoggerFactory.getLogger(ZApiClientService.class);

    private final WebClient webClient;
    private final ZApiProperties zApiProperties;

    public ZApiClientService(ZApiProperties zApiProperties) {
        this.zApiProperties = zApiProperties;
        this.webClient = WebClient.builder()
                .baseUrl(zApiProperties.apiUrl())
                .build();
    }

    public void sendMessage(String phone, String message) {
        var request = new SendMessageRequest(phone, message);

        webClient.post()
                .uri(zApiProperties.sendMessageEndpoint())
                .body(Mono.just(request), SendMessageRequest.class)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(error -> logger.error("Erro ao enviar mensagem para Z-API: {}", error.getMessage()))
                .subscribe();
    }
}
