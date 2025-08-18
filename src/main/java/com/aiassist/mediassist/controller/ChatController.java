package com.aiassist.mediassist.controller;

import com.aiassist.mediassist.ai.OpenAiAgent;
import com.aiassist.mediassist.context.UserContext;
import com.aiassist.mediassist.dto.req.chat.ChatReq;
import com.aiassist.mediassist.dto.res.chat.ChatStreamRes;
import com.aiassist.mediassist.dto.res.chat.ChatStringRes;
import com.aiassist.mediassist.result.Result;
import com.aiassist.mediassist.service.ConversationService;
import com.aiassist.mediassist.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * 聊天记忆控制器
 * <p>
 * 提供基于MongoDB持久化的聊天记忆功能
 * 独立于原有的ChatController，不影响原有接口
 * <p>
 * 设计架构：
 * - 完全依赖OpenAiAgent的chatMemoryProvider配置
 * - 所有记忆逻辑由LangChain4j统一管理
 * - 提供额外的会话管理和查询功能
 */
@Slf4j
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private OpenAiAgent openAiAgent;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private MongoTemplate mongoTemplate;

    // ==================== 聊天接口 ====================

    /**
     * 带记忆的聊天接口
     */
    @PostMapping("/nonStream")
    public Result<ChatStringRes> chatWithMemory(@RequestBody ChatReq req, HttpServletRequest request) {
        try {
            String userIp = IpUtils.getClientIp();
            UserContext.setCurrentUserIp(userIp);

            // 处理和验证 memoryId
            String memoryId = conversationService.processMemoryId(req.getMemoryId(), userIp);
            UserContext.setCurrentMemoryId(memoryId);

            log.info("开始带记忆对话 - IP: {}, memoryId: {}, message: {}", userIp, memoryId, req.getMessage());

            // 调用 AI
            String response = openAiAgent.chat(memoryId, req.getMessage());

            ChatStringRes res = ChatStringRes.builder()
                    .memoryId(memoryId)
                    .message(response)
                    .build();
            return Result.success(res);
        } catch (Exception e) {
            log.error("带记忆对话失败", e);
            throw new RuntimeException("客户端对话失败");
        } finally {
//            UserContext.clear();
        }
    }

    /**
     * 带记忆的流式聊天接口
     * Flux<String>
     * Result<ChatStreamRes> 此类型暂时不能保留 UserContext
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_PLAIN_VALUE) // , produces = MediaType.TEXT_PLAIN_VALUE
    public Flux<String> chatStreamWithMemory(@RequestBody ChatReq req, HttpServletRequest request) {
        try {
            String userIp = IpUtils.getClientIp();
            UserContext.setCurrentUserIp(userIp);

            // 处理和验证 memoryId
            String memoryId = conversationService.processMemoryId(req.getMemoryId(), userIp);
            UserContext.setCurrentMemoryId(memoryId);

            log.info("开始带记忆流式对话 - IP: {}, memoryId: {}", userIp, memoryId);

            Flux<String> stringFlux = openAiAgent.chatStream(memoryId, req.getMessage())
                    .doOnNext(chunk -> {
                        log.debug("发送片段: {}",
                                chunk.length() > 50 ? chunk.substring(0, 50) + "..." : chunk);
                    })
                    .doOnComplete(() -> {
                        log.info("带记忆流式对话完成");
                    })
                    .doOnError(error -> log.error("带记忆流式对话出错", error))
                    .doFinally(signalType -> {
                        log.debug("清理用户上下文，信号类型: {}", signalType);
                    });
//            ChatStreamRes res = ChatStreamRes.builder()
//                    .memoryId(memoryId)
//                    .contentStream(stringFlux)
//                    .build();
//            return Result.success(res);
            return stringFlux;
//            return ResponseEntity.ok()
//                    .contentType(MediaType.TEXT_PLAIN)
//                    .header("X-Memory-Id", memoryId)  // 通过header传递memoryId
//                    .body(stringFlux);

        } catch (Exception e) {
            log.error("带记忆对话失败", e);
            throw new RuntimeException("客户端对话失败");
        } finally {
            UserContext.clear();
        }
    }
}
