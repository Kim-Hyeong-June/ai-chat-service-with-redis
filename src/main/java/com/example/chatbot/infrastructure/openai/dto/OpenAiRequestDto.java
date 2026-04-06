package com.example.chatbot.infrastructure.openai.dto;

import java.util.List;

public record OpenAiRequestDto(
        String model,
        List<Message> messages
) {
    public static OpenAiRequestDto of(String model, String userMessage) {
        return new OpenAiRequestDto(
                model,
                List.of(new Message("user", userMessage))
        );
    }

    public record Message(String role, String content) {}
}
