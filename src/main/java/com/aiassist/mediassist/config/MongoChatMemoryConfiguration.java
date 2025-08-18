package com.aiassist.mediassist.config;

import com.aiassist.mediassist.store.MongoChatMemoryStore;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 聊天记忆配置
 * 
 * 集成 MongoDB 持久化存储与 LangChain4j 的 ChatMemoryProvider
 */
@Slf4j
@Configuration
public class MongoChatMemoryConfiguration {

    @Autowired
    private MongoChatMemoryStore mongoChatMemoryStore;

    /**
     * LangChain框架控制的窗口大小，设置为较大值使其失效
     */
    @Value("${chat.langchain.max-size:10}")
    private int langchainMaxSize;

    /**
     * 是否启用MongoDB持久化，可通过配置文件控制
     */
    @Value("${chat.mongodb.enable:true}")
    private boolean enableMongoDbStorage;

    /**
     * ChatMemory 提供器：基于MongoDB的持久化消息记忆 + 窗口限制
     *
     * 功能特性：
     * - 每个 memoryId 维护一个独立的对话上下文
     * - 持久化存储到MongoDB，重启后可恢复
     * - 支持消息窗口限制，避免上下文过长
     * - 支持TTL自动过期清理
     * - 支持用户隔离
     * 
     * 配置说明：
     * - chat.memory.max-messages: 最大消息数量，默认10
     * - chat.memory.enable-mongodb: 是否启用MongoDB存储，默认true
     * 
     * 设计说明：
     * - 所有聊天接口（包括ChatMemoryController）都通过此Provider统一管理记忆
     * - 这确保了记忆机制的一致性和LangChain4j的完整兼容性
     * - MongoDB存储透明地集成在MessageWindowChatMemory中
     */
    @Bean("chatMemoryProviderOpenAi")
    public ChatMemoryProvider chatMemoryProviderOpenAi() {
        log.info("初始化统一ChatMemory配置 - MongoDB存储: {}, LangChain框架窗口: {}", enableMongoDbStorage, langchainMaxSize);
        
        if (enableMongoDbStorage) {
            // 使用MongoDB持久化存储 + 消息窗口限制
            return memoryId -> {
                log.debug("创建MongoDB持久化ChatMemory: memoryId={}", memoryId);
                return MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(langchainMaxSize)
                        .chatMemoryStore(mongoChatMemoryStore)
                        .build();
            };
        } else {
            // 仅使用内存存储（适用于开发和测试）
            log.warn("使用内存存储ChatMemory（非持久化）");
            return memoryId -> MessageWindowChatMemory.withMaxMessages(langchainMaxSize);
        }
    }
}
