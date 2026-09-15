package br.com.ia369.virtual_assistant.whatsapp;

import br.com.ia369.virtual_assistant.whatsapp.dto.ZApiWebhookPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebhookProtectionService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookProtectionService.class);

    // Expiração da deduplicação de mensagens (5 minutos)
    private static final long DEDUP_TTL_MS = 5 * 60 * 1000L;

    // Janela de rate limiting (1 minuto)
    private static final long RATE_LIMIT_WINDOW_MS = 60 * 1000L;

    // Limites de requisições por janela
    private static final int MAX_REQUESTS_PER_PHONE = 10;
    private static final int MAX_REQUESTS_GLOBAL = 50;

    private final Map<String, Long> processedMessageIds = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> phoneRequestTimestamps = new ConcurrentHashMap<>();
    private final Deque<Long> globalRequestTimestamps = new ArrayDeque<>();

    /**
     * Valida se a mensagem deve ser processada ou descartada por segurança.
     */
    public synchronized boolean isAllowed(ZApiWebhookPayload payload) {
        // 1. Filtrar auto-respostas (fromMe == true)
        if (payload.isFromMe()) {
            logger.info("⏩ [DEDUP/IGNORE] Mensagem descartada: enviada pelo próprio BOT (fromMe=true)");
            return false;
        }

        // 2. Filtrar mensagens de grupos (se configurado/padrão)
        if (payload.isGroupMessage()) {
            logger.info("⏩ [IGNORE] Mensagem descartada: vinda de grupo (isGroup=true)");
            return false;
        }

        // 3. Validar se o payload possui texto válido
        if (payload.phone() == null || payload.messageData() == null || payload.messageData().message() == null
                || payload.messageData().message().isBlank()) {
            logger.debug("⏩ [IGNORE] Mensagem descartada: payload sem texto ou telefone");
            return false;
        }

        long now = System.currentTimeMillis();

        // 4. Deduplicação por messageId (ou por hash telefone + texto se messageId for
        // nulo)
        String dedupKey = payload.messageId() != null && !payload.messageId().isBlank()
                ? payload.messageId()
                : payload.phone() + ":" + payload.messageData().message().hashCode();

        cleanOldDedupKeys(now);

        if (processedMessageIds.containsKey(dedupKey)) {
            logger.warn("⚠️ [DEDUP] Mensagem duplicada ignorada (key={})", dedupKey);
            return false;
        }

        // 5. Rate Limit Global (Sliding Window)
        cleanTimestampQueue(globalRequestTimestamps, now);
        if (globalRequestTimestamps.size() >= MAX_REQUESTS_GLOBAL) {
            logger.error("🛑 [RATE LIMIT GLOBAL EXCEDIDO] {} requisições no último minuto. Bloqueando chamada para IA.",
                    globalRequestTimestamps.size());
            return false;
        }

        // 6. Rate Limit Por Telefone (Sliding Window)
        Deque<Long> phoneTimestamps = phoneRequestTimestamps.computeIfAbsent(payload.phone(), k -> new ArrayDeque<>());
        cleanTimestampQueue(phoneTimestamps, now);
        if (phoneTimestamps.size() >= MAX_REQUESTS_PER_PHONE) {
            logger.warn(
                    "🛑 [RATE LIMIT POR TELEFONE EXCEDIDO] Telefone {} fez {} requisições no último minuto. Bloqueando.",
                    payload.phone(), phoneTimestamps.size());
            return false;
        }

        // Registrar a mensagem processada e o timestamp
        processedMessageIds.put(dedupKey, now);
        phoneTimestamps.addLast(now);
        globalRequestTimestamps.addLast(now);

        return true;
    }

    private void cleanOldDedupKeys(long now) {
        processedMessageIds.entrySet().removeIf(entry -> (now - entry.getValue()) > DEDUP_TTL_MS);
    }

    private void cleanTimestampQueue(Deque<Long> queue, long now) {
        while (!queue.isEmpty() && (now - queue.peekFirst()) > RATE_LIMIT_WINDOW_MS) {
            queue.pollFirst();
        }
    }
}
