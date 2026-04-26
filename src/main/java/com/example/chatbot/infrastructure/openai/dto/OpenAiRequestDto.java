package com.example.chatbot.infrastructure.openai.dto;

import java.util.List;

public record OpenAiRequestDto(
        String model,
        List<Message> messages,
        boolean stream // stream 옵션이 있는 OpenAI API 를 호출하면 SSE로 응답이 오는 것
) {
    public static OpenAiRequestDto of(String model, List<Message> messages) {
        return new OpenAiRequestDto(model , messages , false);
    }

    public static OpenAiRequestDto ofStream(String model, List<Message> messages) {
        return new OpenAiRequestDto(model , messages , true);
    }
    // 🔥 단일 테스트용 (선택)
    public static OpenAiRequestDto ofSingle(String model, String userMessage) {
        return new OpenAiRequestDto(
                model,
                List.of(new Message("user", userMessage)),
                false
        );
    }

    public record Message(String role, String content) {}
}
