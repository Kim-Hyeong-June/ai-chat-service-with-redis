package com.example.chatbot.domain.chat.controller;

import com.example.chatbot.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
            Authentication authentication,  // ✅ JWT에서 자동 추출
            @RequestParam String message
    ) {
        Long userId = (Long) authentication.getPrincipal(); // ✅ JwtAuthFilter에서 넣은 userId
        return chatService.chatStream(String.valueOf(userId), message);
    }
}
