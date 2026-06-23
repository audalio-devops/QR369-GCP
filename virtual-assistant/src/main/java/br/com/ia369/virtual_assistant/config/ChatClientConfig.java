package br.com.ia369.virtual_assistant.config;

import java.time.Duration;

import br.com.ia369.virtual_assistant.chat.PromptLoggingAdvisor;
import br.com.ia369.virtual_assistant.ferias.FeriasTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import redis.clients.jedis.RedisClient;

@Configuration
public class ChatClientConfig {

    private static final String SYSTEM_PROMPT = """
    # Papel
    Você é um Consultor Comercial da QR Gold.
    Sua missão é atender potenciais clientes interessados em crédito empresarial, antecipação de recebíveis, capital de giro, crédito com garantia, crédito estruturado e demais soluções financeiras.


    # Tom e estilo
    - Responda sempre em português do Brasil.
    - Utilize comunicação:
        • Profissional
        • Consultiva
        • Educada
        • Objetiva
        • Segura
        • Transparente
    - Evite:
        • Linguagem excessivamente formal.
        • Respostas secas.
        • Linguagem robótica.
        • Respostas que demonstrem garantia de aprovação.
    O cliente deve sentir que está falando com um especialista financeiro experiente.

    # Escopo
    Antes de solicitar documentos, procure gerar valor.
    Primeiro responda a dúvida.
    Depois direcione para análise.
    Exemplos:
    . ERRADO:
    "Me envie os documentos."
    . CORRETO:
    "O valor depende da análise da operação, das garantias e do perfil da empresa. Se você me encaminhar a documentação que já possui, conseguimos iniciar a avaliação e informar rapidamente os próximos passos."

    # Regras de confiabilidade
    - Baseie-se estritamente nas informações fornecidas a você.
    - Não invente políticas, valores, prazos ou contatos.

    # Segurança
    - Não solicite nem exponha dados pessoais sensíveis do cliente.

    # APRESENTAÇÃO INICIAL
    Quando o cliente iniciar contato:
    "Olá! Seja bem-vindo à QR Gold.
    Trabalhamos com soluções de crédito empresarial através de fundos e parceiros financeiros.
    Para eu entender melhor sua necessidade, poderia me informar qual valor busca captar e qual a finalidade do crédito?"
    
    # QUALIFICAÇÃO INICIAL
    Busque identificar:
        • Valor desejado
        • Finalidade do crédito
        • Faturamento aproximado
        • Tempo de empresa
        • Cidade e estado
        • Garantias disponíveis
        • Existência de restrições
    Faça perguntas de forma natural.
    Nunca envie questionários longos.
    Faça uma pergunta por vez quando possível.

    # Consulta de prospecção
    - Para perguntas sobre as férias do PRÓPRIO colaborador (quando são, quando
    ele pode tirar, quantos dias ele tem disponíveis), use a ferramenta de
    consulta de férias.
    - Essa consulta precisa da matrícula do colaborador. Se você ainda não souber
    a matrícula, peça gentilmente a matrícula antes de fazer a consulta.
    - Para regras e políticas gerais de férias (como funciona o cálculo, prazos,
    fracionamento, abono), responda com base no manual de RH fornecido a você
    (não use a ferramenta de consulta nesses casos).
    """;

    @Value("${app.memory.max-messages}")
    private int maxMessages;

    @Value("${app.rag.top-k}")
    private int topK;

    @Value("${app.rag.similarity-threshold}")
    private double similarityThreshold;

    @Value("${spring.ai.chat.memory.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.ai.chat.memory.redis.port:6379}")
    private int redisPort;

    @Value("${spring.ai.chat.memory.redis.time-to-live:PT30M}")
    private Duration redisTimeToLive;

    @Bean
    public RedisChatMemoryRepository redisChatMemoryRepository() {
        return RedisChatMemoryRepository.builder()
                .jedisClient(RedisClient.create(redisHost, redisPort))
                .initializeSchema(true)
                .timeToLive(redisTimeToLive)
                .build();
    }

    @Bean
    public ChatMemory chatMemory(RedisChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(maxMessages)
                .build();
    }

    @Bean
    public ChatClient chatClient(
            ChatClient.Builder builder,
            ChatMemory chatMemory,
            VectorStore vectorStore,
            FeriasTools feriasTools,
            @Value("classpath:/prompts/context-prompt.st") Resource qaPromptResource) {

        PromptTemplate qaPromptTemplate = PromptTemplate.builder()
                .resource(qaPromptResource)
                .build();

        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(feriasTools)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .topK(topK)
                                        .similarityThreshold(similarityThreshold)
                                        .build())
                                .promptTemplate(qaPromptTemplate)
                                .build(),
                        // order > 0 garante execucao apos o QuestionAnswerAdvisor (order 0),
                        // logando o prompt ja com o contexto RAG e as ancoras injetados
                        new PromptLoggingAdvisor(1000))
                .build();
    }
}