package br.com.ia369.virtual_assistant.cnpj;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@RestController
public class CnpjProxyController {

    private final RestClient restClient;

    public CnpjProxyController(
            @Value("${app.prospecting.base-url:http://localhost:8081}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @GetMapping("/cnpj/{cnpj}")
    public ResponseEntity<String> consultarCNPJ(@PathVariable String cnpj) {
        try {
            String response = restClient.get()
                    .uri("/cnpj/{cnpj}", cnpj)
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException.NotFound e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Erro ao consultar o serviço de prospecção: " + e.getMessage() + "\"}");
        }
    }
}
