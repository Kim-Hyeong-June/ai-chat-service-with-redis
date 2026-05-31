package com.example.chatbot.domain.document.service;


import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TikaService {
    private final Tika tika = new Tika();
    private static final int CHUNK_SIZE = 500;      // 청크 크기 (글자 수)
    private static final int CHUNK_OVERLAP = 50;    // 청크 겹침 (문맥)

    // ✅ 텍스트 추출
    public String extractText(InputStream inputStream) {
        try {
            return tika.parseToString(inputStream);
        } catch (Exception e) {
            log.error("텍스트 추출 실패: {}", e.getMessage());
            throw new RuntimeException("텍스트 추출 실패", e);
        }
    }

    // ✅ 청킹
    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            chunks.add(text.substring(start, end));
            start += CHUNK_SIZE - CHUNK_OVERLAP;
        }

        return chunks;
    }
}
