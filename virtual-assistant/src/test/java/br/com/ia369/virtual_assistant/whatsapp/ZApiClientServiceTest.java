package br.com.ia369.virtual_assistant.whatsapp;

import br.com.ia369.virtual_assistant.config.ZApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ZApiClientServiceTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private ZApiProperties zApiProperties;
    private ZApiClientService zApiClientService;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        zApiProperties = new ZApiProperties(
                "https://api.z-api.io/instances/INSTANCE123/token/TOKEN123",
                "/send-text",
                "CLIENT_TOKEN_XYZ"
        );

        zApiClientService = new ZApiClientService(zApiProperties, restClientBuilder);
    }

    @Test
    void shouldSendMessageSuccessfully() {
        mockServer.expect(requestTo("https://api.z-api.io/instances/INSTANCE123/token/TOKEN123/send-text"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Client-Token", "CLIENT_TOKEN_XYZ"))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.phone").value("5511999999999"))
                .andExpect(jsonPath("$.message").value("Olá, tudo bem?"))
                .andRespond(withSuccess("{\"zaapId\":\"123\",\"messageId\":\"456\"}", MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> zApiClientService.sendMessage("5511999999999", "Olá, tudo bem?"));

        mockServer.verify();
    }

    @Test
    void shouldHandleEmptyOrNullInputGracefully() {
        // Should not make any HTTP calls
        zApiClientService.sendMessage(null, "Olá");
        zApiClientService.sendMessage("", "Olá");
        zApiClientService.sendMessage("5511999999999", null);
        zApiClientService.sendMessage("5511999999999", "");

        mockServer.verify();
    }

    @Test
    void shouldThrowWhenZApiReturnsError() {
        mockServer.expect(requestTo("https://api.z-api.io/instances/INSTANCE123/token/TOKEN123/send-text"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest().body("{\"error\":\"your client-token is not configured\"}"));

        assertThrows(RestClientResponseException.class, () ->
                zApiClientService.sendMessage("5511999999999", "Olá"));

        mockServer.verify();
    }
}
