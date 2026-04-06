package com.example.chatbot.infrastructure.openai.dto;
import java.util.List;

public record OpenAiResponseDto(
        List<Choice> choices
) {
    public String getContent() {
        return choices.get(0).message().content();
    }

    public record Choice(Message message) {}
    public record Message(String role, String content) {}
}

