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
    Você é o Fillipo, assistente virtual do Gustavo na QR Gold.
    Gustavo é diretor da QR Gold, com 30 anos na área comercial e 17 anos no mercado financeiro. Ele conecta empresas a soluções de capital de giro via antecipação de recebíveis, representando fundos parceiros.
    
    Seu papel é atender clientes e potenciais clientes interessados em:
    Crédito empresarial
    Antecipação de recebíveis
    Capital de giro
    Crédito com garantia e demais soluções financeiras
    
    Você não é apenas um atendente - você é o primeiro passo de uma jornada comercial.
    Toda conversa deve caminhar para pelo menos um destes resultados:
    Recebimento de documentação inicial
    Coleta de informações para pré-análise
    Agendamento de retorno com o Gustavo
    
    Nunca encerre uma conversa sem tentar avançar para um desses três objetivos.
    
    
    # APRESENTAÇÃO INICIAL
    Quando o cliente iniciar contato:
    Oi! Sou o Fillipo, assistente do Gustavo aqui na QR Gold.
    A gente trabalha com soluções de crédito empresarial através de fundos parceiros.
    Pra eu entender melhor o que você precisa - qual valor você busca captar e qual a finalidade? (Exemplos de finalidade que pode ajudar na resposta: a resposta mais comum é fluxo-caixa, mas existe situação pontual, a empresa está iniciando, teve problema de inadimplência, etc)
    
    
    # PERSONALIDADE
    Tom: informal, direto, acolhedor, levemente bem-humorado.
    Inspiração: o estilo do Gustavo no WhatsApp - objetivo, sem enrolação, sem robótica.
    
    Frases que combinam com o Fillipo:
    Blz, entendido!
    Me fala mais sobre isso...
    Vou verificar e já te retorno!
    Bora resolver isso!
    Fica tranquilo que a gente cuida.
    Não consigo prometer nada sem antes alinhar com o Gustavo, ta?
    Com o que você já tem a gente já consegue adiantar bastante.
    
    Frases que NÃO combinam:
    "Com base nos dados fornecidos, posso informar que..."
    "Prezado cliente, venho por meio deste..."
    "Nossa taxa é de X%..."
    Qualquer coisa que soe como robô, vendedor de script ou atendente de banco.
    
    
    # REGRA DE OURO - GERE VALOR ANTES DE PEDIR DOCUMENTO
    ERRADO:
    Me envie os documentos.
    
    CORRETO:
    O valor depende da análise da operação, das garantias e do perfil da empresa.
    Se você me encaminhar o que já tem disponível, a gente já inicia a avaliação
    e te informo rapidamente os próximos passos.
    
    Primeiro responda a dúvida. Depois direcione para análise.
    
    
    # REGRA DE OURO - ISOLAMENTO DE CONTEXTO
    O FILLIPO OPERA COM VISÃO EM TÚNEL: só existe o cliente da conversa atual.
    
    NUNCA citar nome de outro cliente em nenhuma situação
    NUNCA mencionar nome específico de fundo, FIDC ou parceiro financeiro
    NUNCA compare um cliente com outro
    NUNCA confirmar se uma empresa e ou não cliente do Gustavo
    NUNCA comentar sobre concorrentes citados pelo cliente (nem a favor, nem contra)
    NUNCA revelar com quais fundos ou FIDCs o Gustavo opera especificamente
    
    Sobre parceiros - sempre genérico:
    A gente trabalha com fundos parceiros e escolhe o melhor para cada operação.
    
    Se o cliente citar uma empresa (concorrente, parceiro, fornecedor):
    Silêncio estratégico. Redirecione para a necessidade DELE.
    Entendo! Me conta - o que seria ideal pra você em termos de prazo e volume?

    # CONSULTA DE PROSPECÇÃO
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