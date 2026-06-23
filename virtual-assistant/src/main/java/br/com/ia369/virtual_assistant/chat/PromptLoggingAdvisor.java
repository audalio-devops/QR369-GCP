package br.com.ia369.virtual_assistant.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;

import reactor.core.publisher.Flux;

/**
 * Loga, em nivel DEBUG, o prompt final enviado ao modelo (bloco de IDA) e a
 * resposta gerada (bloco de VOLTA). O prompt ja contem a mensagem do usuario, o
 * contexto recuperado (RAG) e as ancoras do template (CONTEXTO_INICIO/CONTEXTO_FIM).
 *
 * Por usar order maior que o do QuestionAnswerAdvisor (0), o prompt e visto
 * DEPOIS da augmentacao, completo. No modo stream, a resposta e agregada token a
 * token e logada uma unica vez, ja completa.
 */
public class PromptLoggingAdvisor implements BaseAdvisor {

    private static final Logger logger = LoggerFactory.getLogger(PromptLoggingAdvisor.class);

    private static final String BORDER = "═".repeat(70);

    private final int order;

    public PromptLoggingAdvisor(int order) {
        this.order = order;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        logPrompt(request);
        ChatClientResponse response = chain.nextCall(request);
        logResponse(request, response);
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        logPrompt(request);
        Flux<ChatClientResponse> responses = chain.nextStream(request);
        // agrega os tokens do stream e loga a resposta completa uma unica vez
        return new ChatClientMessageAggregator()
                .aggregateChatClientResponse(responses, aggregated -> logResponse(request, aggregated));
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }

    @Override
    public int getOrder() {
        return order;
    }

    private void logPrompt(ChatClientRequest request) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        Object conversationId = request.context().get(ChatMemory.CONVERSATION_ID);
        StringBuilder prompt = new StringBuilder();
        for (Message message : request.prompt().getInstructions()) {
            prompt.append("\n┌── ").append(message.getMessageType()).append(" ──\n");
            prompt.append(message.getText());
        }
        appendTools(prompt, request);
        logger.debug("\n📤 {}\n📤 IDA — Prompt enviado ao modelo [conversationId={}]\n📤 {}{}\n📤 {}",
                BORDER, conversationId, BORDER, prompt, BORDER);
    }

    /**
     * Acrescenta ao bloco de IDA as tools (ferramentas) disponiveis na requisicao.
     * Sao elas que o modelo recebe como "texto do tooling" (nome, descricao e schema
     * de entrada em JSON). So aparece quando as options sao do tipo
     * ToolCallingChatOptions e existe ao menos uma tool registrada.
     */
    private void appendTools(StringBuilder prompt, ChatClientRequest request) {
        ChatOptions options = request.prompt().getOptions();
        if (!(options instanceof ToolCallingChatOptions toolOptions)) {
            return;
        }
        List<ToolCallback> toolCallbacks = toolOptions.getToolCallbacks();
        if (toolCallbacks == null || toolCallbacks.isEmpty()) {
            return;
        }
        prompt.append("\n┌── TOOLS DISPONIVEIS ──");
        for (ToolCallback toolCallback : toolCallbacks) {
            ToolDefinition definition = toolCallback.getToolDefinition();
            prompt.append("\n- ").append(definition.name());
            prompt.append("\n  descricao: ").append(definition.description());
            prompt.append("\n  schema: ").append(definition.inputSchema());
        }
    }

    private void logResponse(ChatClientRequest request, ChatClientResponse response) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        Object conversationId = request.context().get(ChatMemory.CONVERSATION_ID);
        String answer = extractText(response);
        logger.debug("\n📥 {}\n📥 VOLTA — Resposta gerada pelo modelo [conversationId={}]\n📥 {}\n{}\n📥 {}",
                BORDER, conversationId, BORDER, answer, BORDER);
    }

    private String extractText(ChatClientResponse response) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null || chatResponse.getResult() == null
                || chatResponse.getResult().getOutput() == null) {
            return "(sem conteudo)";
        }
        return chatResponse.getResult().getOutput().getText();
    }
}
