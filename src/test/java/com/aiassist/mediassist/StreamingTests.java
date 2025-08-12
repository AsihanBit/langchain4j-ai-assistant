package com.aiassist.mediassist;

import com.aiassist.mediassist.ai.OpenAiAgent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class StreamingTests {

    @Autowired
    private OpenAiAgent openAiAgent;

    @Test
    void testStreamingFlux() {
        System.out.println("=== 流式 Flux 方式 ===");
        
        openAiAgent.chatStream("user1", "请简短介绍一下你自己")
                  .doOnNext(System.out::print)
                  .doOnComplete(() -> System.out.println("\n流式完成"))
                  .doOnError(error -> System.err.println("错误: " + error.getMessage()))
                  .timeout(Duration.ofSeconds(30))
                  .blockLast();
    }

    @Test
    void testChatStreamDirect() {
        System.out.println("=== 直接调用 chatStream ===");
        
        Flux<String> flux = openAiAgent.chatStream("user2", "用一句话说明什么是人工智能");
        
        flux.doOnNext(System.out::print)
            .doOnComplete(() -> System.out.println("\nFlux 完成"))
            .doOnError(error -> System.err.println("错误: " + error.getMessage()))
            .timeout(Duration.ofSeconds(30))
            .blockLast();
    }

    @Test
    void testChatFlux() {
        System.out.println("=== ChatFlux 便捷方式 ===");
        
        openAiAgent.chatFlux("user3", "hello")
                  .doOnNext(System.out::print)
                  .doOnComplete(() -> System.out.println("\n便捷方式完成"))
                  .timeout(Duration.ofSeconds(30))
                  .blockLast();
    }

    @Test
    void testNonStreaming() {
        System.out.println("=== 非流式方式 ===");
        
        String response = openAiAgent.chat("user4", "你好");
        System.out.println("完整响应: " + response);
    }
}
