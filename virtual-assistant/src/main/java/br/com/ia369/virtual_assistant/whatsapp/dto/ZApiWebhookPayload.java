package br.com.ia369.virtual_assistant.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ZApiWebhookPayload(
        @JsonProperty("phone") String phone,

        @JsonProperty("fromMe") Boolean fromMe,

        @JsonProperty("messageId") String messageId,

        @JsonProperty("isGroup") Boolean isGroup,

        @JsonProperty("status") String status,

        @JsonProperty("text") MessageData messageData) {
    public record MessageData(
            String message) {
    }

    public boolean isFromMe() {
        return Boolean.TRUE.equals(fromMe);
    }

    public boolean isGroupMessage() {
        return Boolean.TRUE.equals(isGroup);
    }
}
