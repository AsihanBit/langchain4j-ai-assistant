package com.aiassist.ai.core.memory;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;

//@Configuration
public class ChatMemoryConfiguration {

    @Bean("chatMemoryProviderInMemory")
    public ChatMemoryProvider chatMemoryProviderInMemory() {
        return memoryId -> MessageWindowChatMemory.withMaxMessages(20);
    }
}
