package com.example.chatbot.domain.document.dto;

import java.time.LocalDateTime;

public class DocumentDto {
    public record DocumentResponse(
            Long id,
            String filename,
            String status,
            LocalDateTime createdAt
    ) {}
}
