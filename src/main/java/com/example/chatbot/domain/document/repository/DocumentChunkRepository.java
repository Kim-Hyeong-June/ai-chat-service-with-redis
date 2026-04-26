package com.example.chatbot.domain.document.repository;


import com.example.chatbot.domain.document.entity.DocumentChunk;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface DocumentChunkRepository extends ReactiveCrudRepository<DocumentChunk, Long> {

    Flux<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);
}

