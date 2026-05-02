package com.example.chatbot.domain.auth.dto;


public class AuthResponseDto {
    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String email,
            String name
    ) {}
}
