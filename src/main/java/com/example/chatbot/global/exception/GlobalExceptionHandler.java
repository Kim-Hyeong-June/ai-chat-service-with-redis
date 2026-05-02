package com.example.chatbot.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@Component
@Order(-2)
@Slf4j
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {

        log.error("에러 발생: {}", ex.getMessage());

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String errorCode = "INTERNAL_SERVER_ERROR";

        if (ex instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
            errorCode = "BAD_REQUEST";
        } else if (ex instanceof OpenAiException) {
            status = HttpStatus.BAD_GATEWAY;
            errorCode = "OPENAI_ERROR";
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(Map.of(
                    "error", errorCode,
                    "message", ex.getMessage()
            ));
            DataBuffer buffer = exchange.getResponse()
                    .bufferFactory()
                    .wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
