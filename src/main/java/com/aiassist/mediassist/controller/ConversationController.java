package com.aiassist.mediassist.controller;

import com.aiassist.mediassist.context.UserContext;
import com.aiassist.mediassist.dto.entity.Conversation;
import com.aiassist.mediassist.dto.res.CreateNewConversationRes;
import com.aiassist.mediassist.dto.res.ConversationsRes;
import com.aiassist.mediassist.result.Result;
import com.aiassist.mediassist.service.ConversationService;
import com.aiassist.mediassist.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 对话查询控制器
 * <p>
 * 提供根据IP和对话ID查询MongoDB中对话记录的功能
 * 独立的查询接口，专注于数据检索
 */
@Slf4j
@RestController
@RequestMapping("/conversation")
public class ConversationController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ConversationService conversationService;

    // ==================== 按IP查询对话 ====================

    /**
     * 根据用户IP查询所有对话
     */
    @GetMapping("/getConversationsByIp")
    public Result<ConversationsRes> getConversationsByIp() {
        try {
            String userIp = IpUtils.getClientIp();
            // todo page, size
            // 构建查询条件
            List<Conversation> conversations = conversationService.getUserConversations(userIp);
            int totalCount = conversations.size();

//            log.info("查询完成 - 找到 {} 个对话，总共 {} 个", conversations.size(), totalCount);
            log.info("查询用户对话列表 - IP: {} 找到 {} 个对话", userIp, totalCount);

            ConversationsRes res = ConversationsRes.builder()
                    .conversations(conversations)
                    .totalCount(totalCount)
                    .build();
            return Result.success(res);

        } catch (Exception e) {
            log.error("查询用户对话列表失败 - IP: {}", UserContext.getCurrentUserIp(), e);
            throw new RuntimeException("查询失败");
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 创建新会话
     */
    @PostMapping("/createNew")
    public Result<CreateNewConversationRes> createNew(HttpServletRequest request) {
        try {
            String userIp = IpUtils.getClientIp();
            String memoryId = conversationService.createNewConversation(userIp);

            log.info("创建新会话 - IP: {}, memoryId: {}", userIp, memoryId);
            CreateNewConversationRes res = CreateNewConversationRes.builder()
                    .memoryId(memoryId)
                    .message("会话创建成功")
                    .build();
            return Result.success(res);

        } catch (Exception e) {
            log.error("创建新会话失败", e);
            throw new RuntimeException("创建新会话失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/getConversationInfo/{memoryId}")
    public Conversation getConversationInfo(@PathVariable String memoryId) {
        try {
            return conversationService.getConversation(memoryId);
        } catch (Exception e) {
            log.error("获取会话详情失败: memoryId={}", memoryId, e);
            return null;
        }
    }

    // 统计总数
//    .skip((long) page * size)
//    .limit(size);
//    long totalCount = mongoTemplate.count(
//    Query.query(Criteria.where("user_ip").is(userIp)),
//    Conversation.class
//    .totalPages((int) Math.ceil((double) totalCount / size))


    /**
     * 删除会话
     */
    @DeleteMapping("/delById/{memoryId}")
    public Result<String> deleteConversation(@PathVariable String memoryId) {
        try {
            conversationService.deleteConversation(memoryId);
            log.info("删除会话: memoryId={}", memoryId);
            return Result.success("删除成功");

        } catch (Exception e) {
            log.error("删除会话失败: memoryId={}", memoryId, e);
            throw new RuntimeException("删除会话失败: " + e.getMessage());
        }
    }

    // ==================== 高级查询接口 ====================

    /*
      根据时间范围查询对话
      GET /api/conversation-query/conversations/by-time-range
      ISO格式: 2024-01-01T00:00:00
      page size
     */
}
