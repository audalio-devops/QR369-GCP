package br.com.ia369.virtual_assistant.whatsapp;

import br.com.ia369.virtual_assistant.chat.ChatService;
import br.com.ia369.virtual_assistant.whatsapp.dto.ZApiWebhookPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/zapi")
public class WhatsAppWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final ChatService chatService;
    private final ZApiClientService zApiClientService;
    private final WebhookProtectionService webhookProtectionService;

    public WhatsAppWebhookController(ChatService chatService,
            ZApiClientService zApiClientService,
            WebhookProtectionService webhookProtectionService) {
        this.chatService = chatService;
        this.zApiClientService = zApiClientService;
        this.webhookProtectionService = webhookProtectionService;
    }

    @PostMapping
    public ResponseEntity<Void> receiveMessage(@RequestBody ZApiWebhookPayload payload) {
        if (!webhookProtectionService.isAllowed(payload)) {
            return ResponseEntity.ok().build();
        }

        // Processa de forma assíncrona para liberar a resposta HTTP 200 OK
        // imediatamente para a Z-API
        processMessageAsync(payload);

        return ResponseEntity.ok().build();
    }

    @Async
    public void processMessageAsync(ZApiWebhookPayload payload) {
        try {
            var userPhone = payload.phone();
            var userMessage = payload.messageData().message();
            var conversationId = userPhone;

            var response = chatService.chat(userMessage, conversationId);
            zApiClientService.sendMessage(userPhone, response);
        } catch (Exception e) {
            logger.error("❌ Erro ao processar mensagem assíncrona do WhatsApp (phone={}): {}", payload.phone(),
                    e.getMessage(), e);
        }
    }
}
