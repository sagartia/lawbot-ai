package com.lawask.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.lawask.model.ChatResponse;

@Service
public class LawChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ChatMemory chatMemory;

    public LawChatService(ChatClient chatClient, VectorStore vectorStore, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.chatMemory = chatMemory;
    }

    public ChatResponse ask(String question, String conversationId) {
        // 1. 向量搜尋找最相關的前5條條文
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(5)
                        .build());

        // 2. 組成法條上下文
        String context = docs.stream()
                .map(doc -> {
                    Map<String, Object> meta = doc.getMetadata();
                    return "【" + meta.get("lawName") + " " + meta.get("articleNo") + "】\n"
                            + doc.getText();
                })
                .collect(Collectors.joining("\n\n"));

        // 3. 呼叫 AI（帶入對話記憶）
        String answer = chatClient.prompt()
                .system("""
                        你是一位專業的台灣勞動法律顧問。
                        請根據以下法條內容回答使用者的問題。
                        - 務必引用具體條文，例如「依勞基法第22條」
                        - 使用繁體中文，語氣親切易懂
                        - 如果提供的法條不足以回答，請誠實說明

                        相關法條：
                        %s
                        """.formatted(context))
                .user(question)
                .advisors(spec -> spec
                        .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                        .param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        // 4. 整理來源清單
        List<ChatResponse.Source> sources = docs.stream()
                .map(doc -> {
                    Map<String, Object> meta = doc.getMetadata();
                    return new ChatResponse.Source(
                            (String) meta.get("lawName"),
                            (String) meta.get("articleNo"));
                })
                .collect(Collectors.toList());

        return new ChatResponse(answer, sources);
    }
}
