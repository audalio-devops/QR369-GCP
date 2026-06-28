package br.com.ia369.virtual_assistant.whatsapp.dto;

public record SendMessageRequest(
    String phone,
    String message
) {}
