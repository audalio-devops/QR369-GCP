package br.com.ia369.prospecting_service.client;

import br.com.ia369.prospecting_service.dto.BrasilApiCnpjDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Cliente HTTP para a BrasilAPI (endpoint de CNPJ).
 * Usa o RestClient do Spring Boot 3.x.
 *
 * Propriedade configurável:
 * brasilapi.base-url (padrão: https://brasilapi.com.br)
 */
@Component
public class BrasilApiClient {

    private static final Logger log = LoggerFactory.getLogger(BrasilApiClient.class);

    private final RestClient restClient;

    public BrasilApiClient(@Value("${brasilapi.base-url:https://brasilapi.com.br}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Consulta o CNPJ na BrasilAPI.
     *
     * @param cnpj 14 dígitos, sem máscara
     * @return Optional com os dados da empresa, ou empty se não encontrado / erro
     */
    public Optional<BrasilApiCnpjDTO> buscarCnpj(String cnpj) {
        log.info("Consultando BrasilAPI para CNPJ: {}", cnpj);
        try {
            BrasilApiCnpjDTO response = restClient
                    .get()
                    .uri("/api/cnpj/v1/{cnpj}", cnpj)
                    .retrieve()
                    .body(BrasilApiCnpjDTO.class);

            return Optional.ofNullable(response);

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("CNPJ {} não encontrado na BrasilAPI (404).", cnpj);
            } else {
                log.error("Erro HTTP ao consultar BrasilAPI para CNPJ {}: {} - {}",
                        cnpj, ex.getStatusCode(), ex.getMessage());
            }
            return Optional.empty();
        } catch (Exception ex) {
            log.error("Falha inesperada ao consultar BrasilAPI para CNPJ {}: {}", cnpj, ex.getMessage(), ex);
            return Optional.empty();
        }
    }
}
