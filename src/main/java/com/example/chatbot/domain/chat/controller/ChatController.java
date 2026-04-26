package com.example.chatbot.domain.chat.controller;

import com.example.chatbot.domain.chat.dto.ChatRequest;
import com.example.chatbot.domain.chat.dto.ChatResponse;
import com.example.chatbot.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;


@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // 🔥 Streaming API
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
            @RequestParam String userId,
            @RequestParam String message
    ) {
        return chatService.chatStream(userId, message);
    }
}
