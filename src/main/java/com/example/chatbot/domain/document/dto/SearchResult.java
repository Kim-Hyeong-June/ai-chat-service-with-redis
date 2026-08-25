package com.example.chatbot.domain.document.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SearchResult {
    private String content;
    private Double distance;
}
