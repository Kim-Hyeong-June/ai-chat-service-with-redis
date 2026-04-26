package com.example.chatbot.domain.chat.service;

import com.example.chatbot.infrastructure.openai.dto.OpenAiRequestDto;
import com.example.chatbot.infrastructure.openai.service.OpenAiService;
import com.example.chatbot.infrastructure.redis.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final OpenAiService openAiService;
    private final ChatHistoryService chatHistoryService;

    public Flux<String> chatStream(String userId, String message) {

        // 1. 기존 대화 가져오기
        List<OpenAiRequestDto.Message> messages =
                chatHistoryService.getMessages(userId);

        // 2. system 메시지 (처음만)
        if (messages.isEmpty()) {
            messages.add(new OpenAiRequestDto.Message(
                    "system",
                    "너는 친절한 AI 상담원이다"
            ));
        }

        // 3. user 메시지 추가
        messages.add(new OpenAiRequestDto.Message(
                "user",
                message
        ));

        // 4. 길이 제한 (람다 문제 해결)
        List<OpenAiRequestDto.Message> finalMessages =
                messages.size() > 20
                        ? new ArrayList<>(
                        messages.subList(
                                messages.size() - 20,
                                messages.size()
                        )
                )
                        : messages;

        // 5. OpenAI Streaming 호출
        Flux<String> responseFlux = openAiService.ask(finalMessages);

        // 🔥 6. streaming 유지 + Redis 저장 (핵심 수정)
        StringBuilder fullResponse = new StringBuilder();

        return responseFlux
                .doOnNext(chunk -> fullResponse.append(chunk)) // 실시간 누적
                .doOnComplete(() -> {

                    // 전체 응답 저장
                    finalMessages.add(new OpenAiRequestDto.Message(
                            "assistant",
                            fullResponse.toString()
                    ));

                    // Redis 저장
                    chatHistoryService.saveMessages(userId, finalMessages);
                });
    }
}
