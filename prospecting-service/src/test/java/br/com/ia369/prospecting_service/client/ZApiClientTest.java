package br.com.ia369.prospecting_service.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest(ZApiClient.class)
@TestPropertySource(properties = {
        "zapi.base-url=https://api.z-api.io",
        "zapi.instance=test-instance",
        "zapi.token=test-token",
        "zapi.client-token=test-client-token"
})
class ZApiClientTest {

    @Autowired
    private ZApiClient zApiClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    @DisplayName("Deve retornar true quando Z-API responder connected=true no GET /status")
    void testIsConnectedRetornaTrue() {
        server.expect(requestTo("https://api.z-api.io/instances/test-instance/token/test-token/status"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Client-Token", "test-client-token"))
                .andRespond(withSuccess("{\"connected\": true}", MediaType.APPLICATION_JSON));

        boolean connected = zApiClient.isConnected();

        assertTrue(connected);
        server.verify();
    }

    @Test
    @DisplayName("Deve retornar false quando Z-API responder connected=false no GET /status")
    void testIsConnectedRetornaFalse() {
        server.expect(requestTo("https://api.z-api.io/instances/test-instance/token/test-token/status"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Client-Token", "test-client-token"))
                .andRespond(withSuccess("{\"connected\": false}", MediaType.APPLICATION_JSON));

        boolean connected = zApiClient.isConnected();

        assertFalse(connected);
        server.verify();
    }

    @Test
    @DisplayName("Deve retornar false e capturar exceção se Z-API responder com erro HTTP")
    void testIsConnectedRetornaFalseEmErroHttp() {
        server.expect(requestTo("https://api.z-api.io/instances/test-instance/token/test-token/status"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Client-Token", "test-client-token"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        boolean connected = zApiClient.isConnected();

        assertFalse(connected);
        server.verify();
    }
}
