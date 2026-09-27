package br.com.ia369.virtual_assistant.config;

import com.assemblyai.api.AssemblyAI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssemblyAiConfig {

    private static final Logger logger = LoggerFactory.getLogger(AssemblyAiConfig.class);

    @Bean
    public AssemblyAI assemblyAI(AssemblyAiProperties properties) {
        String apiKey = properties.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("⚠️ [ASSEMBLYAI] app.assemblyai.api-key não configurada. A transcrição de áudio não funcionará até que a chave seja informada.");
            return null;
        }
        logger.info("🔑 [ASSEMBLYAI] Cliente AssemblyAI configurado com sucesso.");
        return AssemblyAI.builder().apiKey(apiKey).build();
    }
}
