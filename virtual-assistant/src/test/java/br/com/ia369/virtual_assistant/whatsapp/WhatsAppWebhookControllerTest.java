package br.com.ia369.virtual_assistant.whatsapp;

import br.com.ia369.virtual_assistant.chat.ChatService;
import br.com.ia369.virtual_assistant.whatsapp.dto.ZApiWebhookPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppWebhookControllerTest {

    @Mock
    private WebhookProtectionService webhookProtectionService;

    @Mock
    private WhatsAppMessageProcessorService messageProcessorService;

    @Mock
    private ChatService chatService;

    private WhatsAppWebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new WhatsAppWebhookController(
                webhookProtectionService,
                messageProcessorService,
                chatService
        );
    }

    @Test
    @DisplayName("Mensagem com fromMe=true deve ser registrada no Redis e não deve processar resposta de bot")
    void shouldRegisterFromMeMessageInRedisWithoutCallingProcessor() {
        String phone = "5511999998888";
        String outboundText = "Olá, sou o Fillipo da QR Gold!";

        var payload = new ZApiWebhookPayload(
                phone,
                true,
                "outbound-msg-123",
                false,
                "SENT",
                new ZApiWebhookPayload.MessageData(outboundText),
                null
        );

        ResponseEntity<Void> response = controller.receiveMessage(payload);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(chatService, times(1)).recordAssistantMessageIfNotPresent(phone, outboundText);
        verify(messageProcessorService, never()).processPayloadAsync(any());
        verify(webhookProtectionService, never()).isAllowed(any());
    }

    @Test
    @DisplayName("Mensagem inbound (fromMe=false) deve passar por proteção e processor assíncrono")
    void shouldPassInboundMessageToProtectionAndProcessor() {
        String phone = "5511999998888";
        String inboundText = "Olá, gostaria de saber sobre antecipação.";

        var payload = new ZApiWebhookPayload(
                phone,
                false,
                "inbound-msg-456",
                false,
                "RECEIVED",
                new ZApiWebhookPayload.MessageData(inboundText),
                null
        );

        when(webhookProtectionService.isAllowed(payload)).thenReturn(true);

        ResponseEntity<Void> response = controller.receiveMessage(payload);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(chatService, never()).recordAssistantMessageIfNotPresent(any(), any());
        verify(messageProcessorService, times(1)).processPayloadAsync(payload);
    }
}
