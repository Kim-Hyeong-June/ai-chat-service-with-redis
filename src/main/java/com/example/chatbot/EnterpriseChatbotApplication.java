package com.example.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

@SpringBootApplication(scanBasePackages = "com.example.chatbot")
@EnableR2dbcAuditing  // ✅ 추가
public class EnterpriseChatbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnterpriseChatbotApplication.class, args);
    }

}
