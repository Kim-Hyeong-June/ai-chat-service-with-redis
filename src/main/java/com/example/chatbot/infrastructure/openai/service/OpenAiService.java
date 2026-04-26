package com.example.chatbot.infrastructure.openai.service;

import com.example.chatbot.infrastructure.openai.client.OpenAiClient;
import com.example.chatbot.infrastructure.openai.dto.OpenAiRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final OpenAiClient openAiClient;

    public Flux<String> ask(List<OpenAiRequestDto.Message> messages) {

        OpenAiRequestDto request =
                OpenAiRequestDto.ofStream("gpt-4.1-mini", messages); // 🔥 stream=true

        return openAiClient.call(request);
    }
}
