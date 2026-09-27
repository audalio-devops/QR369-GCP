package br.com.ia369.virtual_assistant.whatsapp;

import br.com.ia369.virtual_assistant.chat.ChatService;
import br.com.ia369.virtual_assistant.whatsapp.dto.ZApiWebhookPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WhatsAppMessageProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppMessageProcessorService.class);

    public static final String AUDIO_ERROR_FALLBACK_MESSAGE =
            "Não foi possível compreender o áudio enviado. Por favor, tente enviar um novo áudio ou envie sua mensagem por texto.";

    private final ChatService chatService;
    private final ZApiClientService zApiClientService;
    private final AudioTranscriptionService audioTranscriptionService;

    public WhatsAppMessageProcessorService(ChatService chatService,
                                           ZApiClientService zApiClientService,
                                           AudioTranscriptionService audioTranscriptionService) {
        this.chatService = chatService;
        this.zApiClientService = zApiClientService;
        this.audioTranscriptionService = audioTranscriptionService;
    }

    /**
     * Processa o payload do webhook de forma verdadeiramente assíncrona.
     * Suporta mensagens de texto (fluxo preservado) e mensagens de áudio (com transcrição e retry).
     */
    @Async
    public void processPayloadAsync(ZApiWebhookPayload payload) {
        String phone = payload.phone();
        String userMessage = null;

        try {
            if (payload.isText()) {
                // Fluxo de texto existente: 100% mantido
                userMessage = payload.messageData().message();
                logger.info("💬 [phone={}] Mensagem de texto recebida: \"{}\"", phone, userMessage);
            } else if (payload.isAudio()) {
                // Fluxo de áudio: download + transcrição com retry de 10s
                String audioUrl = payload.audioData().audioUrl();
                logger.info("🎙️ [phone={}] Mensagem de áudio recebida. Iniciando transcrição...", phone);

                Optional<String> transcription = audioTranscriptionService.transcribeAudioWithRetry(audioUrl, phone);

                if (transcription.isEmpty() || transcription.get().isBlank()) {
                    logger.warn("❌ [phone={}] Falha definitiva na transcrição. Enviando mensagem de orientação ao cliente.", phone);
                    zApiClientService.sendMessage(phone, AUDIO_ERROR_FALLBACK_MESSAGE);
                    return;
                }

                userMessage = transcription.get();
                logger.info("🎤 [phone={}] Áudio transcrito com sucesso: \"{}\"", phone, userMessage);
            }

            if (userMessage != null && !userMessage.isBlank()) {
                String conversationId = phone;
                String response = chatService.chat(userMessage, conversationId);
                zApiClientService.sendMessage(phone, response);
                logger.info("✅ [phone={}] Resposta do assistente enviada com sucesso.", phone);
            }

        } catch (Exception e) {
            logger.error("❌ Erro ao processar mensagem assíncrona do WhatsApp (phone={}): {}", phone, e.getMessage(), e);
        }
    }
}
