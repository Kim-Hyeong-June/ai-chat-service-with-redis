package com.example.chatbot.domain.document.entity;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("documents")
@Getter
@Builder
public class Document {

    @Id
    private Long id;

    private Long userId;
    private String filename;
    private String filePath;
    private String status;      // UPLOADING | PROCESSING | READY | FAILED

    @CreatedDate
    private LocalDateTime createdAt;
}
