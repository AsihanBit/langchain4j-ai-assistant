package com.aiassist.rpcservice.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.rpc.service.web.searcher.searxng.AsyncSearchServiceGrpc;
import com.rpc.service.web.searcher.searxng.SearchRequest;
import com.rpc.service.web.searcher.searxng.SearchResponse;
import com.rpc.service.web.searcher.searxng.SearchResultItem;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * gRPC客户端，作为Spring Bean进行管理。
 * 通过依赖注入使用，并从配置文件中获取连接信息。
 */
@Slf4j
@Component
public class SearchServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceClient.class);

    @Value("${grpc.search.client.host:localhost}")
    private String host;

    @Value("${grpc.search.client.port:50052}")
    private int port;

    private ManagedChannel channel;
    //    private AsyncSearchServiceGrpc.AsyncSearchServiceBlockingStub blockingStub; // 换成了 FutureStub
    private AsyncSearchServiceGrpc.AsyncSearchServiceFutureStub futureStub;

    /**
     * 使用@PostConstruct注解，在Bean初始化后执行此方法。
     * 负责建立gRPC连接。
     */
    @PostConstruct
    private void init() {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext() // 在生产环境中建议使用TLS加密
                .build();
//        blockingStub = AsyncSearchServiceGrpc.newBlockingStub(channel);
        futureStub = AsyncSearchServiceGrpc.newFutureStub(channel);
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

//            SearchResponse response = blockingStub.search(request);

            // 使用Future客户端发送请求并等待结果 TODO 调整超时参数
            ListenableFuture<SearchResponse> future = futureStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .search(request);
            // 等待Future结果
            SearchResponse response = future.get(10, TimeUnit.SECONDS);

            logger.info("Search completed, returned {} results", response.getResultsList());
            List<SearchResultItem> results = response.getResultsList();
            return results;

        } catch (StatusRuntimeException e) {
            log.error("gRPC 调用失败: code={}, desc={}, query={}, pageNumber={}",
                    e.getStatus().getCode(), e.getStatus().getDescription(), query, pageNumber, e);
            // 转换为更通用的异常，以便上层处理
            throw new RuntimeException("gRPC service call failed: " + e.getStatus(), e);
        } catch (TimeoutException e) {
            log.error("网页搜索超时: query={}, pageNumber={}", query, pageNumber, e);
            // 如果有 Future：future.cancel(true);
            throw new RuntimeException("网页搜索超时: " + query, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断标记
            log.error("网页搜索被中断: query={}, pageNumber={}", query, pageNumber, e);
            throw new RuntimeException("网页搜索被中断: " + query, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof io.grpc.StatusRuntimeException sre) {
                log.error("gRPC 调用失败(封装在 ExecutionException): code={}, desc={}, query={}, pageNumber={}",
                        sre.getStatus().getCode(), sre.getStatus().getDescription(), query, pageNumber, sre);
                throw new RuntimeException("gRPC service call failed: " + sre.getStatus(), sre);
            }
            log.error("网页搜索执行失败: query={}, pageNumber={}", query, pageNumber, e);
            throw new RuntimeException("网页搜索执行失败: " + query, e);
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
