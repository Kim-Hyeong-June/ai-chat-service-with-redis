package com.example.chatbot.domain.chat.entity;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("messages")
@Getter
@Builder
public class Message {

    @Id
    private Long id;

    private Long conversationId;
    private String role;        // user | assistant | system
    private String content;
    private Integer tokens;

    @CreatedDate
    private LocalDateTime createdAt;
}
