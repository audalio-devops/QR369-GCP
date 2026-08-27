package br.com.ia369.prospecting_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Serviço responsável por sortear e carregar as mensagens para contadores.
 *
 * Lógica:
 * 1. Sortear X entre 1 e {@code messageCount} (padrão: 5)
 * 2. Carregar classpath:messages/msg_contador_{X}.txt
 * 3. Retornar o texto da mensagem
 */
@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final int messageCount;
    private final Random random = new Random();

    public MessageService(@Value("${prospecting.message.count:5}") int messageCount) {
        this.messageCount = messageCount;
    }

    /**
     * Sorteia uma mensagem aleatória dentre as disponíveis para contadores.
     *
     * @return conteúdo da mensagem sorteada
     * @throws RuntimeException se o arquivo não puder ser lido
     */
    public String sortearMensagem() {
        int x = random.nextInt(messageCount) + 1; // 1 a messageCount
        String caminho = "messages/msg_contador_" + x + ".txt";
        log.info("Mensagem sorteada: {} (arquivo: {})", x, caminho);

        try {
            ClassPathResource resource = new ClassPathResource(caminho);
            byte[] bytes = resource.getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("Falha ao carregar arquivo de mensagem '{}': {}", caminho, ex.getMessage(), ex);
            throw new RuntimeException("Não foi possível carregar a mensagem: " + caminho, ex);
        }
    }
}
