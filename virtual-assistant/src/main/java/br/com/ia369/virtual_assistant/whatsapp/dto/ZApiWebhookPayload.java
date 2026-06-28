package br.com.ia369.virtual_assistant.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ZApiWebhookPayload(
    @JsonProperty("phone")
    String phone,
    @JsonProperty("text")
    MessageData messageData
) {
    public record MessageData(
        String message
    ) {}
}
