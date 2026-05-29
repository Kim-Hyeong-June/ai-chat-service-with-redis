package com.example.chatbot.domain.document.repository;


import com.example.chatbot.domain.document.entity.DocumentChunk;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DocumentChunkRepository extends ReactiveCrudRepository<DocumentChunk, Long> {

    Flux<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    // ✅ 벡터 저장 Native Query
    @Modifying
    @Query("UPDATE document_chunks SET embedding = CAST(:embedding AS vector) WHERE id = :id")
    Mono<Void> updateEmbedding(Long id, String embedding);

}

