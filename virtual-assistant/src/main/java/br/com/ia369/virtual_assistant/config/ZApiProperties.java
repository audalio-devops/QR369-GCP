package br.com.ia369.virtual_assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.zapi")
public record ZApiProperties(
    String apiUrl,
    String sendMessageEndpoint
) {}
