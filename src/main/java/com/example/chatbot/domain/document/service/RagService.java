package com.example.chatbot.domain.document.service;

import com.example.chatbot.infrastructure.openai.client.EmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final EmbeddingClient embeddingClient;
    private final DatabaseClient databaseClient;

    // ✅ 유사도 검색 → 관련 청크 추출
    public Mono<String> retrieveContext(String question, Long userId) {
        return embeddingClient.embed(question)
                .flatMap(vector -> {

                    // float[] → "[0.1, 0.2, ...]" 변환
                    String vectorStr = Arrays.stream(toDoubleArray(vector))
                            .mapToObj(String::valueOf)
                            .collect(Collectors.joining(",", "[", "]"));

                    // ✅ pgvector 유사도 검색 (코사인 거리)
                    return databaseClient.sql("""
                            SELECT dc.content
                            FROM document_chunks dc
                            JOIN documents d ON dc.document_id = d.id
                            WHERE d.user_id = :userId
                              AND dc.embedding IS NOT NULL
                            ORDER BY dc.embedding <=> CAST(:embedding AS vector)
                            LIMIT 3
                            """)
                            .bind("userId", userId)
                            .bind("embedding", vectorStr)
                            .map((row, metadata) -> row.get("content", String.class))
                            .all()
                            .collectList()
                            .map(chunks -> {
                                if (chunks.isEmpty()) return "";

                                String context = String.join("\n\n", chunks);
                                return context;
                            });
                });
    }

    private double[] toDoubleArray(float[] floats) {
        double[] doubles = new double[floats.length];
        for (int i = 0; i < floats.length; i++) {
            doubles[i] = floats[i];
        }
        return doubles;
    }
}

