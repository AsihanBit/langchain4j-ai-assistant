package com.aiassist.mediassist.config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
public class ChatMemoryConfiguration {

    @Bean("chatMemoryProviderInMemory")
    public ChatMemoryProvider chatMemoryProviderInMemory() {
        return memoryId -> MessageWindowChatMemory.withMaxMessages(20);
    }
}
