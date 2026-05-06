package com.example.chatbot.global.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter implements WebFilter {

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        log.info("JwtAuthFilter 진입"); // ✅ 추가

        String token = extractToken(exchange);

        log.info("token={}", token); // ✅ 추가

        if (token != null && jwtUtil.validateToken(token)) {
            Authentication auth = getAuthentication(token);
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
        }

        log.info("토큰 없음 → 그냥 통과"); // ✅ 추가
        return chain.filter(exchange);
    }


    // ✅ Authorization 헤더에서 토큰 추출
    private String extractToken(ServerWebExchange exchange) {
        String header = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    // ✅ Authentication 객체 생성
    private Authentication getAuthentication(String token) {
        Long userId = jwtUtil.getUserId(token);
        String email = jwtUtil.getEmail(token);

        return new UsernamePasswordAuthenticationToken(
                userId,         // principal → userId로 설정
                email,          // credentials → email
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
