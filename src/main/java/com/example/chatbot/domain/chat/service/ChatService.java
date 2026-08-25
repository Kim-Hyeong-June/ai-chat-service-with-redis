package com.example.chatbot.domain.chat.service;

import com.example.chatbot.domain.document.service.RagService;
import com.example.chatbot.infrastructure.openai.dto.OpenAiRequestDto;
import com.example.chatbot.infrastructure.openai.service.OpenAiService;
import com.example.chatbot.infrastructure.redis.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final OpenAiService openAiService;
    private final ChatHistoryService chatHistoryService;
    private final MessageService messageService;
    private final RagService ragService;

    public Flux<String> chatStream(String userId, String message, Long conversationId) {

        return ragService.retrieveContext(message, Long.parseLong(userId))
                .flatMapMany(context ->

                        chatHistoryService.getMessages(userId)
                                .flatMapMany(historyMessages -> {

                                    // 1. 매 요청마다 System Prompt 생성
                                    String systemPrompt = context.isEmpty()
                                            ? "너는 친절한 AI 상담원이다."
                                            : """
                                              너는 친절한 AI 상담원이다.
                                              아래 문서를 참고해서 답변해라.

                                              %s
                                              """.formatted(context);

                                    // 2. OpenAI 요청용 메시지 생성
                                    List<OpenAiRequestDto.Message> requestMessages =
                                            new ArrayList<>();

                                    requestMessages.add(
                                            new OpenAiRequestDto.Message(
                                                    "system",
                                                    systemPrompt
                                            )
                                    );

                                    requestMessages.addAll(historyMessages);

                                    requestMessages.add(
                                            new OpenAiRequestDto.Message(
                                                    "user",
                                                    message
                                            )
                                    );

                                    // 최근 20개만 유지
                                    if (requestMessages.size() > 20) {
                                        requestMessages = new ArrayList<>(
                                                requestMessages.subList(
                                                        requestMessages.size() - 20,
                                                        requestMessages.size()
                                                )
                                        );
                                    }

                                    StringBuilder fullResponse =
                                            new StringBuilder();

                                    // Redis 저장용 (system 제외)

                                    return openAiService.ask(requestMessages)
                                            .doOnNext(fullResponse::append)
                                            .doOnComplete(() -> {
                                                List<OpenAiRequestDto.Message> historyToSave =
                                                        new ArrayList<>(historyMessages);

                                                historyToSave.add(
                                                        new OpenAiRequestDto.Message(
                                                                "user",
                                                                message
                                                        )
                                                );

                                                historyToSave.add(
                                                        new OpenAiRequestDto.Message(
                                                                "assistant",
                                                                fullResponse.toString()
                                                        )
                                                );

                                                if (historyToSave.size() > 20) {
                                                    historyToSave.subList(
                                                            0,
                                                            historyToSave.size() - 20
                                                    ).clear();
                                                }

                                                Mono.when(
                                                                chatHistoryService.saveMessages(userId, historyToSave),
                                                                messageService.saveMessage(conversationId, "user", message),
                                                                messageService.saveMessage(
                                                                        conversationId,
                                                                        "assistant",
                                                                        fullResponse.toString()
                                                                )
                                                        )
                                                        .doOnError(e -> log.error("저장 실패", e))
                                                        .subscribe();
                                                });
                                })
                );
    }
}
