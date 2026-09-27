package br.com.ia369.virtual_assistant.whatsapp;

import br.com.ia369.virtual_assistant.whatsapp.dto.ZApiWebhookPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookProtectionServiceTest {

    private WebhookProtectionService protectionService;

    @BeforeEach
    void setUp() {
        protectionService = new WebhookProtectionService();
    }

    @Test
    void shouldAllowValidTextMessage() {
        var payload = new ZApiWebhookPayload(
                "5511999999999",
                false,
                "msg-123",
                false,
                "RECEIVED",
                new ZApiWebhookPayload.MessageData("Olá, tudo bem?"),
                null
        );

        assertThat(protectionService.isAllowed(payload)).isTrue();
    }

    @Test
    void shouldAllowValidAudioMessage() {
        var payload = new ZApiWebhookPayload(
                "5511999999999",
                false,
                "msg-audio-1",
                false,
                "RECEIVED",
                null,
                new ZApiWebhookPayload.AudioData("https://example.com/audio.ogg", "audio/ogg")
        );

        assertThat(protectionService.isAllowed(payload)).isTrue();
    }

    @Test
    void shouldDiscardMessageFromMe() {
        var payload = new ZApiWebhookPayload(
                "5511999999999",
                true,
                "msg-from-me",
                false,
                "RECEIVED",
                new ZApiWebhookPayload.MessageData("Texto enviado pelo bot"),
                null
        );

        assertThat(protectionService.isAllowed(payload)).isFalse();
    }

    @Test
    void shouldDiscardGroupMessage() {
        var payload = new ZApiWebhookPayload(
                "5511999999999-group",
                false,
                "msg-group",
                true,
                "RECEIVED",
                new ZApiWebhookPayload.MessageData("Mensagem de grupo"),
                null
        );

        assertThat(protectionService.isAllowed(payload)).isFalse();
    }

    @Test
    void shouldDiscardMessageWithoutTextAndWithoutAudio() {
        var payload = new ZApiWebhookPayload(
                "5511999999999",
                false,
                "msg-empty",
                false,
                "RECEIVED",
                null,
                null
        );

        assertThat(protectionService.isAllowed(payload)).isFalse();
    }

    @Test
    void shouldDeduplicateIdenticalMessages() {
        var payload1 = new ZApiWebhookPayload(
                "5511999999999",
                false,
                "msg-repeat",
                false,
                "RECEIVED",
                new ZApiWebhookPayload.MessageData("Mesmo texto"),
                null
        );

        var payload2 = new ZApiWebhookPayload(
                "5511999999999",
                false,
                "msg-repeat",
                false,
                "RECEIVED",
                new ZApiWebhookPayload.MessageData("Mesmo texto"),
                null
        );

        assertThat(protectionService.isAllowed(payload1)).isTrue();
        assertThat(protectionService.isAllowed(payload2)).isFalse();
    }

    @Test
    void shouldDeduplicateAudioMessagesByUrlWhenMessageIdIsNull() {
        var payload1 = new ZApiWebhookPayload(
                "5511999999999",
                false,
                null,
                false,
                "RECEIVED",
                null,
                new ZApiWebhookPayload.AudioData("https://example.com/audio-repeat.ogg", "audio/ogg")
        );

        var payload2 = new ZApiWebhookPayload(
                "5511999999999",
                false,
                null,
                false,
                "RECEIVED",
                null,
                new ZApiWebhookPayload.AudioData("https://example.com/audio-repeat.ogg", "audio/ogg")
        );

        assertThat(protectionService.isAllowed(payload1)).isTrue();
        assertThat(protectionService.isAllowed(payload2)).isFalse();
    }
}
