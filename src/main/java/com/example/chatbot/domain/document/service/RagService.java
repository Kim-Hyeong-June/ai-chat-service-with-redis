package com.example.chatbot.domain.document.service;

import com.example.chatbot.domain.document.dto.SearchResult;
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
        SELECT
        dc.content,
        dc.embedding <=> CAST(:embedding AS vector) AS distance
        FROM document_chunks dc
        JOIN documents d
          ON dc.document_id = d.id
        WHERE d.user_id = :userId
        ORDER BY distance
        LIMIT 3
        """)
                            .bind("userId", userId)
                            .bind("embedding", vectorStr)
                            .map((row, metadata) ->
                                    new SearchResult(
                                            row.get("content", String.class),
                                            row.get("distance", Double.class)
                                    )
                            )
                            .all()
                            .collectList()
                            .map(results -> {

                                if (results.isEmpty()) {
                                    return "";
                                }
                                // 가장 유사한 청크
                                double topDistance = results.get(0).getDistance();

                                if (topDistance > 0.6) {
                                    log.info("관련 문서 없음 distance={}", topDistance);
                                    return "";
                                }

                                return results.stream()
                                        .map(SearchResult::getContent)
                                        .collect(Collectors.joining("\n\n"));
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

