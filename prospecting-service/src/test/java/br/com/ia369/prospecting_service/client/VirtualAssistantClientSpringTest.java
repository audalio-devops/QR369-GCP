package br.com.ia369.prospecting_service.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {VirtualAssistantClient.class, org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration.class})
class VirtualAssistantClientSpringTest {

    @Autowired(required = false)
    private VirtualAssistantClient client;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testBeanCreation() {
        assertNotNull(client, "VirtualAssistantClient should be created by Spring");
    }
}
