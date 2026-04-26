package com.example.chatbot.domain.chat.repository;

import com.example.chatbot.domain.chat.entity.Conversation;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ConversationRepository extends ReactiveCrudRepository<Conversation, Long> {

    Flux<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
