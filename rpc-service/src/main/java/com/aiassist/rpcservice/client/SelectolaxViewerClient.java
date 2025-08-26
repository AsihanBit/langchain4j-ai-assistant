package com.aiassist.rpcservice.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.rpc.service.web.viewer.selectolax.ViewRequest;
import com.rpc.service.web.viewer.selectolax.ViewResponse;
import com.rpc.service.web.viewer.selectolax.WebViewerServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * gRPC客户端 - 基于Future的实现
 * 网页内容抓取
 * <p>
 * 特点：
 * - 使用FutureStub，避免阻塞调用线程
 * - 适合Web应用的多用户并发场景
 * - 提供同步和异步两种调用方式
 * - 同步方式：内部使用Future但等待结果，不阻塞gRPC线程池
 * - 异步方式：返回CompletableFuture，完全非阻塞
 */
@Slf4j
@Component
public class SelectolaxViewerClient {

    @Value("${grpc.view.client.host:localhost}")
    private String host;

    @Value("${grpc.view.client.port:50055}")
    private int port;

    private ManagedChannel channel;
    private WebViewerServiceGrpc.WebViewerServiceFutureStub futureStub;

    @PostConstruct
    public void init() {
        // 创建gRPC通道
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext() // 使用明文传输，生产环境建议使用TLS
                .build();

        // 创建Future式客户端存根（适合Web应用，避免阻塞线程）
        futureStub = WebViewerServiceGrpc.newFutureStub(channel);

        log.info("gRPC Future客户端初始化完成，连接到 {}:{}", host, port);
    }

    /**
     * 抓取网页内容 - 简单版本（同步等待Future结果）
     *
     * @param url 要抓取的网页URL
     * @return 网页内容响应
     */
    public ViewResponse viewWebPage(String url) {
        return viewWebPage(url, 10, null, false, false, true, null, null);
    }

    /**
     * 抓取网页内容 - 带超时设置（同步等待Future结果）
     *
     * @param url            要抓取的网页URL
     * @param timeoutSeconds 超时时间（秒）
     * @return 网页内容响应
     */
    public ViewResponse viewWebPage(String url, int timeoutSeconds) {
        return viewWebPage(url, timeoutSeconds, null, false, false, true, null, null);
    }

    /**
     * 抓取网页内容 - 带超时 截断
     *
     * @param url            要抓取的网页URL
     * @param timeoutSeconds 超时时间（秒）
     * @return 网页内容响应
     */
    public ViewResponse viewWebPage(String url, int timeoutSeconds, int textStartPos, int textEndPos) {
        return viewWebPage(url, timeoutSeconds, null, false, false, true, textStartPos, textEndPos);
    }

    /**
     * 抓取网页内容 - 完整版本（同步等待Future结果）
     *
     * @param url              要抓取的网页URL
     * @param timeoutSeconds   超时时间（秒）
     * @param customHeaders    自定义请求头
     * @param includeLinks     是否包含链接
     * @param includeImages    是否包含图片信息
     * @param preserveNewlines 是否保留换行符
     * @param textStartPos     文本截取起始位置（可选）
     * @param textEndPos       文本截取结束位置（可选）
     * @return 网页内容响应
     */
    public ViewResponse viewWebPage(String url, int timeoutSeconds, Map<String, String> customHeaders,
                                    boolean includeLinks, boolean includeImages, boolean preserveNewlines,
                                    Integer textStartPos, Integer textEndPos) {
        try {
            // 构建请求
            ViewRequest.Builder requestBuilder = ViewRequest.newBuilder()
                    .setUrl(url)
                    .setTimeoutSeconds(timeoutSeconds)
                    .setIncludeLinks(includeLinks)
                    .setIncludeImages(includeImages)
                    .setPreserveNewlines(preserveNewlines);

            // 添加自定义请求头
            if (customHeaders != null && !customHeaders.isEmpty()) {
                requestBuilder.putAllCustomHeaders(customHeaders);
            }

            // 设置文本截取位置
            if (textStartPos != null) {
                requestBuilder.setTextStartPos(textStartPos);
            }
            if (textEndPos != null) {
                requestBuilder.setTextEndPos(textEndPos);
            }

            ViewRequest request = requestBuilder.build();

            log.debug("发送网页抓取请求: url={}, timeout={}s", url, timeoutSeconds);

            // 使用Future客户端发送请求并等待结果 TODO 调整超时参数
            ListenableFuture<ViewResponse> future = futureStub
                    .withDeadlineAfter(timeoutSeconds + 5, TimeUnit.SECONDS)
                    .viewWebPage(request);

            // 等待Future结果
            ViewResponse response = future.get(timeoutSeconds + 10, TimeUnit.SECONDS);

            log.debug("网页抓取完成: url={}, status={}, title={}, contentLength={}",
                    response.getUrl(), response.getStatusCode(), response.getTitle(),
                    response.getTextContent().length());

            return response;

        } catch (TimeoutException e) {
            log.error("网页抓取超时: url={}, timeout={}s", url, timeoutSeconds);
            throw new RuntimeException("网页抓取超时: " + url, e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof StatusRuntimeException) {
                StatusRuntimeException grpcException = (StatusRuntimeException) e.getCause();
                log.error("gRPC调用失败: url={}, status={}, message={}", url, grpcException.getStatus(), grpcException.getMessage());
                throw new RuntimeException("网页抓取失败: " + grpcException.getMessage(), grpcException);
            }
            log.error("网页抓取执行异常: url={}", url, e.getCause());
            throw new RuntimeException("网页抓取异常: " + e.getCause().getMessage(), e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("网页抓取被中断: url={}", url);
            throw new RuntimeException("网页抓取被中断: " + url, e);
        } catch (Exception e) {
            log.error("网页抓取异常: url={}", url, e);
            throw new RuntimeException("网页抓取异常: " + e.getMessage(), e);
        }
    }

    /**
     * 异步抓取网页内容 - 推荐用于Web应用，避免阻塞线程
     *
     * @param url 要抓取的网页URL
     * @return CompletableFuture包装的响应
     */
    public CompletableFuture<ViewResponse> viewWebPageAsync(String url) {
        return viewWebPageAsync(url, 15, null, false, false, true, null, null);
    }

    /**
     * 异步抓取网页内容 - 带超时设置
     *
     * @param url            要抓取的网页URL
     * @param timeoutSeconds 超时时间（秒）
     * @return CompletableFuture包装的响应
     */
    public CompletableFuture<ViewResponse> viewWebPageAsync(String url, int timeoutSeconds) {
        return viewWebPageAsync(url, timeoutSeconds, null, false, false, true, null, null);
    }

    /**
     * 异步抓取网页内容 - 完整版本，推荐用于Web应用
     *
     * @param url              要抓取的网页URL
     * @param timeoutSeconds   超时时间（秒）
     * @param customHeaders    自定义请求头
     * @param includeLinks     是否包含链接
     * @param includeImages    是否包含图片信息
     * @param preserveNewlines 是否保留换行符
     * @param textStartPos     文本截取起始位置（可选）
     * @param textEndPos       文本截取结束位置（可选）
     * @return CompletableFuture包装的响应
     */
    public CompletableFuture<ViewResponse> viewWebPageAsync(String url, int timeoutSeconds,
                                                            Map<String, String> customHeaders,
                                                            boolean includeLinks, boolean includeImages,
                                                            boolean preserveNewlines,
                                                            Integer textStartPos, Integer textEndPos) {
        try {
            // 构建请求
            ViewRequest.Builder requestBuilder = ViewRequest.newBuilder()
                    .setUrl(url)
                    .setTimeoutSeconds(timeoutSeconds)
                    .setIncludeLinks(includeLinks)
                    .setIncludeImages(includeImages)
                    .setPreserveNewlines(preserveNewlines);

            // 添加自定义请求头
            if (customHeaders != null && !customHeaders.isEmpty()) {
                requestBuilder.putAllCustomHeaders(customHeaders);
            }

            // 设置文本截取位置
            if (textStartPos != null) {
                requestBuilder.setTextStartPos(textStartPos);
            }
            if (textEndPos != null) {
                requestBuilder.setTextEndPos(textEndPos);
            }

            ViewRequest request = requestBuilder.build();

            log.debug("发送异步网页抓取请求: url={}, timeout={}s", url, timeoutSeconds);

            // 使用Future客户端发送异步请求
            ListenableFuture<ViewResponse> future = futureStub
                    .withDeadlineAfter(timeoutSeconds + 5, TimeUnit.SECONDS)
                    .viewWebPage(request);

            // 转换为CompletableFuture
            CompletableFuture<ViewResponse> completableFuture = new CompletableFuture<>();

            future.addListener(() -> {
                try {
                    ViewResponse response = future.get();
                    log.debug("异步网页抓取完成: url={}, status={}, title={}, contentLength={}",
                            response.getUrl(), response.getStatusCode(), response.getTitle(),
                            response.getTextContent().length());
                    completableFuture.complete(response);
                } catch (Exception e) {
                    log.error("异步网页抓取失败: url={}", url, e);
                    completableFuture.completeExceptionally(
                            new RuntimeException("异步网页抓取失败: " + e.getMessage(), e));
                }
            }, Runnable::run);

            return completableFuture;

        } catch (Exception e) {
            log.error("创建异步网页抓取请求失败: url={}", url, e);
            CompletableFuture<ViewResponse> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("创建异步请求失败: " + e.getMessage(), e));
            return failedFuture;
        }
    }


    /**
     * 批量异步抓取网页内容
     *
     * @param urls URL列表
     * @return CompletableFuture包装的响应列表
     */
    public CompletableFuture<List<ViewResponse>> viewWebPagesBatch(List<String> urls) {
        return viewWebPagesBatch(urls, 15);
    }

    /**
     * 批量异步抓取网页内容 - 带超时设置
     *
     * @param urls           URL列表
     * @param timeoutSeconds 超时时间（秒）
     * @return CompletableFuture包装的响应列表
     */
    public CompletableFuture<List<ViewResponse>> viewWebPagesBatch(List<String> urls, int timeoutSeconds) {
        if (urls == null || urls.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        log.info("开始批量异步抓取 {} 个网页", urls.size());

        // 创建所有异步请求
        List<CompletableFuture<ViewResponse>> futures = new ArrayList<>();
        for (String url : urls) {
            CompletableFuture<ViewResponse> future = viewWebPageAsync(url, timeoutSeconds,
                    null, false, false, true, null, null);
            futures.add(future);
        }

        // 等待所有请求完成
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<ViewResponse> results = new ArrayList<>();
                    int successCount = 0;

                    for (int i = 0; i < futures.size(); i++) {
                        try {
                            ViewResponse response = futures.get(i).get();
                            results.add(response);
                            successCount++;
                            log.debug("批量抓取完成 {}/{}: {}", i + 1, urls.size(), urls.get(i));
                        } catch (Exception e) {
                            log.warn("批量抓取失败 {}/{}: {}, 错误: {}", i + 1, urls.size(), urls.get(i), e.getMessage());
                            // 添加null保持索引对应
                            results.add(null);
                        }
                    }

                    log.info("批量异步抓取完成: 总数={}, 成功={}, 失败={}",
                            urls.size(), successCount, urls.size() - successCount);
                    return results;
                });
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            try {
                // 优雅关闭通道
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                log.info("gRPC客户端已关闭");
            } catch (InterruptedException e) {
                log.warn("关闭gRPC客户端时被中断", e);
                Thread.currentThread().interrupt();
            }
        }
    }
}
