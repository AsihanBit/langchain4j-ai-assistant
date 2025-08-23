package com.aiassist.ai.core.ai;

import com.aiassist.ai.core.properties.PromptProperties;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
public class AgentConfig {

    @Bean
    public OpenAiAgent openAiAgent(ChatModel model,
                                   StreamingChatModel streamingModel,     // 接口有 Flux 方式 -> 必须提供
                                   @Qualifier("chatMemoryProviderOpenAi") ChatMemoryProvider memoryProvider,
                                   @Qualifier("textTools") Object textTools,
                                   @Qualifier("grpcTools") Object grpcTools,
                                   PromptProperties props) throws Exception {
        String systemPrompt = props.getPath().getContentAsString(StandardCharsets.UTF_8);
        return AiServices.builder(OpenAiAgent.class)
                .chatModel(model)                       // 支持 chat(...)
                .streamingChatModel(streamingModel)     // 支持 chatStream(...)
                .chatMemoryProvider(memoryProvider)
                .tools(textTools, grpcTools)
                .systemMessageProvider(ignored -> systemPrompt) // 外部化系统提示词
                .build();
    }

}
