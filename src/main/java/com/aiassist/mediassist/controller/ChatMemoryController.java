package com.aiassist.mediassist.controller;

import com.aiassist.mediassist.ai.OpenAiAgent;
import com.aiassist.mediassist.context.UserContext;
import com.aiassist.mediassist.dto.entity.Conversation;
import com.aiassist.mediassist.dto.entity.Message;
import com.aiassist.mediassist.dto.req.AgentReq;
import com.aiassist.mediassist.service.ConversationService;
import com.aiassist.mediassist.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 聊天记忆控制器
 * 
 * 提供基于MongoDB持久化的聊天记忆功能
 * 独立于原有的ChatController，不影响原有接口
 * 
 * 设计架构：
 * - 完全依赖OpenAiAgent的chatMemoryProvider配置
 * - 所有记忆逻辑由LangChain4j统一管理
 * - 提供额外的会话管理和查询功能
 */
@Slf4j
@RestController
@RequestMapping("/api/chat-memory")
public class ChatMemoryController {

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
    @PostMapping("/chat")
    public ChatResponse chatWithMemory(@RequestBody ChatMemoryRequest req, HttpServletRequest request) {
        try {
            String userIp = IpUtils.getClientIp();
            UserContext.setCurrentUserIp (userIp);
            
            // 处理和验证 memoryId
            String memoryId = processMemoryId(req.getMemoryId(), userIp);
            UserContext.setCurrentMemoryId(memoryId);
            
            log.info("开始带记忆对话 - IP: {}, memoryId: {}, message: {}", userIp, memoryId, req.getMessage());
            
            // 调用 AI
            String response = openAiAgent.chat(memoryId, req.getMessage());
            
            return ChatResponse.builder()
                    .memoryId(memoryId)
                    .message(response)
                    .timestamp(LocalDateTime.now())
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("带记忆对话失败", e);
            return ChatResponse.builder()
                    .success(false)
                    .error(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 带记忆的流式聊天接口
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> chatStreamWithMemory(@RequestBody ChatMemoryRequest req, HttpServletRequest request) {
        String userIp = IpUtils.getClientIp();
        UserContext.setCurrentUserIp(userIp);
        
        // 处理和验证 memoryId
        String memoryId = processMemoryId(req.getMemoryId(), userIp);
        UserContext.setCurrentMemoryId(memoryId);
        
        log.info("开始带记忆流式对话 - IP: {}, memoryId: {}", userIp, memoryId);

        return openAiAgent.chatStream(memoryId, req.getMessage())
                .doOnNext(chunk -> log.debug("发送片段: {}", 
                    chunk.length() > 50 ? chunk.substring(0, 50) + "..." : chunk))
                .doOnComplete(() -> log.info("带记忆流式对话完成"))
                .doOnError(error -> log.error("带记忆流式对话出错", error))
                .doFinally(signalType -> {
                    UserContext.clear();
                    log.debug("清理用户上下文，信号类型: {}", signalType);
                });
    }

    // ==================== 会话管理接口 ====================

    /**
     * 创建新会话
     */
    @PostMapping("/conversations")
    public ConversationResponse createNewConversation(HttpServletRequest request) {
        try {
            String userIp = IpUtils.getClientIp();
            String memoryId = conversationService.createNewConversation(userIp);
            
            log.info("创建新会话 - IP: {}, memoryId: {}", userIp, memoryId);
            return ConversationResponse.builder()
                    .memoryId(memoryId)
                    .success(true)
                    .message("会话创建成功")
                    .build();
                    
        } catch (Exception e) {
            log.error("创建新会话失败", e);
            return ConversationResponse.builder()
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * 获取用户的会话列表
     */
    @GetMapping("/conversations")
    public List<Conversation> getUserConversations(HttpServletRequest request) {
        try {
            String userIp = IpUtils.getClientIp();
            return conversationService.getUserConversations(userIp);
        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return List.of();
        }
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/conversations/{memoryId}")
    public ConversationDetailResponse getConversationDetail(@PathVariable String memoryId) {
        try {
            // 获取会话信息
            Conversation conversation = conversationService.getConversation(memoryId);
            if (conversation == null) {
                return ConversationDetailResponse.builder()
                        .success(false)
                        .error("会话不存在")
                        .build();
            }
            
            // 获取消息列表
            Query query = Query.query(Criteria.where("memory_id").is(memoryId))
                    .with(Sort.by(Sort.Direction.ASC, "turn_index"));
            List<Message> messages = mongoTemplate.find(query, Message.class);
            
            return ConversationDetailResponse.builder()
                    .conversation(conversation)
                    .messages(messages)
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("获取会话详情失败: memoryId={}", memoryId, e);
            return ConversationDetailResponse.builder()
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/conversations/{memoryId}")
    public ConversationResponse deleteConversation(@PathVariable String memoryId) {
        try {
            conversationService.deleteConversation(memoryId);
            log.info("删除会话: memoryId={}", memoryId);
            return ConversationResponse.builder()
                    .memoryId(memoryId)
                    .success(true)
                    .message("会话删除成功")
                    .build();
                    
        } catch (Exception e) {
            log.error("删除会话失败: memoryId={}", memoryId, e);
            return ConversationResponse.builder()
                    .memoryId(memoryId)
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * 获取会话统计信息
     */
    @GetMapping("/conversations/stats")
    public ConversationService.ConversationStats getConversationStats() {
        return conversationService.getStats();
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

    // ==================== 请求/响应对象 ====================

    @Data
    public static class ChatMemoryRequest {
        private String memoryId;
        private String message;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ChatResponse {
        private String memoryId;
        private String message;
        private LocalDateTime timestamp;
        private boolean success;
        private String error;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ConversationResponse {
        private String memoryId;
        private String message;
        private boolean success;
        private String error;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ConversationDetailResponse {
        private Conversation conversation;
        private List<Message> messages;
        private boolean success;
        private String error;
    }
}
