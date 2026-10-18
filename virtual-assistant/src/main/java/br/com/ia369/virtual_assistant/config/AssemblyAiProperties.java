package br.com.ia369.virtual_assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.assemblyai")
public record AssemblyAiProperties(
    String apiKey,
    String languageCode,
    Long retryDelayMs
) {
    public AssemblyAiProperties {
        if (languageCode == null || languageCode.isBlank()) {
            languageCode = "pt";
        }
        if (retryDelayMs == null || retryDelayMs <= 0) {
            retryDelayMs = 10000L;
        }
    }
}
