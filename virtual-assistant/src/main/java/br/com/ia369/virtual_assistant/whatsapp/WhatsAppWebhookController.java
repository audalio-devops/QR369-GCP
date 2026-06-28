package br.com.ia369.virtual_assistant.whatsapp;

import br.com.ia369.virtual_assistant.chat.ChatService;
import br.com.ia369.virtual_assistant.whatsapp.dto.ZApiWebhookPayload;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/zapi")
public class WhatsAppWebhookController {

    private final ChatService chatService;
    private final ZApiClientService zApiClientService;

    public WhatsAppWebhookController(ChatService chatService, ZApiClientService zApiClientService) {
        this.chatService = chatService;
        this.zApiClientService = zApiClientService;
    }

    @PostMapping
    public ResponseEntity<Void> receiveMessage(@RequestBody ZApiWebhookPayload payload) {
        var userPhone = payload.phone();
        var userMessage = payload.messageData().message();

        // O ID da conversa será o número de telefone do usuário
        var conversationId = userPhone;

        var response = chatService.chat(userMessage, conversationId);

        zApiClientService.sendMessage(userPhone, response);

        return ResponseEntity.ok().build();
    }
}
