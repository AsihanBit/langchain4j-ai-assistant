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
public class RagConfiguration {

    /**
     * Weaviate HTTP 主机名或 IP。
     * 示例：103.117.122.68
     */
    @Value("${weaviate.host}")
    private String weaviateHost;

    /**
     * Weaviate HTTP 端口，默认 8080。
     */
    @Value("${weaviate.port:8080}")
    private Integer weaviatePort;

    /**
     * 协议：http 或 https。若网关前有 TLS，请改为 https。
     */
    @Value("${weaviate.scheme:http}")
    private String weaviateScheme;

    /**
     * 客户端超时（毫秒）。SDK 内部使用，可按网络环境加大，例如 60000。
     */
    @Value("${weaviate.timeout:30000}")
    private Integer weaviateTimeout;

    /**
     * 嵌入服务的 Base URL（OpenAI 兼容）。例如：https://api.zetatechs.com/v1/embeddings
     */
    @Value("${embedding.base-url}")
    private String embeddingBaseUrl;

    /**
     * 嵌入服务 API Key（务必使用环境变量或 dev 配置，不要提交到仓库）。
     */
    @Value("${embedding.api-key}")
    private String embeddingApiKey;

    /**
     * 嵌入模型名，默认 text-embedding-3-small 或其他兼容模型。
     */
    @Value("${embedding.model-name}")
    private String embeddingModelName;

    /**
     * 调用嵌入接口的超时（毫秒）。
     */
    @Value("${embedding.timeout:30000}")
    private Integer embeddingTimeout;

    /**
     * 构建 Weaviate 客户端 Bean。
     * <p>
     * 说明：
     * - 目前使用 weaviate-java v1 的 GraphQL HTTP 客户端；若迁移到 v4/gRPC，请调整此处工厂。
     * - 仅依赖 host:port 与 scheme；认证（如 API-KEY）可在此处补充 Header 配置。
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
     * 用于调用嵌入服务的 RestTemplate。可按需添加拦截器、超时、重试等配置。
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
