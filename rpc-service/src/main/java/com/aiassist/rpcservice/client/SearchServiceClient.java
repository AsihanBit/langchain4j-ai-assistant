package com.aiassist.rpcservice.client;

import com.rpc.service.web.searcher.searxng.AsyncSearchServiceGrpc;
import com.rpc.service.web.searcher.searxng.SearchRequest;
import com.rpc.service.web.searcher.searxng.SearchResponse;
import com.rpc.service.web.searcher.searxng.SearchResultItem;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * gRPC客户端，作为Spring Bean进行管理。
 * 通过依赖注入使用，并从配置文件中获取连接信息。
 */
@Component
public class SearchServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceClient.class);

    @Value("${grpc.client.host:localhost}")
    private String host;

    @Value("${grpc.client.port:50052}")
    private int port;

    private ManagedChannel channel;
    private AsyncSearchServiceGrpc.AsyncSearchServiceBlockingStub blockingStub;

    /**
     * 使用@PostConstruct注解，在Bean初始化后执行此方法。
     * 负责建立gRPC连接。
     */
    @PostConstruct
    private void init() {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext() // 在生产环境中建议使用TLS加密
                .build();
        blockingStub = AsyncSearchServiceGrpc.newBlockingStub(channel);
        logger.info("gRPC client bean initialized, connected to {}:{}", host, port);
    }

    /**
     * 执行完整的搜索请求
     *
     * @param query      搜索关键词，例如："苹果17"
     * @param pageNumber 页码
     * @param timeRange  时间范围，例如："", "day", "week"
     * @param category   类别，例如："general", "images"
     * @param language   语言，例如："auto", "zh-CN"
     * @param safeSearch 安全搜索开关
     * @return 搜索结果列表
     */
    public List<SearchResultItem> search(String query, int pageNumber, String timeRange,
                                         String category, String language, boolean safeSearch) {
        logger.info("Executing search request: query={}, page={}, timeRange={}, category={}, language={}, safeSearch={}",
                query, pageNumber, timeRange, category, language, safeSearch);

        try {
            SearchRequest request = SearchRequest.newBuilder()
                    .setQuery(query)
                    .setPageNumber(pageNumber)
                    .setTimeRange(timeRange)
                    .setCategory(category)
                    .setLanguage(language)
                    .setSafeSearch(safeSearch)
                    .build();

            SearchResponse response = blockingStub.search(request);
            List<SearchResultItem> results = response.getResultsList();

            logger.info("Search completed, returned {} results", results.size());
            return results;

        } catch (StatusRuntimeException e) {
            logger.error("Search request failed: {}", e.getStatus());
            // 转换为更通用的异常，以便上层处理
            throw new RuntimeException("gRPC service call failed", e);
        }
    }

    /**
     * 执行搜索请求的简化版本
     */
    public List<SearchResultItem> search(String query) {
        return search(query, 1, "", "general", "auto", false);
    }

    /**
     * 使用@PreDestroy注解，在应用关闭前执行此方法。
     * 负责优雅地关闭gRPC连接。
     */
    @PreDestroy
    public void shutdown() {
        try {
            if (channel != null) {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                logger.info("gRPC client connection shut down.");
            }
        } catch (InterruptedException e) {
            logger.warn("gRPC client shutdown interrupted.", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 打印搜索结果的辅助方法
     *
     * @param results 搜索结果列表
     */
    public static void printResults(List<SearchResultItem> results) {
        if (results == null || results.isEmpty()) {
            System.out.println("未找到任何结果。");
            return;
        }
        System.out.println("=== 搜索结果 ===");
        for (int i = 0; i < results.size(); i++) {
            SearchResultItem item = results.get(i);
            System.out.printf("%d. 标题: %s%n", i + 1, item.getTitle());
            System.out.printf("   链接: %s%n", item.getUrl());
            System.out.printf("   内容: %s%n", item.getContent());
            System.out.println("   ---");
        }
    }
}
