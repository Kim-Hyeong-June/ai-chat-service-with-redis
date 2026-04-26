package com.example.chatbot.domain.document.entity;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;


@Table("document_chunks")
@Getter
@Builder
public class DocumentChunk {
    @Id
    private Long id;

    private Long documentId;
    private String content;
    private Integer chunkIndex;
    private String metadata;    // JSONB → String으로 처리

    // embedding은 R2DBC가 vector 타입 미지원
    // → 벡터 검색은 Native Query로 따로 처리

    @CreatedDate
    private LocalDateTime createdAt;
}
