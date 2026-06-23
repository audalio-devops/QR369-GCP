package br.com.ia369.virtual_assistant.ferias;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class FeriasTools {

    private static final Logger logger = LoggerFactory.getLogger(FeriasTools.class);

    private final RestClient restClient;

    public FeriasTools(@Value("${app.vacation.base-url:http://localhost:8081}") String baseUrl) {
        // Timeouts explicitos para nao travar o chat caso o microservico de ferias
        // esteja lento ou indisponivel (connect 3s, read 5s).
        HttpClientSettings settings = HttpClientSettings
                .defaults()
                .withConnectTimeout(Duration.ofSeconds(3))
                .withReadTimeout(Duration.ofSeconds(5));
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Tool(description = "Consulta os períodos e a quantidade de dias de férias disponíveis de um colaborador a partir da matrícula. Use quando o colaborador perguntar quando são as férias dele, quando ele pode tirar, ou quantos dias tem.")
    public FeriasResponse consultarFerias(
            @ToolParam(description = "Matrícula do colaborador, ex.: 1001", required = true) String matricula) {

        logger.info("Consultando ferias para a matricula {}", matricula);
        try {
            FeriasResponse response = restClient.get()
                    .uri("/ferias/{matricula}", matricula)
                    .retrieve()
                    .body(FeriasResponse.class);

            logger.info("Ferias encontradas para a matricula {}: {}", matricula, response);
            return response;
        } catch (HttpClientErrorException.NotFound e) {
            logger.info("Matricula {} nao encontrada no sistema de ferias (404)", matricula);
            throw new RuntimeException(
                    "Matrícula " + matricula + " não encontrada no sistema de férias.");
        } catch (ResourceAccessException e) {
            // Timeout ou servico fora do ar.
            logger.error("Erro ao consultar o sistema de ferias para a matricula {}: {}",
                    matricula, e.getMessage());
            throw new RuntimeException("Não consegui consultar o sistema de férias agora.");
        }
    }
}
