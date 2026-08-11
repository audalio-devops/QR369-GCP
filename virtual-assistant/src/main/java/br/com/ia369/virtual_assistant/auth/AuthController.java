package br.com.ia369.virtual_assistant.auth;

import br.com.ia369.virtual_assistant.auth.dto.LoginRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final String expectedUsername;
    private final String expectedPassword;

    public AuthController(
            @Value("${app.auth.username}") String expectedUsername,
            @Value("${app.auth.password}") String expectedPassword) {
        this.expectedUsername = expectedUsername;
        this.expectedPassword = expectedPassword;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
        if (expectedUsername.equals(request.username()) && expectedPassword.equals(request.password())) {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Login bem-sucedido"));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "Usuário ou senha inválido(s)"));
    }
}
