package br.com.ia369.virtual_assistant.whatsapp;

import br.com.ia369.virtual_assistant.chat.ChatService;
import br.com.ia369.virtual_assistant.whatsapp.dto.ZApiWebhookPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/webhooks/zapi", "/webhooks/zapi/luiggi", "/webhooks/zapi/fillipo"})
public class WhatsAppWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final WebhookProtectionService webhookProtectionService;
    private final WhatsAppMessageProcessorService messageProcessorService;
    private final ChatService chatService;

    public WhatsAppWebhookController(WebhookProtectionService webhookProtectionService,
                                     WhatsAppMessageProcessorService messageProcessorService,
                                     ChatService chatService) {
        this.webhookProtectionService = webhookProtectionService;
        this.messageProcessorService = messageProcessorService;
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<Void> receiveMessage(@RequestBody ZApiWebhookPayload payload) {
        if (payload != null && payload.isFromMe()) {
            if (payload.phone() != null && !payload.phone().isBlank() && payload.isText()) {
                logger.info("📥 [fromMe=true] Mensagem enviada pelo bot/atendente detectada para telefone {}. Verificando registro no Redis...", payload.phone());
                chatService.recordAssistantMessageIfNotPresent(payload.phone(), payload.messageData().message());
            }
            return ResponseEntity.ok().build();
        }

        if (!webhookProtectionService.isAllowed(payload)) {
            return ResponseEntity.ok().build();
        }

        // Processa de forma verdadeiramente assíncrona desacoplada da thread HTTP da Z-API
        messageProcessorService.processPayloadAsync(payload);

        return ResponseEntity.ok().build();
    }
}
