package com.example.chatbot.infrastructure.openai.service;

import com.example.chatbot.infrastructure.openai.client.OpenAiClient;
import com.example.chatbot.infrastructure.openai.dto.OpenAiRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final OpenAiClient openAiClient;

    public String ask(List<OpenAiRequestDto.Message> messages) {
        return openAiClient.call(
                new OpenAiRequestDto("gpt-4.1-mini", messages)
        );
    }
}
