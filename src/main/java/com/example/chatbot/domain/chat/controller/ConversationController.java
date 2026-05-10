package com.example.chatbot.domain.chat.controller;

import com.example.chatbot.domain.chat.dto.ConversationDto.ConversationResponse;
import com.example.chatbot.domain.chat.dto.ConversationDto.CreateRequest;
import com.example.chatbot.domain.chat.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    // ✅ 대화방 생성
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ConversationResponse> createConversation(@RequestBody CreateRequest request) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (Long) ctx.getAuthentication().getPrincipal())
                .flatMap(userId -> conversationService.createConversation(userId, request));
    }

    // ✅ 대화방 목록
    @GetMapping
    public Flux<ConversationResponse> getConversations() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (Long) ctx.getAuthentication().getPrincipal())
                .flatMapMany(userId -> conversationService.getConversations(userId));
    }

    // ✅ 대화방 삭제
    @DeleteMapping("/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteConversation(@PathVariable Long conversationId) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (Long) ctx.getAuthentication().getPrincipal())
                .flatMap(userId -> conversationService.deleteConversation(userId, conversationId));
    }
}
