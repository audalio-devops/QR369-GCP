package br.com.ia369.virtual_assistant.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceRecordMessageTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatMemory chatMemory;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(chatClient, chatMemory);
        ReflectionTestUtils.setField(chatService, "assistantId", "fillipo");
    }

    @Test
    @DisplayName("Deve gravar mensagem do assistente no Redis se histórico estiver vazio")
    void shouldRecordMessageWhenHistoryIsEmpty() {
        String phone = "5511999998888";
        String initialMessage = "Olá! Aqui é o Fillipo, da QR Gold.";

        when(chatMemory.get(eq("fillipo:" + phone))).thenReturn(List.of());

        chatService.recordAssistantMessageIfNotPresent(phone, initialMessage);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        verify(chatMemory, times(1)).add(eq("fillipo:" + phone), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getText()).isEqualTo(initialMessage);
    }

    @Test
    @DisplayName("Não deve duplicar se mensagem idêntica já estiver no histórico do Redis")
    void shouldNotDuplicateWhenMessageAlreadyExists() {
        String phone = "5511999998888";
        String initialMessage = "Olá! Aqui é o Fillipo, da QR Gold.";

        when(chatMemory.get(eq("fillipo:" + phone))).thenReturn(List.of(
                new AssistantMessage(initialMessage)
        ));

        chatService.recordAssistantMessageIfNotPresent(phone, initialMessage);

        verify(chatMemory, never()).add(any(), anyList());
    }

    @Test
    @DisplayName("Deve gravar nova mensagem se histórico tiver apenas mensagens do usuário")
    void shouldRecordWhenHistoryContainsOtherMessages() {
        String phone = "5511999998888";
        String initialMessage = "Olá! Aqui é o Fillipo, da QR Gold.";

        when(chatMemory.get(eq("fillipo:" + phone))).thenReturn(List.of(
                new UserMessage("Oi")
        ));

        chatService.recordAssistantMessageIfNotPresent(phone, initialMessage);

        verify(chatMemory, times(1)).add(eq("fillipo:" + phone), anyList());
    }
}
