package br.com.ia369.virtual_assistant.chat;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuestionAnswerAdvisorMemoryTest {

    static class SimpleChatMemory implements ChatMemory {
        private final Map<String, List<Message>> store = new ConcurrentHashMap<>();

        @Override
        public void add(String conversationId, List<Message> messages) {
            store.computeIfAbsent(conversationId, k -> new ArrayList<>()).addAll(messages);
        }

        @Override
        public List<Message> get(String conversationId) {
            return store.getOrDefault(conversationId, List.of());
        }

        @Override
        public void clear(String conversationId) {
            store.remove(conversationId);
        }
    }

    @Test
    void testAdvisorsInteractionOnTurn2() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());
        when(chatModel.call(any(Prompt.class))).thenReturn(
                new ChatResponse(List.of(new Generation(new AssistantMessage("Resposta 1")))),
                new ChatResponse(List.of(new Generation(new AssistantMessage("Resposta 2"))))
        );

        VectorStore vectorStore = mock(VectorStore.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                new Document("Documento de crédito")
        ));

        ChatMemory chatMemory = new SimpleChatMemory();

        PromptTemplate qaPromptTemplate = PromptTemplate.builder()
                .resource(new org.springframework.core.io.ClassPathResource("/prompts/context-prompt.st"))
                .build();

        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem("System Prompt: Você é o Fillipo.")
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder().build())
                                .promptTemplate(qaPromptTemplate)
                                .build()
                )
                .build();

        String convId = "test-conv-1";

        // Turno 1
        chatClient.prompt().user("Boa tarde! Qual é o seu trabalho?")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, convId))
                .call()
                .content();

        // Turno 2
        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        chatClient.prompt().user("Sim, desconto de títulos.")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, convId))
                .call()
                .content();

        verify(chatModel, times(2)).call(promptCaptor.capture());
        Prompt turn2Prompt = promptCaptor.getAllValues().get(1);

        List<Message> instructions = turn2Prompt.getInstructions();
        assertThat(instructions).hasSize(4);
        assertThat(instructions.get(0).getMessageType().getValue()).isEqualTo("system");
        assertThat(instructions.get(1).getMessageType().getValue()).isEqualTo("user");
        assertThat(instructions.get(1).getText()).isEqualTo("Boa tarde! Qual é o seu trabalho?");
        assertThat(instructions.get(2).getMessageType().getValue()).isEqualTo("assistant");
        assertThat(instructions.get(2).getText()).isEqualTo("Resposta 1");
        assertThat(instructions.get(3).getMessageType().getValue()).isEqualTo("user");

        String turn2UserText = instructions.get(3).getText();
        assertThat(turn2UserText).contains("Pergunta do cliente:");
        assertThat(turn2UserText).contains("Sim, desconto de títulos.");
        assertThat(turn2UserText).contains("CONTEXTO_INICIO");
        assertThat(turn2UserText).doesNotContain("Você é o Fillipo");
        assertThat(turn2UserText).doesNotContain("REGRA ZERO");
        assertThat(turn2UserText).doesNotContain("IDENTIDADE");
    }
}
