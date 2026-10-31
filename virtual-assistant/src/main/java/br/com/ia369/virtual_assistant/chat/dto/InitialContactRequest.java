package br.com.ia369.virtual_assistant.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InitialContactRequest(
        @JsonProperty("phone") String phone,
        @JsonProperty("message") String message,
        @JsonProperty("conversationId") String conversationId
) {
    public String targetId() {
        if (phone != null && !phone.isBlank()) {
            return phone;
        }
        return conversationId;
    }
}
