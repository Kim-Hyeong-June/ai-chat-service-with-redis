package com.example.chatbot.domain.chat.dto;


import java.time.LocalDateTime;

public class ConversationDto {

    public record CreateRequest(String title) {}

    public record ConversationResponse(
            Long id,
            String title,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
