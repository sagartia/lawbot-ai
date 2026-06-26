package com.lawask.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class LawIngestionService {

    private final VectorStore vectorStore;
    // 原本限制256KB，提高到10MB
    private final WebClient webClient = WebClient.builder()
            .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
            .build();

    private static final Set<String> TARGET_LAWS = Set.of(
            "勞動基準法");

    public LawIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public String ingestAll() throws Exception {
        // 1. 從全國法規 API 下載 ZIP
        byte[] zipBytes = webClient.get()
                .uri("https://law.moj.gov.tw/api/ch/law/json")
                .retrieve()
                .bodyToMono(byte[].class)
                .block();

        // 2. 解壓取出 JSON 字串
        String json = extractJsonFromZip(zipBytes);

        // 3. 解析 JSON
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        // 4. 整理成 Document 清單
        List<Document> documents = new ArrayList<>();
        for (JsonNode law : root.get("Laws")) {
            String lawName = law.get("LawName").asText();
            if (!TARGET_LAWS.contains(lawName))
                continue;

            for (JsonNode article : law.get("LawArticles")) {
                if (!"A".equals(article.get("ArticleType").asText()))
                    continue;

                String articleNo = article.get("ArticleNo").asText();
                String content = article.get("ArticleContent").asText();

                Document doc = new Document(
                        lawName + " " + articleNo + "\n" + content,
                        Map.of(
                                "lawName", lawName,
                                "articleNo", articleNo));
                documents.add(doc);
            }
        }

        // 5. 存入向量資料庫
        int batchSize = 10;
        for (int i = 0; i < documents.size(); i += batchSize) {
            List<Document> batch = documents.subList(i, Math.min(i + batchSize, documents.size()));
            vectorStore.add(batch);
            System.out.println("已處理" + Math.min(i + batchSize, documents.size()) + "/" + documents.size());
            Thread.sleep(5000); // 等五秒再送下一批;
        }
        return "匯入完成，共" + documents.size() + "條條文";
    }

    private String extractJsonFromZip(byte[] zipBytes) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".json")) {
                    byte[] bytes = zis.readAllBytes();
                    // 處理 UTF-8 BOM（全國法規 API 有這個問題）
                    if (bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                        return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
                    }
                    return new String(bytes, StandardCharsets.UTF_8);
                }
            }
        }
        throw new RuntimeException("ZIP 裡找不到 JSON 檔案");
    }
}
