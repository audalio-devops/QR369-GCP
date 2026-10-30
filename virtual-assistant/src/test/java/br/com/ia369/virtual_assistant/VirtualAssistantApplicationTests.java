package br.com.ia369.virtual_assistant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("anthropic")
// VA_DB_PASSWORD e LOGIN_PWD devem ser fornecidas pelo ambiente.
// application.yml resolve essas credenciais sem sobrescritas neste teste.
@TestPropertySource(properties = {
        "VA_SERVER_PORT=8080",
        "VA_DB_URL=jdbc:postgresql://localhost:5432/ragdb",
        "VA_DB_USERNAME=dbrag369",
        "LOGIN_USR=admin",
        "ANTHROPIC_API_KEY=test-key",
        "GEMINI_API_KEY=test-key",
        "SPRING_AI_OLLAMA_BASE_URL=http://localhost:11434",
        "ZAPI_INSTANCE_ID=test-inst",
        "ZAPI_TOKEN=test-token",
        "ZAPI_CLIENT_TOKEN=test-client-token"
})
class VirtualAssistantApplicationTests {

	@Test
	void contextLoads() {
	}

}
