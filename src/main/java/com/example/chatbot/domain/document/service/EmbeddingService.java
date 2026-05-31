// src/main/java/com/example/chatbot/domain/document/service/EmbeddingService.java

package com.example.chatbot.domain.document.service;

import com.example.chatbot.domain.document.repository.DocumentChunkRepository;
import com.example.chatbot.infrastructure.openai.client.EmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final EmbeddingClient embeddingClient;
    private final DocumentChunkRepository documentChunkRepository;

    // ✅ 문서 청크 임베딩 생성 + 저장
    public Mono<Void> embedDocument(Long documentId) {
        return documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId)
                .flatMap(chunk ->
                        embeddingClient.embed(chunk.getContent())
                                .flatMap(vector -> {
                                    // float[] → "[0.1, 0.2, ...]" 형태로 변환
                                    String vectorStr = Arrays.stream(
                                                    toDoubleArray(vector))
                                            .mapToObj(String::valueOf)
                                            .collect(Collectors.joining(",", "[", "]"));

                                    return documentChunkRepository.updateEmbedding(chunk.getId(), vectorStr);
                                })
                )
                .then();
    }

    private double[] toDoubleArray(float[] floats) {
        double[] doubles = new double[floats.length];
        for (int i = 0; i < floats.length; i++) {
            doubles[i] = floats[i];
        }
        return doubles;
    }
}
