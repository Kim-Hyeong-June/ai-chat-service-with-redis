package com.example.chatbot.domain.auth.controller;


import com.example.chatbot.domain.auth.dto.AuthRequestDto.LoginRequest;
import com.example.chatbot.domain.auth.dto.AuthRequestDto.SignupRequest;
import com.example.chatbot.domain.auth.dto.AuthResponseDto.TokenResponse;
import com.example.chatbot.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ✅ 회원가입
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TokenResponse> signup(@RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    // ✅ 로그인
    @PostMapping("/login")
    public Mono<TokenResponse> login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
