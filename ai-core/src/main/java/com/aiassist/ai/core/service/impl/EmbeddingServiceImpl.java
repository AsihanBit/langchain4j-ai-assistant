package com.aiassist.ai.core.service.impl;

import com.aiassist.ai.core.config.RagConfiguration;
import com.aiassist.ai.core.service.EmbeddingService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    @Autowired
    private RagConfiguration ragConfiguration;

    @Autowired
    @Qualifier("embeddingRestTemplate")
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Float> getEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("文本内容不能为空");
        }

        try {
            // 构建请求
            EmbeddingRequest request = new EmbeddingRequest();
            request.setInput(text.trim());
            request.setModel(ragConfiguration.getEmbeddingModelName());
            request.setEncodingFormat("float");

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(ragConfiguration.getEmbeddingApiKey());

            HttpEntity<EmbeddingRequest> entity = new HttpEntity<>(request, headers);

            // 发送请求
            ResponseEntity<EmbeddingResponse> response = restTemplate.exchange(
                    ragConfiguration.getEmbeddingBaseUrl(),
                    HttpMethod.POST,
                    entity,
                    EmbeddingResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                EmbeddingResponse embeddingResponse = response.getBody();
                if (embeddingResponse.getData() != null && !embeddingResponse.getData().isEmpty()) {
                    List<Float> embedding = embeddingResponse.getData().get(0).getEmbedding();
                    log.debug("获取嵌入向量成功，维度: {}", embedding.size());
                    return embedding;
                } else {
                    throw new RuntimeException("嵌入响应数据为空");
                }
            } else {
                throw new RuntimeException("嵌入服务请求失败: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("获取嵌入向量失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取嵌入向量失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<List<Float>> getBatchEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }

        // 过滤空文本
        List<String> validTexts = texts.stream()
                .filter(text -> text != null && !text.trim().isEmpty())
                .map(String::trim)
                .toList();

        if (validTexts.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // 构建批量请求
            EmbeddingRequest request = new EmbeddingRequest();
            request.setInputs(validTexts);  // 批量输入
            request.setModel(ragConfiguration.getEmbeddingModelName());
            request.setEncodingFormat("float");

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(ragConfiguration.getEmbeddingApiKey());

            HttpEntity<EmbeddingRequest> entity = new HttpEntity<>(request, headers);

            // 发送请求
            ResponseEntity<EmbeddingResponse> response = restTemplate.exchange(
                    ragConfiguration.getEmbeddingBaseUrl(),
                    HttpMethod.POST,
                    entity,
                    EmbeddingResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                EmbeddingResponse embeddingResponse = response.getBody();
                List<List<Float>> embeddings = new ArrayList<>();

                if (embeddingResponse.getData() != null) {
                    for (EmbeddingData data : embeddingResponse.getData()) {
                        embeddings.add(data.getEmbedding());
                    }
                }

                log.debug("批量获取嵌入向量成功，数量: {}", embeddings.size());
                return embeddings;
            } else {
                throw new RuntimeException("批量嵌入服务请求失败: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("批量获取嵌入向量失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量获取嵌入向量失败: " + e.getMessage(), e);
        }
    }

    @Override
    public float calculateCosineSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1 == null || vector2 == null || vector1.size() != vector2.size()) {
            return 0.0f;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            float v1 = vector1.get(i);
            float v2 = vector2.get(i);

            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0f;
        }

        return (float) (dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2)));
    }

    // 内部类：嵌入请求
    @Data
    private static class EmbeddingRequest {
        private String input;  // 单个文本
        private List<String> inputs;  // 批量文本
        private String model;
        @JsonProperty("encoding_format")
        private String encodingFormat;

        // 根据是否有inputs决定使用哪个字段
        public Map<String, Object> toMap() {
            if (inputs != null && !inputs.isEmpty()) {
                return Map.of(
                        "input", inputs,
                        "model", model,
                        "encoding_format", encodingFormat
                );
            } else {
                return Map.of(
                        "input", input,
                        "model", model,
                        "encoding_format", encodingFormat
                );
            }
        }
    }

    // 内部类：嵌入响应
    @Data
    private static class EmbeddingResponse {
        private String object;
        private List<EmbeddingData> data;
        private String model;
        private EmbeddingUsage usage;
    }

    @Data
    private static class EmbeddingData {
        private String object;
        private List<Float> embedding;
        private int index;
    }

    @Data
    private static class EmbeddingUsage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        @JsonProperty("total_tokens")
        private int totalTokens;
    }
}
