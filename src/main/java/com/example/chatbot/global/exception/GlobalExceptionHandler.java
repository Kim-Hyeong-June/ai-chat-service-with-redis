package com.example.chatbot.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // OpenAI 에러
    @ExceptionHandler(OpenAiException.class)
    public ResponseEntity<?> handleOpenAiException(OpenAiException e) {
        log.error("OpenAI 오류", e);

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of(
                        "error", "OPENAI_ERROR",
                        "message", e.getMessage()
                ));
    }

    // 전체 에러
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        log.error("서버 오류", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "INTERNAL_SERVER_ERROR",
                        "message", e.getMessage()
                ));
    }
}
