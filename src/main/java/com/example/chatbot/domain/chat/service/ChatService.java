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
    private final MessageService messageService; // ✅ 추가

    public Flux<String> chatStream(String userId, String message , Long conversationId) {

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
                            // flux 에서 데이터 들어올 때 마다 생성
                            .doOnComplete(() -> {
                                // 5. assistant 메시지 추가 위에서 다 끝나면 실행
                                finalMessages.add(new OpenAiRequestDto.Message(
                                        "assistant",
                                        fullResponse.toString()
                                ));

                                // ✅ Redis 저장
                                chatHistoryService.saveMessages(userId, finalMessages).subscribe();

                                // ✅ PostgreSQL 저장
                                messageService.saveMessage(conversationId, "user", message)
                                        .then(messageService.saveMessage(conversationId, "assistant", fullResponse.toString()))
                                        .subscribe();
                                // Reactor의 Mono, Flux 는 기본적으로 lazy(게으름) 상태라서 subscribe 에 실행
                                // Reactor란 Spring WebFlux의 핵심 비동기 라이브러리 ex> mono , flux

                            });
                });
    }
}
