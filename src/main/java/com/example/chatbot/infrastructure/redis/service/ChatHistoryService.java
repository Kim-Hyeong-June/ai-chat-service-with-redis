package com.example.chatbot.infrastructure.redis.service;

import com.example.chatbot.infrastructure.openai.dto.OpenAiRequestDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryService {
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PREFIX = "chat:";
    private static final Duration TTL = Duration.ofHours(24); // ✅ 24시간 TTL

    // ✅ 메시지 저장 (논블로킹)
//    public Mono<Void> saveMessages(String userId, List<OpenAiRequestDto.Message> messages) {
//        try {
//            String key = PREFIX + userId;
//            String value = objectMapper.writeValueAsString(messages);
//            return reactiveRedisTemplate.opsForValue()
//                    .set(key, value, TTL)
//                    .then();
//        } catch (Exception e) {
//            return Mono.error(new RuntimeException("Redis 저장 실패", e));
//        }
//    }

    public Mono<Void> saveMessages(String userId, List<OpenAiRequestDto.Message> messages) {
        try {
            String key = PREFIX + userId;
            String value = objectMapper.writeValueAsString(messages);

            return reactiveRedisTemplate.opsForValue()
                    .set(key, value, TTL)
                    .then();

        } catch (Exception e) {
            return Mono.error(new RuntimeException("Redis 저장 실패", e));
        }
    }

    // ✅ 메시지 조회 (논블로킹)
    public Mono<List<OpenAiRequestDto.Message>> getMessages(String userId) {
        String key = PREFIX + userId;
        return reactiveRedisTemplate.opsForValue()
                .get(key)
                .map(value -> {
                    try {
                        return objectMapper.<List<OpenAiRequestDto.Message>>readValue(
                                value,
                                new TypeReference<List<OpenAiRequestDto.Message>>() {}
                        );
                    } catch (Exception e) {
                        throw new RuntimeException("Redis 조회 실패", e);
                    }
                })
                .defaultIfEmpty(new ArrayList<>()); // ✅ 없으면 빈 리스트
    }
}
