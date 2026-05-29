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

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiClient {

    private final WebClient webClient;

    @Value("${openai.api.key}")
    private String apiKey;

    private final ObjectMapper mapper = new ObjectMapper();

    public Flux<String> call(OpenAiRequestDto request) {

        return webClient.post()
                .uri("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()

                // 🔥 에러 처리
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("OpenAI API 오류: status={}, body={}",
                                            clientResponse.statusCode(), errorBody);
                                    return Mono.error(new RuntimeException("OpenAI API 호출 실패"));
                                }))

                // SSE를 안정적으로 받기 HTTP 스트리밍 body를 DataBuffer 조각 단위로 읽음
                // data buffer 로 받는 이유 SSE chunk 깨짐 대응 줄 단위 직접 처리
                // 메모리 관리 안정적 스트리밍 처리
                .bodyToFlux(DataBuffer.class)

                // DataBuffer → byte[] -> String
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer); // 🔥 메모리 누수 방지
                    //WebFlux Netty는 성능 때문에 메모리 풀(pool) 기반 버퍼를 씀 즉 GC가 자동 정리 안 할 수도 있음
                    //그래서 직접 release 해야 함.
                    return new String(bytes, StandardCharsets.UTF_8);
                })


                // 🔥 줄 단위로 분리 (SSE 구조 대응) 여러줄이 한 청크로 , 중간에 짤릴 수도 있음..
                .flatMap(chunk -> Flux.fromArray(chunk.split("\n")))

                // 🔥 "data:" 라인만 처리
                .map(line -> {
                    try {
                        if (!line.startsWith("data:")) return "";

                        String json = line.replace("data: ", "").trim();

                        if (json.equals("[DONE]")) return "";

                        JsonNode node = mapper.readTree(json);

                        // 🔥 안전한 파싱 (중요)
                        JsonNode contentNode = node
                                .path("choices")
                                .path(0)
                                .path("delta")
                                .path("content");
                        //path 쓰는 이유 : get() 쓰면 값 없을 때 null 나와서 NPE 터질 수 있음.

                        if (contentNode.isMissingNode()) return "";

                        return contentNode.asText();

                    } catch (Exception e) {
                        log.warn("파싱 실패: {}", line);
                        return "";
                    }
                })

                // 🔥 빈 문자열 제거
                .filter(content -> !content.isEmpty());
    }
}
