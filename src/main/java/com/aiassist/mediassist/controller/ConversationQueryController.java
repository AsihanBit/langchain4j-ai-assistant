package com.aiassist.mediassist.controller;

import com.aiassist.mediassist.context.UserContext;
import com.aiassist.mediassist.dto.entity.Conversation;
import com.aiassist.mediassist.dto.entity.Message;
import com.aiassist.mediassist.dto.res.ConversationsRes;
import com.aiassist.mediassist.dto.res.MessagesRes;
import com.aiassist.mediassist.result.Result;
import com.aiassist.mediassist.util.IpUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话查询控制器
 * 
 * 提供根据IP和对话ID查询MongoDB中对话记录的功能
 * 独立的查询接口，专注于数据检索
 */
@Slf4j
@RestController
@RequestMapping("/conversations")
public class ConversationQueryController {

    @Autowired
    private MongoTemplate mongoTemplate;

    // ==================== 按IP查询对话 ====================

    /**
     * 根据用户IP查询所有对话
     * GET /api/conversation-query/conversations/by-ip/{userIp}
     */
    @GetMapping("/getConversation")
    public Result<ConversationsRes> getConversationsByIp() {
        try {
            String userIp = IpUtils.getClientIp();
            UserContext.setCurrentUserIp (userIp);

            // todo page, size
            log.info("查询用户对话列表 - IP: {}", userIp);

            // 构建查询条件
            Query query = Query.query(Criteria.where("user_ip").is(userIp))
                    .with(Sort.by(Sort.Direction.DESC, "created_time"));  // 按创建时间倒序
//                    .skip((long) page * size)
//                    .limit(size);
            
            // 执行查询
            List<Conversation> conversations = mongoTemplate.find(query, Conversation.class);
            
            // 统计总数
//            long totalCount = mongoTemplate.count(
//                Query.query(Criteria.where("user_ip").is(userIp)),
//                Conversation.class
//            );
            int totalCount = conversations.size();
            
//            log.info("查询完成 - 找到 {} 个对话，总共 {} 个", conversations.size(), totalCount);
            log.info("查询完成 - 找到 {} 个对话", totalCount);

            ConversationsRes res = ConversationsRes.builder()
                    .conversations(conversations)
                    .totalCount(totalCount)
                    .build();
            return Result.success(res);

//                    .totalPages((int) Math.ceil((double) totalCount / size))

        } catch (Exception e) {
            log.error("查询用户对话列表失败 - IP: {}", UserContext.getCurrentUserIp(), e);
            throw new RuntimeException("查询失败");
        } finally {
            UserContext.clear();
        }
    }

    // ==================== 按对话ID查询详细信息 ====================

    /**
     * 根据对话ID查询消息列表（仅消息，不包含对话元信息）
     * GET /api/conversation-query/conversations/{memoryId}/messages
     */
    @GetMapping("/messages")
    public Result<MessagesRes> getConversationMessages(@RequestParam  String memoryId) {
        try {
            log.info("查询对话消息列表 - memoryId: {}", memoryId);
            
            // 构建查询条件
//            Query query = Query.query(Criteria.where("memory_id").is(memoryId))
//                    .with(Sort.by(Sort.Direction.ASC, "turn_index"))
//                    .skip((long) page * size)
//                    .limit(size)
            Query query = Query.query(Criteria.where("memory_id").is(memoryId))
                    .with(Sort.by(Sort.Direction.ASC, "turn_index"));

            // 执行查询
            List<Message> messages = mongoTemplate.find(query, Message.class);
            
            // 统计总数
//            long totalCount = mongoTemplate.count(
//                Query.query(Criteria.where("memory_id").is(memoryId)),
//                Message.class
//            );
            int totalCount = messages.size();

            MessagesRes res = MessagesRes.builder()
                    .messages(messages)
                    .totalCount(totalCount)
                    .build();
            return  Result.success(res);

// .totalPages((int) Math.ceil((double) totalCount / size))

        } catch (Exception e) {
            log.error("查询对话消息列表失败 - memoryId: {}", memoryId, e);
            throw new RuntimeException("查询失败");
        }
    }

    // ==================== 高级查询接口 ====================

    /**
     * 根据时间范围查询对话
     * GET /api/conversation-query/conversations/by-time-range
     */
//    @GetMapping("/by-time-range")
    public Result getConversationsByTimeRange(
            @RequestParam(required = false) String userIp,
            @RequestParam String startTime,  // ISO格式: 2024-01-01T00:00:00
            @RequestParam String endTime,    // ISO格式: 2024-01-31T23:59:59
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            LocalDateTime start = LocalDateTime.parse(startTime);
            LocalDateTime end = LocalDateTime.parse(endTime);
            
            log.info("按时间范围查询对话 - IP: {}, 时间范围: {} ~ {}", userIp, start, end);
            
            // 构建查询条件
            Criteria criteria = Criteria.where("created_time").gte(start).lte(end);
            if (userIp != null && !userIp.trim().isEmpty()) {
                criteria = criteria.and("user_ip").is(userIp);
            }
            
            Query query = Query.query(criteria)
                    .with(Sort.by(Sort.Direction.DESC, "created_time"))
                    .skip((long) page * size)
                    .limit(size);
            
            List<Conversation> conversations = mongoTemplate.find(query, Conversation.class);
            long totalCount = mongoTemplate.count(Query.query(criteria), Conversation.class);
            
            return Result.success();

        } catch (Exception e) {
            log.error("按时间范围查询对话失败", e);
            return Result.success();
        }
    }

//    public static class ConversationStats {
//        private int messageCount;
//        private int totalTokensInput;
//        private int totalTokensOutput;
//        private int totalTokens;
//    }
}
