package com.example.chatbot.domain.chat.service;
import com.example.chatbot.domain.chat.entity.Message;
import com.example.chatbot.domain.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;



@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;

    // ✅ 메시지 저장
    public Mono<Message> saveMessage(Long conversationId, String role, String content) {
        Message message = Message.builder()
                .conversationId(conversationId)
                .role(role)
                .content(content)
                .build();
        return messageRepository.save(message);
    }

    // ✅ 대화 메시지 목록 조회
    public Flux<Message> getMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }
}
