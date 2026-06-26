package com.lawask.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lawask.model.ChatResponse;
import com.lawask.service.LawChatService;
import com.lawask.service.LawIngestionService;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatClient chatClient;
    private final LawIngestionService lawIngestionService;
    private final LawChatService lawChatService;

    public ChatController(ChatClient chatClient, LawIngestionService lawIngestionService,
            LawChatService lawChatService) {
        this.chatClient = chatClient;
        this.lawIngestionService = lawIngestionService;
        this.lawChatService = lawChatService;

    }

    // 測試有接上 AI
    @GetMapping("/test")
    public String test() {
        return chatClient.prompt()
                .user("用一句話介紹勞動基準法")
                .call()
                .content();
    }

    // 注入資料
    @PostMapping("/ingest")
    public String ingest() throws Exception {
        return lawIngestionService.ingestAll();
    }

    // 問答功能
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody java.util.Map<String, String> body) {
        String question = body.get("question");
        String conversationId = body.getOrDefault("conversationId", "default");
        return lawChatService.ask(question, conversationId);
    }

}
