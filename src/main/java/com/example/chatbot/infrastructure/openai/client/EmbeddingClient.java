// src/main/java/com/example/chatbot/infrastructure/openai/client/EmbeddingClient.java

package com.example.chatbot.infrastructure.openai.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmbeddingClient {

    private final WebClient webClient;

    @Value("${openai.api-key}")
    private String apiKey;

    // ✅ 텍스트 → 벡터 변환
    public Mono<float[]> embed(String text) {

        log.info("apiKey null={}", apiKey == null);

        if (apiKey != null) {
            log.info("apiKey prefix={}", apiKey.substring(0, 25));
            log.info("apiKey length={}", apiKey.length());
        }

        Map<String, Object> request = Map.of(
                "model", "text-embedding-3-small",
                "input", text
        );

        return webClient.post()
                .uri("https://api.openai.com/v1/embeddings")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("OpenAI Error Body={}", body);
                                    return Mono.error(new RuntimeException(body));
                                })
                )
                .bodyToMono(Map.class)
                .map(response -> {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                    List<Double> embedding = (List<Double>) data.get(0).get("embedding");

                    float[] vector = new float[embedding.size()];
                    for (int i = 0; i < embedding.size(); i++) {
                        vector[i] = embedding.get(i).floatValue();
                    }

                    return vector;
                });
    }
}
