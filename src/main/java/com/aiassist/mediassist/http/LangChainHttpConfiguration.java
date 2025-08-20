package com.aiassist.mediassist.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.*;

/**
 * 自定义的 RestClient (Apache HttpClient5) , 覆盖 LangChain4j 默认调用的
 * 思路： 定制 RestClient（基于 Apache HttpClient 5 的连接池和超时、驱逐策略），再把它包一层 SpringRestClient 作为 LangChain4j 的 HttpClient 传给 OpenAiChatModel。 确保模型用这套 HTTP 栈。
 * LangChain4j 默认走的 Spring RestClient 适配器： dev.langchain4j.http.client.spring.restclient.SpringRestClient
 * 这表示 LangChain4j 的 HTTP 层用的是 Spring Framework 6 的 RestClient，而不是 WebClient（Reactor Netty）或 OkHttp
 */
@Slf4j
@Configuration
public class LangChainHttpConfiguration {
    @Bean
    @Primary
    public RestClient langchainRestClient() {
        // 1. 配置连接池管理器
        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setConnectionTimeToLive(TimeValue.ofMinutes(2)) // 连接最大存活时间 2分钟
                        .setValidateAfterInactivity(TimeValue.ofSeconds(30)) // 空闲30秒后验证连接有效性
                        .setMaxConnTotal(200) // 最大连接数
                        .setMaxConnPerRoute(50)
                        .build();


        // 2. 自定义重试策略
        // 使用 DefaultHttpRequestRetryHandler
//        HttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(
//                3, // 最大重试次数
//                true // 是否重试请求发送异常
//        );
//        HttpRequestRetryStrategy retryStrategy = new DefaultHttpRequestRetryStrategy(); // 简化的重试策略
        HttpRequestRetryStrategy retryStrategy = new HttpRequestRetryStrategy() {
            @Override
            public boolean retryRequest(HttpRequest request, IOException exception, int execCount, HttpContext context) {
                log.info("[LangChainHttpConfiguration.retryRequest] ----- 重试策略被调用 当前重试次数:{} , 错误: {}", execCount, exception.getMessage());
                // 最多重试3次，只重试连接相关异常
                return execCount <= 3 && (
                        exception instanceof ConnectException ||
                                exception instanceof SocketTimeoutException ||
                                exception instanceof UnknownHostException ||
                                exception instanceof ConnectTimeoutException
                );
            }

            @Override
            public boolean retryRequest(HttpResponse response, int execCount, HttpContext context) {
                log.info("[LangChainHttpConfiguration.retryRequest] ----- 重试策略被调用 当前重试次数:{}", execCount);
                // 最多重试3次，只重试服务器错误
                return execCount <= 3 && response.getCode() >= 500;
            }

            @Override
            public TimeValue getRetryInterval(HttpRequest request, IOException exception, int execCount, HttpContext context) {
                log.info("[LangChainHttpConfiguration.getRetryInterval] ----- 重试策略被调用 当前重试次数:{} , 错误: {}", execCount, exception.getMessage());
//                return HttpRequestRetryStrategy.super.getRetryInterval(request, exception, execCount, context); 返回父类的
                return TimeValue.ofSeconds(execCount); // 1s, 2s, 3s
            }

            @Override
            public TimeValue getRetryInterval(HttpResponse response, int execCount, HttpContext context) {
                log.info("[LangChainHttpConfiguration.getRetryInterval] ----- 重试策略被调用 当前重试次数:{}", execCount);
                return TimeValue.ofSeconds(execCount); // 1s, 2s, 3s
            }

        };
        // 3. 请求配置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(10))
                .setResponseTimeout(Timeout.ofSeconds(30))
                .build();

        // 4. 创建HttpClient
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setRetryStrategy(retryStrategy)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections() // 定期清理过期连接
                .evictIdleConnections(TimeValue.ofSeconds(60)) // 清理空闲超过60秒的连接
//                .evictIdleConnections(TimeValue.ofSeconds(0)) // 设置为立即驱逐空闲连接
                .build();
//                // 使用默认的重试处理器，重试3次
//                .setRetryHandler(new DefaultHttpRequestRetryHandler())
//                .setRetryHandler(retryHandler)

        // 5. 配置RestClient
        return RestClient.builder()
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();

    }
}