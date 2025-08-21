package com.aiassist.chat.server.controller;

import com.aiassist.chat.server.dto.res.MessagesRes;
import com.aiassist.chat.server.result.Result;
import com.aiassist.ai.core.entity.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private MongoTemplate mongoTemplate;

    // ==================== 按对话ID查询详细信息 ====================

    /**
     * 根据对话ID查询消息列表（仅消息，不包含对话元信息）
     */
    @GetMapping("/getMessagesById")
    public Result<MessagesRes> getMessagesById(@RequestParam String memoryId) {
        try {
            log.info("查询对话消息列表 - memoryId: {}", memoryId);

            // 构建查询条件
//            Query query = Query.query(Criteria.where("memory_id").is(memoryId))
//                    .with(Sort.by(Sort.Direction.ASC, "turn_index"))
//                    .skip((long) page * size)
//                    .limit(size)
            Query query = Query.query(Criteria.where("memory_id").is(memoryId))
                    .with(Sort.by(Sort.Direction.DESC, "turn_index"));

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
            return Result.success(res);

// .totalPages((int) Math.ceil((double) totalCount / size))

        } catch (Exception e) {
            log.error("查询对话消息列表失败 - memoryId: {}", memoryId, e);
            throw new RuntimeException("查询失败");
        }
    }

}
