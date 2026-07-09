package br.com.ia369.virtual_assistant.cnpj;

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
public class CNPJTools {

    private static final Logger logger = LoggerFactory.getLogger(CNPJTools.class);

    private final RestClient restClient;

    public CNPJTools(@Value("${app.prospecting.base-url:http://localhost:8081}") String baseUrl) {
        // Timeouts explicitos para nao travar o chat caso o microservico de cnpj
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

    @Tool(description = "Consulta os dados de uma empresa a partir do CNPJ. Use quando o usuário informar um CNPJ.")
    public CNPJResponse consultarCNPJ(
            @ToolParam(description = "CNPJ da empresa, ex.: 60701190000104", required = true) String cnpj) {

        logger.info("Consultando dados para o CNPJ {}", cnpj);
        try {
            CNPJResponse response = restClient.get()
                    .uri("/cnpj/{cnpj}", cnpj)
                    .retrieve()
                    .body(CNPJResponse.class);

            logger.info("Dados encontrados para o CNPJ {}: {}", cnpj, response);
            return response;
        } catch (HttpClientErrorException.NotFound e) {
            logger.info("CNPJ {} nao encontrado no sistema de prospecção (404)", cnpj);
            throw new RuntimeException(
                    "CNPJ " + cnpj + " não encontrado no sistema de prospecção.");
        } catch (ResourceAccessException e) {
            // Timeout ou servico fora do ar.
            logger.error("Erro ao consultar o sistema de prospecção para o CNPJ {}: {}",
                    cnpj, e.getMessage());
            throw new RuntimeException("Não consegui consultar o sistema de prospecção agora.");
        }
    }
}
