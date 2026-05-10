package com.example.chatbot.domain.chat.service;


import com.example.chatbot.domain.chat.dto.ConversationDto.ConversationResponse;
import com.example.chatbot.domain.chat.dto.ConversationDto.CreateRequest;
import com.example.chatbot.domain.chat.entity.Conversation;
import com.example.chatbot.domain.chat.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationRepository conversationRepository;

    // ✅ 대화방 생성
    public Mono<ConversationResponse> createConversation(Long userId, CreateRequest request) {
        Conversation conversation = Conversation.builder()
                .userId(userId)
                .title(request.title())
                .build();

        return conversationRepository.save(conversation)
                .map(this::toResponse);
    }

    // ✅ 대화방 목록 조회
    public Flux<ConversationResponse> getConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .map(this::toResponse);
    }

    // ✅ 대화방 삭제
    public Mono<Void> deleteConversation(Long userId, Long conversationId) {
        return conversationRepository.findById(conversationId)
                .flatMap(conversation -> {
                    if (!conversation.getUserId().equals(userId)) {
                        return Mono.error(new IllegalArgumentException("삭제 권한이 없습니다."));
                    }
                    return conversationRepository.delete(conversation);
                });
    }

    private ConversationResponse toResponse(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
}
