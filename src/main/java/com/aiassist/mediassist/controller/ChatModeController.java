package com.aiassist.mediassist.controller;

import com.aiassist.mediassist.ai.OpenAiAgent;
import com.aiassist.mediassist.context.UserContext;
import com.aiassist.mediassist.dto.req.AgentReq;
import com.aiassist.mediassist.service.ConversationService;
import com.aiassist.mediassist.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

/**
 * 对话接口控制器
 * <p>
 * 说明：
 * - 提供普通与流式对话的各种方案
 * - 支持多种流式实现：WebFlux (Flux) 和 Spring MVC (SseEmitter)
 * - 进入对话前会将用户 IP 与 memoryId 放入 ThreadLocal，供工具类（如 TextTools）使用
 */
@Slf4j
@RestController
@RequestMapping("/chat-mode")
public class ChatModeController {

    @Autowired
    private OpenAiAgent openAiAgent;

    @Autowired
    private ConversationService conversationService;

    // ==================== 非流式对话 ====================

    @PostMapping("/message")
    public String chat(@RequestBody AgentReq req, HttpServletRequest request) {
        try {
            String userIp = IpUtils.getClientIp();
            UserContext.setCurrentUserIp(userIp);

            // 处理和验证 memoryId
            String memoryId = conversationService.processMemoryId(req.getMemoryId(), userIp);
            UserContext.setCurrentMemoryId(memoryId);

            log.info("开始对话 - IP: {}, memoryId: {}", userIp, memoryId);

            // 调用 AI（工具现在可以通过 UserContext 获取 IP）
            return openAiAgent.chat(memoryId, req.getMessage());

        } finally {
            UserContext.clear();
        }
    }

    // ==================== 流式对话 ====================

    /**
     * 方案一：WebFlux 纯文本流式响应（推荐）
     * 优点：
     * - 真正的响应式流，性能最佳
     * - 原生支持背压控制
     * - 返回纯文本流，前端直接读取
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> chatStreamFlux(@RequestBody AgentReq req, HttpServletRequest request) {
        try {
            String userIp = IpUtils.getClientIp();
            UserContext.setCurrentUserIp(userIp);

            log.info("开始 WebFlux 流式对话 - IP: {}, memoryId: {}", userIp, req.getMemoryId());

            return openAiAgent.chatStream(req.getMemoryId(), req.getMessage())
                    .doOnNext(chunk -> log.debug("WebFlux 发送片段: {}",
                            chunk.length() > 50 ? chunk.substring(0, 50) + "..." : chunk))
                    .doOnComplete(() -> log.info("WebFlux 流式对话完成"))
                    .doOnError(error -> log.error("WebFlux 流式对话出错", error))
                    .doFinally(signalType -> {
                        UserContext.clear();
                        log.debug("清理用户上下文，信号类型: {}", signalType);
                    });

        } catch (Exception e) {
            throw new RuntimeException("客户端对话失败");
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 方案二-SSE：WebFlux SSE 格式流式响应
     * 如果前端需要标准 SSE 格式，使用此接口
     */
    @PostMapping(value = "/stream-flux-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStreamFluxSse(@RequestBody AgentReq req, HttpServletRequest request) {
        try {
            String userIp = IpUtils.getClientIp();
            UserContext.setCurrentUserIp(userIp);

            log.info("开始 WebFlux SSE 流式对话 - IP: {}, memoryId: {}", userIp, req.getMemoryId());

            return openAiAgent.chatStream(req.getMemoryId(), req.getMessage())
                    .map(chunk -> "data: " + chunk.replace("\n", "\ndata: ") + "\n\n")  // 正确的 SSE 格式
                    .doOnNext(chunk -> log.debug("WebFlux SSE 发送片段"))
                    .doOnComplete(() -> log.info("WebFlux SSE 流式对话完成"))
                    .doOnError(error -> log.error("WebFlux SSE 流式对话出错", error))
                    .doFinally(signalType -> {
                        UserContext.clear();
                        log.debug("清理用户上下文，信号类型: {}", signalType);
                    });

        } catch (Exception e) {
            log.error("带记忆对话失败", e);
            throw new RuntimeException("客户端对话失败");
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 方案三：Spring MVC SseEmitter（兼容性好）
     * 优点：
     * - 基于传统 Servlet，无需额外依赖
     * - 兼容性好，适合现有 Spring MVC 项目
     * 前端示例：
     * const eventSource = new EventSource('/api/chat/stream-sse');
     * eventSource.onmessage = (event) => console.log(event.data);
     */
    @PostMapping(value = "/stream-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStreamSse(@RequestBody AgentReq req, HttpServletRequest request) {
        String userIp = IpUtils.getClientIp();
        UserContext.setCurrentUserIp(userIp);
        UserContext.setCurrentMemoryId(req.getMemoryId());

        log.info("开始 SSE 流式对话 - IP: {}, memoryId: {}", userIp, req.getMemoryId());

        // 创建 SseEmitter，0L 表示永不超时（可根据需要调整）
        SseEmitter emitter = new SseEmitter(0L);

        try {
            openAiAgent.chatStream(req.getMemoryId(), req.getMessage())
                    .subscribe(
                            chunk -> {
                                try {
                                    log.debug("SSE 发送片段: {}",
                                            chunk.length() > 50 ? chunk.substring(0, 50) + "..." : chunk);
                                    emitter.send(SseEmitter.event().data(chunk));
                                } catch (Exception e) {
                                    log.error("SSE 发送数据失败", e);
                                    emitter.completeWithError(e);
                                }
                            },
                            error -> {
                                log.error("SSE 流式对话出错", error);
                                emitter.completeWithError(error);
                                UserContext.clear();
                            },
                            () -> {
                                log.info("SSE 流式对话完成");
                                emitter.complete();
                                UserContext.clear();
                            }
                    );
        } catch (Exception e) {
            log.error("SSE 启动失败", e);
            emitter.completeWithError(e);
            UserContext.clear();
        }
        return emitter;
    }

    /**
     * 方案三：传统阻塞式（仅用于测试，不推荐生产使用）
     */
//    @PostMapping("/stream-debug")
    public void chatStreamDebug(@RequestBody AgentReq req, HttpServletRequest request) {
        try {
            String userIp = IpUtils.getClientIp();
            UserContext.setCurrentUserIp(userIp);
            UserContext.setCurrentMemoryId(req.getMemoryId());

            log.info("开始调试模式流式对话 - IP: {}, memoryId: {}", userIp, req.getMemoryId());

            // 注意：这种方式会阻塞线程，仅用于服务端调试
            openAiAgent.chatStream(req.getMemoryId(), req.getMessage())
                    .doOnNext(chunk -> System.out.print(chunk))
                    .blockLast();
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 快速测试接口：验证流式是否正常工作
     */
//    @GetMapping(value = "/test-stream", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> testStream() {
        return Flux.just("Hello", " ", "World", "!", " 这是一个测试流。")
                .delayElements(java.time.Duration.ofMillis(500));  // 每500ms发送一个片段
    }
}
