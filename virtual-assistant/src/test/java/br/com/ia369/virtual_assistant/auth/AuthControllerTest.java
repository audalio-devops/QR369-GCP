package br.com.ia369.virtual_assistant.auth;

import br.com.ia369.virtual_assistant.auth.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthControllerTest {

    @Test
    void testLoginSuccess() {
        AuthController controller = new AuthController("testuser", "testpass");
        ResponseEntity<Map<String, String>> response = controller.login(new LoginRequest("testuser", "testpass"));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("success", response.getBody().get("status"));
    }

    @Test
    void testLoginFailure() {
        AuthController controller = new AuthController("testuser", "testpass");
        ResponseEntity<Map<String, String>> response = controller.login(new LoginRequest("wronguser", "testpass"));
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Usuário ou senha inválido(s)", response.getBody().get("error"));
    }
}
