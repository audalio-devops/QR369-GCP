package br.com.ia369.virtual_assistant.chat;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    @org.springframework.beans.factory.annotation.Value("${app.assistant.id:fillipo}")
    private String assistantId;

    public ChatService(ChatClient chatClient, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    private String resolveConversationId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return assistantId + ":default";
        }
        if (rawId.startsWith(assistantId + ":")) {
            return rawId;
        }
        return assistantId + ":" + rawId;
    }

    public String chat(String message, String conversationId) {
        String targetConvId = resolveConversationId(conversationId);
        logger.info("[conversationId={}] Iniciando chat (mensagem com {} caracteres)",
                targetConvId, message.length());

        try {
            var responseSpec = chatClient.prompt()
                    .user(message)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, targetConvId))
                    .call();

            String response = responseSpec.content();

            if (response == null || response.isBlank()) {
                logger.warn("[conversationId={}] call().content() retornou vazio! Recuperando resposta via stream...", targetConvId);
                var chunks = stream(message, targetConvId).collectList().block();
                response = chunks != null ? String.join("", chunks) : "";
            }

            logger.info("[conversationId={}] Chat finalizado com sucesso (resposta com {} caracteres)",
                    targetConvId, response != null ? response.length() : 0);
            return response;
        } catch (Exception e) {
            logger.error("[conversationId={}] Erro ao processar chat síncrono via call(): {}. Tentando fallback para stream...",
                    targetConvId, e.getMessage(), e);
            var chunks = stream(message, targetConvId).collectList().block();
            String response = chunks != null ? String.join("", chunks) : "";
            logger.info("[conversationId={}] Resposta recuperada via fallback de stream ({} caracteres)",
                    targetConvId, response.length());
            return response;
        }
    }

    public Flux<String> stream(String message, String conversationId) {
        String targetConvId = resolveConversationId(conversationId);
        logger.info("[conversationId={}] Iniciando stream de chat (mensagem com {} caracteres)",
                targetConvId, message.length());
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, targetConvId))
                .stream()
                .content()
                .doOnComplete(() -> logger.info("[conversationId={}] Stream de chat concluido com sucesso",
                        targetConvId))
                .doOnError(error -> logger.error("[conversationId={}] Erro durante o stream de chat: {}",
                        targetConvId, error.getMessage(), error));
    }

    public void recordAssistantMessageIfNotPresent(String conversationId, String messageText) {
        if (messageText == null || messageText.isBlank()) {
            return;
        }
        String targetConvId = resolveConversationId(conversationId);
        try {
            List<Message> existing = chatMemory.get(targetConvId);
            boolean alreadyPresent = false;
            if (existing != null) {
                for (Message msg : existing) {
                    if (messageText.trim().equalsIgnoreCase(msg.getText() != null ? msg.getText().trim() : "")) {
                        alreadyPresent = true;
                        break;
                    }
                }
            }
            if (!alreadyPresent) {
                chatMemory.add(targetConvId, List.of(new AssistantMessage(messageText)));
                logger.info("[conversationId={}] Mensagem inicial do assistente registrada no Redis com sucesso ({} caracteres)",
                        targetConvId, messageText.length());
            } else {
                logger.info("[conversationId={}] Mensagem do assistente já presente no histórico do Redis para esta conversa",
                        targetConvId);
            }
        } catch (Exception e) {
            logger.error("[conversationId={}] Erro ao registrar mensagem do assistente no Redis: {}",
                    targetConvId, e.getMessage(), e);
        }
    }
}
