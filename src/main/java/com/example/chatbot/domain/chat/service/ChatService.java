package com.example.chatbot.domain.chat.service;

import com.example.chatbot.infrastructure.openai.dto.OpenAiRequestDto;
import com.example.chatbot.infrastructure.openai.service.OpenAiService;
import com.example.chatbot.infrastructure.redis.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.chatbot.infrastructure.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final OpenAiService openAiService;
    private final ChatHistoryService chatHistoryService;

    public String chat(String userId, String message) {

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

        // 3. user 추가
        messages.add(new OpenAiRequestDto.Message(
                "user",
                message
        ));

        // 4. 길이 제한
        if (messages.size() > 20) {
            messages = messages.subList(messages.size() - 20, messages.size());
        }

        // 5. GPT 호출
        String response = openAiService.ask(messages);
        //이전 대화 + 현재 메세지를 같이 보내기 때문에 멀티턴

        // 6. assistant 저장
        messages.add(new OpenAiRequestDto.Message(
                "assistant",
                response
        ));

        // 7. Redis 저장
        chatHistoryService.saveMessages(userId, messages);

        return response;
    }
}
