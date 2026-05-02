package com.example.chatbot.domain.auth.service;

import com.example.chatbot.domain.auth.dto.AuthRequestDto.LoginRequest;
import com.example.chatbot.domain.auth.dto.AuthRequestDto.SignupRequest;
import com.example.chatbot.domain.auth.dto.AuthResponseDto.TokenResponse;
import com.example.chatbot.domain.user.entity.User;
import com.example.chatbot.domain.user.repository.UserRepository;
import com.example.chatbot.global.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // ✅ 회원가입
    public Mono<TokenResponse> signup(SignupRequest request) {
        return userRepository.existsByEmail(request.email())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException("이미 사용중인 이메일입니다."));
                    }

                    User user = User.builder()
                            .email(request.email())
                            .password(passwordEncoder.encode(request.password()))
                            .name(request.name())
                            .build();

                    return userRepository.save(user);
                })
                .map(savedUser -> new TokenResponse(
                        jwtUtil.generateAccessToken(savedUser.getId(), savedUser.getEmail()),
                        jwtUtil.generateRefreshToken(savedUser.getId()),
                        savedUser.getEmail(),
                        savedUser.getName()
                ));
    }

    // ✅ 로그인
    public Mono<TokenResponse> login(LoginRequest request) {
        return userRepository.findByEmail(request.email())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("존재하지 않는 이메일입니다.")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                        return Mono.error(new IllegalArgumentException("비밀번호가 올바르지 않습니다."));
                    }

                    return Mono.just(new TokenResponse(
                            jwtUtil.generateAccessToken(user.getId(), user.getEmail()),
                            jwtUtil.generateRefreshToken(user.getId()),
                            user.getEmail(),
                            user.getName()
                    ));
                });
    }
}
