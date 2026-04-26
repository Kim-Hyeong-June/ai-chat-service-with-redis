package com.example.chatbot.domain.document.repository;

import com.example.chatbot.domain.document.entity.Document;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface DocumentRepository extends ReactiveCrudRepository<Document, Long> {

    Flux<Document> findByUserId(Long userId);
}
