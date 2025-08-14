package com.aiassist.mediassist.controller;

import com.aiassist.mediassist.ai.OpenAiAgent;
import com.aiassist.mediassist.context.UserContext;
import com.aiassist.mediassist.dto.entity.Conversation;
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
 *
 * 说明：
 * - 提供普通与流式对话入口
 * - 支持多种流式实现：WebFlux (Flux) 和 Spring MVC (SseEmitter)
 * - 进入对话前会将用户 IP 与 memoryId 放入 ThreadLocal，供工具类（如 TextTools）使用
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private OpenAiAgent openAiAgent;
    
    @Autowired
    private ConversationService conversationService;

    @PostMapping("/message")
    public String chat(@RequestBody AgentReq req, HttpServletRequest request) {
        try {
            String userIp = IpUtils.getClientIp();
            UserContext.setCurrentUserIp(userIp);
            
            // 处理和验证 memoryId
            String memoryId = processMemoryId(req.getMemoryId(), userIp);
            UserContext.setCurrentMemoryId(memoryId);
            
            log.info("开始对话 - IP: {}, memoryId: {}", userIp, memoryId);
            
            // 调用 AI（工具现在可以通过 UserContext 获取 IP）
            return openAiAgent.chat(memoryId, req.getMessage());
        } finally {
            UserContext.clear();
        }
    }
   
    @PostMapping("/message/ori")
    public String chatori(@RequestBody AgentReq req, HttpServletRequest request) {
        try {
            String userIp = IpUtils.getClientIp();
            UserContext.setCurrentUserIp(userIp);
            UserContext.setCurrentMemoryId(req.getMemoryId());
            
            // 调用 AI（工具现在可以通过 UserContext 获取 IP）
            return openAiAgent.chat(req.getMemoryId(), req.getMessage());
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 方案一：WebFlux 纯文本流式响应（推荐）
     * 
     * 优点：
     * - 真正的响应式流，性能最佳
     * - 原生支持背压控制
     * - 返回纯文本流，前端直接读取
     */
    @PostMapping(value = "/stream-flux", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> chatStreamFlux(@RequestBody AgentReq req, HttpServletRequest request) {
        String userIp = IpUtils.getClientIp();
        UserContext.setCurrentUserIp(userIp);
        UserContext.setCurrentMemoryId(req.getMemoryId());
        
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
    }

    /**
     * 方案一-SSE：WebFlux SSE 格式流式响应
     * 
     * 如果前端需要标准 SSE 格式，使用此接口
     */
    @PostMapping(value = "/stream-flux-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStreamFluxSse(@RequestBody AgentReq req, HttpServletRequest request) {
        String userIp = IpUtils.getClientIp();
        UserContext.setCurrentUserIp(userIp);
        UserContext.setCurrentMemoryId(req.getMemoryId());
        
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
    }

    /**
     * 方案二：Spring MVC SseEmitter（兼容性好）
     * 
     * 优点：
     * - 基于传统 Servlet，无需额外依赖
     * - 兼容性好，适合现有 Spring MVC 项目
     * 
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
    @PostMapping("/stream-debug")
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


    @PostMapping("/stream")
    public String chatStream(@RequestBody AgentReq req,
                           HttpServletRequest request) {
        try {
            // 获取用户IP
            String userIp = IpUtils.getClientIp();

            // 设置用户上下文，让工具能够访问
            UserContext.setCurrentUserIp(userIp);
            UserContext.setCurrentMemoryId(req.getMemoryId());

            // 调用 AI
            return openAiAgent.chat(req.getMemoryId(), req.getMessage());

        } finally {
            // 清理 ThreadLocal
            UserContext.clear();
        }
    }

    /**
     * 快速测试接口：验证流式是否正常工作
     */
    @GetMapping(value = "/test-stream", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> testStream() {
        return Flux.just("Hello", " ", "World", "!", " 这是一个测试流。")
                .delayElements(java.time.Duration.ofMillis(500));  // 每500ms发送一个片段
    }
    
    // ==================== 会话管理接口 ====================
    
    /**
     * 创建新会话
     */
    @PostMapping("/conversations/new")
    public String createNewConversation(HttpServletRequest request) {
        try {
            String userIp = IpUtils.getClientIp();
            String memoryId = conversationService.createNewConversation(userIp);
            
            log.info("创建新会话 - IP: {}, memoryId: {}", userIp, memoryId);
            return memoryId;
            
        } catch (Exception e) {
            log.error("创建新会话失败", e);
            throw new RuntimeException("创建新会话失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户的会话列表
     */
    @GetMapping("/conversations")
    public java.util.List<Conversation> getUserConversations(HttpServletRequest request) {
        try {
            String userIp = IpUtils.getClientIp();
            return conversationService.getUserConversations(userIp);
            
        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return java.util.List.of();
        }
    }
    
    /**
     * 获取会话详情
     */
    @GetMapping("/conversations/{memoryId}")
    public Conversation getConversation(@PathVariable String memoryId) {
        try {
            return conversationService.getConversation(memoryId);
            
        } catch (Exception e) {
            log.error("获取会话详情失败: memoryId={}", memoryId, e);
            return null;
        }
    }
    
    /**
     * 删除会话
     */
    @DeleteMapping("/conversations/{memoryId}")
    public String deleteConversation(@PathVariable String memoryId) {
        try {
            conversationService.deleteConversation(memoryId);
            log.info("删除会话: memoryId={}", memoryId);
            return "删除成功";
            
        } catch (Exception e) {
            log.error("删除会话失败: memoryId={}", memoryId, e);
            throw new RuntimeException("删除会话失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取会话统计信息
     */
    @GetMapping("/conversations/stats")
    public ConversationService.ConversationStats getConversationStats() {
        try {
            return conversationService.getStats();
            
        } catch (Exception e) {
            log.error("获取会话统计失败", e);
            return ConversationService.ConversationStats.builder().build();
        }
    }

    // ==================== 私有辅助方法 ====================
    
    /**
     * 处理和验证 memoryId
     * 如果客户端提供的 memoryId 无效或为空，则自动创建新会话
     */
    private String processMemoryId(String clientMemoryId, String userIp) {
        // 如果客户端没有提供 memoryId 或者无效，创建新会话
        if (clientMemoryId == null || clientMemoryId.trim().isEmpty() || 
            !conversationService.isValidConversation(clientMemoryId)) {
            
            String newMemoryId = conversationService.createNewConversation(userIp);
            log.info("客户端memoryId无效 '{}', 自动创建新会话: {}", clientMemoryId, newMemoryId);
            return newMemoryId;
        }
        
        return clientMemoryId.trim();
    }

}
