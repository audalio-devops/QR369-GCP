package br.com.ia369.virtual_assistant.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;

    @org.springframework.beans.factory.annotation.Value("${app.assistant.id:fillipo}")
    private String assistantId;

    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
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

        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, targetConvId))
                .call()
                .content();
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
}
