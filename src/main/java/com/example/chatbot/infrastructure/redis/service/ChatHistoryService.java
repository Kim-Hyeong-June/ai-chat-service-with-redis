package com.example.chatbot.infrastructure.redis.service;

import com.example.chatbot.infrastructure.openai.dto.OpenAiRequestDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PREFIX = "chat:";

    public void saveMessages(String userId, List<OpenAiRequestDto.Message> messages) {
        try {
            String key = PREFIX + userId;
            String value = objectMapper.writeValueAsString(messages);
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            throw new RuntimeException("Redis 저장 실패", e);
        }
    }

    public List<OpenAiRequestDto.Message> getMessages(String userId) {
        try {
            String key = PREFIX + userId;
            String value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                return new ArrayList<>();
            }

            return objectMapper.readValue(
                    value,
                    new TypeReference<List<OpenAiRequestDto.Message>>() {}
            );

        } catch (Exception e) {
            throw new RuntimeException("Redis 조회 실패", e);
        }
    }
}
