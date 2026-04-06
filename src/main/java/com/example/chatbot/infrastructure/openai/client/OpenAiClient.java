package com.example.chatbot.infrastructure.openai.client;

import com.example.chatbot.infrastructure.openai.dto.OpenAiRequestDto;
import com.example.chatbot.infrastructure.openai.dto.OpenAiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiClient {

    private final WebClient webClient;

    @Value("${openai.api.key}")
    private String apiKey;

    // 🔥 변경: String → OpenAiRequestDto
    public String call(OpenAiRequestDto request) {

        try {
            OpenAiResponseDto response = webClient.post()
                    .uri("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(request)
                    .retrieve()

                    // 🔥 HTTP 에러 처리 (개선 버전)
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("OpenAI API 오류: status={}, body={}",
                                                clientResponse.statusCode(), errorBody);
                                        return Mono.error(new RuntimeException("OpenAI API 호출 실패"));
                                    }))

                    .bodyToMono(OpenAiResponseDto.class)
                    .block();

            // 🔥 null 방어
            if (response == null || response.getContent() == null) {
                log.error("OpenAI 응답이 비어있음");
                throw new RuntimeException("OpenAI 응답 없음");
            }

            return response.getContent();

        } catch (Exception e) {
            log.error("OpenAI 호출 중 예외 발생", e);
            throw new RuntimeException("OpenAI 호출 실패", e);
        }
    }
}
