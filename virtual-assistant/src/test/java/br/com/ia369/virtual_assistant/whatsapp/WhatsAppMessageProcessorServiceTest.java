package br.com.ia369.virtual_assistant.whatsapp;

import br.com.ia369.virtual_assistant.chat.ChatService;
import br.com.ia369.virtual_assistant.whatsapp.dto.ZApiWebhookPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppMessageProcessorServiceTest {

    @Mock
    private ChatService chatService;

    @Mock
    private ZApiClientService zApiClientService;

    @Mock
    private AudioTranscriptionService audioTranscriptionService;

    private WhatsAppMessageProcessorService processorService;

    @BeforeEach
    void setUp() {
        processorService = new WhatsAppMessageProcessorService(
                chatService,
                zApiClientService,
                audioTranscriptionService
        );
    }

    @Test
    void shouldProcessTextMessageSuccessfully() {
        String phone = "5511999999999";
        String userText = "Gostaria de saber sobre crédito.";
        String botResponse = "Temos excelentes linhas de crédito.";

        when(chatService.chat(userText, phone)).thenReturn(botResponse);

        var payload = new ZApiWebhookPayload(
                phone,
                false,
                "msg-1",
                false,
                "RECEIVED",
                new ZApiWebhookPayload.MessageData(userText),
                null
        );

        processorService.processPayloadAsync(payload);

        verify(chatService, times(1)).chat(userText, phone);
        verify(zApiClientService, times(1)).sendMessage(phone, botResponse);
        verifyNoInteractions(audioTranscriptionService);
    }

    @Test
    void shouldProcessAudioMessageWithSuccessfulTranscription() {
        String phone = "5511999999999";
        String audioUrl = "https://example.com/audio.ogg";
        String transcribedText = "Texto falado no áudio";
        String botResponse = "Resposta do assistente para o áudio";

        when(audioTranscriptionService.transcribeAudioWithRetry(audioUrl, phone))
                .thenReturn(Optional.of(transcribedText));
        when(chatService.chat(transcribedText, phone)).thenReturn(botResponse);

        var payload = new ZApiWebhookPayload(
                phone,
                false,
                "msg-audio",
                false,
                "RECEIVED",
                null,
                new ZApiWebhookPayload.AudioData(audioUrl, "audio/ogg")
        );

        processorService.processPayloadAsync(payload);

        verify(audioTranscriptionService, times(1)).transcribeAudioWithRetry(audioUrl, phone);
        verify(chatService, times(1)).chat(transcribedText, phone);
        verify(zApiClientService, times(1)).sendMessage(phone, botResponse);
    }

    @Test
    void shouldSendFallbackMessageWhenAudioTranscriptionFailsAfterRetries() {
        String phone = "5511999999999";
        String audioUrl = "https://example.com/audio-ruim.ogg";

        when(audioTranscriptionService.transcribeAudioWithRetry(audioUrl, phone))
                .thenReturn(Optional.empty());

        var payload = new ZApiWebhookPayload(
                phone,
                false,
                "msg-audio-fail",
                false,
                "RECEIVED",
                null,
                new ZApiWebhookPayload.AudioData(audioUrl, "audio/ogg")
        );

        processorService.processPayloadAsync(payload);

        verify(audioTranscriptionService, times(1)).transcribeAudioWithRetry(audioUrl, phone);
        verifyNoInteractions(chatService);
        verify(zApiClientService, times(1)).sendMessage(
                eq(phone),
                eq(WhatsAppMessageProcessorService.AUDIO_ERROR_FALLBACK_MESSAGE)
        );
    }
}
