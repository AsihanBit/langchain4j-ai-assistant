package com.aiassist.mediassist.config;

import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
public class RagConfig {

    @Value("${weaviate.host}")
    private String weaviateHost;

    @Value("${weaviate.port:8080}")
    private Integer weaviatePort;

    @Value("${weaviate.scheme:http}")
    private String weaviateScheme;

    @Value("${weaviate.timeout:30000}")
    private Integer weaviateTimeout;

    @Value("${embedding.base-url}")
    private String embeddingBaseUrl;

    @Value("${embedding.api-key}")
    private String embeddingApiKey;

    @Value("${embedding.model-name}")
    private String embeddingModelName;

    @Value("${embedding.timeout:30000}")
    private Integer embeddingTimeout;

    /**
     * Weaviate客户端Bean
     */
    @Bean
    public WeaviateClient weaviateClient() {
        try {
            Config config = new Config(weaviateScheme, weaviateHost + ":" + weaviatePort);
            WeaviateClient client = new WeaviateClient(config);
            
            log.info("Weaviate客户端初始化成功 - {}://{}:{}", weaviateScheme, weaviateHost, weaviatePort);
            return client;
        } catch (Exception e) {
            log.error("Weaviate客户端初始化失败", e);
            throw new RuntimeException("无法连接到Weaviate服务器", e);
        }
    }

    /**
     * RestTemplate for embedding API calls
     */
    @Bean("embeddingRestTemplate")
    public RestTemplate embeddingRestTemplate() {
        return new RestTemplate();
    }

    // Getter methods for configuration values
    public String getEmbeddingBaseUrl() {
        return embeddingBaseUrl;
    }

    public String getEmbeddingApiKey() {
        return embeddingApiKey;
    }

    public String getEmbeddingModelName() {
        return embeddingModelName;
    }

    public Integer getEmbeddingTimeout() {
        return embeddingTimeout;
    }

    public String getWeaviateHost() {
        return weaviateHost;
    }

    public Integer getWeaviatePort() {
        return weaviatePort;
    }

    public String getWeaviateScheme() {
        return weaviateScheme;
    }

    public Integer getWeaviateTimeout() {
        return weaviateTimeout;
    }
}
