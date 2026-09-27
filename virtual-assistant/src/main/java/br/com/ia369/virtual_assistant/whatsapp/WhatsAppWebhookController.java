package br.com.ia369.virtual_assistant.whatsapp;

import br.com.ia369.virtual_assistant.whatsapp.dto.ZApiWebhookPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/zapi")
public class WhatsAppWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final WebhookProtectionService webhookProtectionService;
    private final WhatsAppMessageProcessorService messageProcessorService;

    public WhatsAppWebhookController(WebhookProtectionService webhookProtectionService,
                                     WhatsAppMessageProcessorService messageProcessorService) {
        this.webhookProtectionService = webhookProtectionService;
        this.messageProcessorService = messageProcessorService;
    }

    @PostMapping
    public ResponseEntity<Void> receiveMessage(@RequestBody ZApiWebhookPayload payload) {
        if (!webhookProtectionService.isAllowed(payload)) {
            return ResponseEntity.ok().build();
        }

        // Processa de forma verdadeiramente assíncrona desacoplada da thread HTTP da Z-API
        messageProcessorService.processPayloadAsync(payload);

        return ResponseEntity.ok().build();
    }
}
