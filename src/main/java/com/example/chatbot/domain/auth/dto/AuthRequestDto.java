package com.example.chatbot.domain.auth.dto;


public class AuthRequestDto {
    public record SignupRequest(
            String email,
            String password,
            String name
    ) {}

    public record LoginRequest(
            String email,
            String password
    ) {}
}
