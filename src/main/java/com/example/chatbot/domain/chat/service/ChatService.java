package com.example.chatbot.domain.chat.service;

import com.example.chatbot.infrastructure.openai.dto.OpenAiRequestDto;
import com.example.chatbot.infrastructure.openai.service.OpenAiService;
import com.example.chatbot.infrastructure.redis.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j  // ✅ 추가
public class ChatService {

    private final OpenAiService openAiService;
    private final ChatHistoryService chatHistoryService;

    public Flux<String> chatStream(String userId, String message) {

        return chatHistoryService.getMessages(userId)
                .flatMapMany(messages -> {

                    // 1. system 메시지 (처음만)
                    if (messages.isEmpty()) {
                        messages.add(new OpenAiRequestDto.Message(
                                "system",
                                "너는 친절한 AI 상담원이다"
                        ));
                    }

                    // 2. user 메시지 추가
                    messages.add(new OpenAiRequestDto.Message("user", message));

                    // 3. 길이 제한
                    List<OpenAiRequestDto.Message> finalMessages =
                            messages.size() > 20
                                    ? new ArrayList<>(messages.subList(
                                    messages.size() - 20,
                                    messages.size()))
                                    : messages;

                    // 4. OpenAI 스트리밍 호출
                    StringBuilder fullResponse = new StringBuilder();

                    return openAiService.ask(finalMessages)
                            .doOnNext(chunk -> fullResponse.append(chunk))
                            .doOnComplete(() -> {
                                // 5. assistant 메시지 추가
                                finalMessages.add(new OpenAiRequestDto.Message(
                                        "assistant",
                                        fullResponse.toString()
                                ));

                                // 6. Redis 저장 (비동기)
                                chatHistoryService.saveMessages(userId, finalMessages)
                                        .doOnSuccess(v -> log.info("Redis 저장 완료")) // ✅ 추가
                                        .doOnError(e -> log.error("Redis 저장 실패: {}", e.getMessage())) // ✅ 추가
                                        .subscribe();
                            });
                });
    }
}
