package br.com.ia369.prospecting_service.client;

import br.com.ia369.prospecting_service.client.dto.BrasilApiEmpresaDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Service
public class ApiCnpjClient {

    private final RestClient restClient;

    public ApiCnpjClient(RestClient brasilApiRestClient) {
        this.restClient = brasilApiRestClient;
    }

    public Optional<BrasilApiEmpresaDTO> fetchEmpresa(String cnpj) {
        try {
            BrasilApiEmpresaDTO empresa = restClient.get()
                    .uri("/api/cnpj/v1/{cnpj}", cnpj)
                    .retrieve()
                    .body(BrasilApiEmpresaDTO.class);
            return Optional.ofNullable(empresa);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }
}
