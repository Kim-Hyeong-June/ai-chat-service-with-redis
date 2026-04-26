package com.example.chatbot.domain.chat.repository;

import com.example.chatbot.domain.chat.entity.Message;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface MessageRepository extends ReactiveCrudRepository<Message, Long> {

    Flux<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
