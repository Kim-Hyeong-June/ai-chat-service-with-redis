package com.example.chatbot.infrastructure.openai.client;

import com.example.chatbot.infrastructure.openai.dto.OpenAiRequestDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiClient {

    private final WebClient webClient;

    @Value("${openai.api-key}")
    private String apiKey;

    private final ObjectMapper mapper = new ObjectMapper();

    public Flux<String> call(OpenAiRequestDto request) {

        StringBuilder sseBuffer = new StringBuilder();

        return webClient.post()
                .uri("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()

                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("OpenAI Error={}", body);
                                    return Mono.error(new RuntimeException(body));
                                })
                )

                .bodyToFlux(DataBuffer.class)

                .map(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);

                    return new String(bytes, StandardCharsets.UTF_8);
                })

                .flatMap(chunk -> {

                    sseBuffer.append(chunk);

                    List<String> events = new ArrayList<>();

                    // 버퍼 안에 완성된 SSE 이벤트를
                    // 모두 꺼낼 때까지 while
                    while (true) {

                        int endIndex = sseBuffer.indexOf("\n\n");

                        if (endIndex == -1) {
                            break;
                        }

                        String event =
                                sseBuffer.substring(0, endIndex);

                        sseBuffer.delete(0, endIndex + 2);

                        events.add(event);
                    }

                    return Flux.fromIterable(events);
                })

                .map(event -> {

                    try {

                        if (!event.startsWith("data:")) {
                            return "";
                        }

                        String json =
                                event.replaceFirst("data:\\s*", "");

                        if ("[DONE]".equals(json)) {
                            return "";
                        }

                        JsonNode node =
                                mapper.readTree(json);

                        return node
                                .path("choices")
                                .path(0)
                                .path("delta")
                                .path("content")
                                .asText("");

                    } catch (Exception e) {

                        log.warn("SSE Parse Error={}", event);

                        return "";
                    }
                })

                .filter(content -> !content.isBlank());
    }
}
