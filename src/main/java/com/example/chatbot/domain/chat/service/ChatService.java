package com.example.chatbot.domain.chat.service;

import com.example.chatbot.domain.document.service.RagService;
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
    private final RagService ragService; // ✅ 추가

    public Flux<String> chatStream(String userId, String message, Long conversationId) {

        return ragService.retrieveContext(message, Long.parseLong(userId)) // ✅ RAG 검색
                .flatMapMany(context -> {

                    return chatHistoryService.getMessages(userId)
                            //Mono<List<OpenAiRequestDto.Message>> 반환
                            .flatMapMany(messages -> {

                                if (messages.isEmpty()) {
                                    // ✅ RAG 컨텍스트 시스템 프롬프트에 주입
                                    String systemPrompt = context.isEmpty()
                                            ? "너는 친절한 AI 상담원이다."
                                            : "너는 친절한 AI 상담원이다. 아래 문서를 참고해서 답변해라.\n\n" + context;

                                    messages.add(new OpenAiRequestDto.Message("system", systemPrompt));
                                }

                                messages.add(new OpenAiRequestDto.Message("user", message));

                                List<OpenAiRequestDto.Message> finalMessages =
                                        messages.size() > 20
                                                ? new ArrayList<>(messages.subList(messages.size() - 20, messages.size()))
                                                : messages;

                                StringBuilder fullResponse = new StringBuilder();

                                return openAiService.ask(finalMessages)
                                        .doOnNext(chunk -> fullResponse.append(chunk))
                                        .doOnComplete(() -> {
                                            finalMessages.add(new OpenAiRequestDto.Message(
                                                    "assistant", fullResponse.toString()));

                                            chatHistoryService.saveMessages(userId, finalMessages)
                                                    .doOnError(e -> log.error("Redis 저장 실패", e))
                                                    .subscribe();

                                            messageService.saveMessage(conversationId, "user", message)
                                                    .then(messageService.saveMessage(conversationId, "assistant", fullResponse.toString()))
                                                    .subscribe();
                                        });
                            });
                });
    }
}
