package com.example.chatbot.domain.chat.controller;

import com.example.chatbot.domain.chat.dto.ChatRequest;
import com.example.chatbot.domain.chat.dto.ChatResponse;
import com.example.chatbot.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {

        String answer = chatService.chat(
                request.userId(),
                request.message()
        );

        return new ChatResponse(answer);
    }
}
